#!/usr/bin/env python3
"""Build a small, provenance preserving AC catalog from pinned local snapshots."""
from __future__ import annotations

import json
import math
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "data/generated"
MOBILE_INDEX = ROOT / "app/src/main/assets/catalog-index.json"
REGISTRY = ROOT / "app/src/main/java/com/thanhnha/universalacremote/ir/ProtocolRegistry.kt"
STATE = ROOT / "app/src/main/java/com/thanhnha/universalacremote/ir/AcState.kt"
PINNED_IRREMOTE_SHA = "1e2f0f3ef0a93cbf2a8ddb2e95130f8f4c584b3f"
SHA_RE = re.compile(r"^[0-9a-f]{40}$")
FLIPPER_LICENSE_CUTOFF = "2319685"


def selected_input_paths(source: str, folder: Path) -> list[Path]:
    if source == "smartir": return sorted((folder / "codes/climate").glob("*.json"))
    if source == "flipper-irdb": return sorted((folder / "ACs").rglob("*.ir"))
    return []


def flipper_path_is_ac(path: str) -> bool:
    return bool(re.search(r"(?:^|[/\\])ACs(?:[/\\])", path, re.I))


def license_is_clear(source: str, lock: dict) -> bool:
    return lock.get(source, {}).get("status") in ("ACTIVE", "PARTIAL_LICENSE_SAFE") and bool(lock.get(source, {}).get("licenseId"))


def normalize_brand(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", "", value.casefold())


def base_profile(*, source: str, sha: str, path: str, source_id: str,
                 brand: str, ac_model: str | None, remote_model: str | None,
                 protocol_id: str | None, variant: str | None,
                 encoding: str, capabilities: list[str], temp: dict | None,
                 fans: list[str], modes: list[str], v_swing: dict,
                 h_swing: dict, special: list[str], verification: str) -> dict:
    if not SHA_RE.fullmatch(sha):
        raise ValueError(f"{source}: sourceCommitSha must be a full 40-character SHA")
    return {
        "id": f"{source}:{source_id}", "brand": brand,
        "normalizedBrand": normalize_brand(brand), "aliases": [],
        "acModel": ac_model, "remoteModel": remote_model,
        "protocolId": protocol_id, "protocolModel": variant,
        "encodingType": encoding, "capabilities": sorted(set(capabilities)),
        "temperatureRange": temp, "fanModes": fans,
        "operationModes": modes,
        "verticalSwingCapabilities": v_swing,
        "horizontalSwingCapabilities": h_swing,
        "specialCapabilities": special,
        "source": source, "sourceCommitSha": sha, "sourcePath": path,
        "verificationStatus": verification,
        "rawCommands": {}, "sourceMetadata": {},
    }


def parse_irremote_registry(text: str, sha: str = PINNED_IRREMOTE_SHA, state_text: str | None = None) -> list[dict]:
    # Read only reviewed entries in the app registry; do not infer from protocol names.
    state_text = state_text if state_text is not None else STATE.read_text(encoding="utf-8")
    mode_match = re.search(r"enum class AcMode[^\{]*\{([^}]+)\}", state_text)
    fan_match = re.search(r"enum class AcFan[^\{]*\{([^}]+)\}", state_text)
    if not mode_match or not fan_match: raise ValueError("Could not read AcMode/AcFan registry enums")
    modes = [v.lower() for v in re.findall(r"\b([A-Z][A-Z0-9_]*)\s*\(", mode_match.group(1))]
    fans = ["low" if v == "min" else v.lower() for v in re.findall(r"\b([A-Z][A-Z0-9_]*)\s*\(", fan_match.group(1))]
    result = []
    pattern = re.compile(r'ProtocolDefinition\("([^"]+)",\s*"([^"]+)",\s*"[^"]+",\s*"([^"]+)",\s*(null|"[^"]+")?,\s*(setOf\([^)]*\)|emptySet\(\)),\s*(\d+),\s*(\d+),\s*commonModes,\s*commonFans,\s*(true|false),\s*(true|false)\)')
    for match in pattern.finditer(text):
        pid, upstream, brand, remote, modelset, lo, hi, vs, hs = match.groups()
        models = re.findall(r'"([^"]+)"', modelset)
        if remote and remote != "null":
            models = [remote[1:-1]]
        result.append(base_profile(
            source="irremoteesp8266", sha=sha,
            path="app/src/main/java/com/thanhnha/universalacremote/ir/ProtocolRegistry.kt",
            source_id=pid, brand=brand, ac_model=None,
            remote_model=remote.strip('"') if remote and remote != "null" else None,
            protocol_id=upstream, variant=",".join(models) or None,
            encoding="PROTOCOL", capabilities=["power", *[f"mode:{x}" for x in modes], *[f"fan:{x}" for x in fans]],
            temp={"minC": int(lo), "maxC": int(hi)}, fans=fans,
            modes=modes,
            v_swing={"type": "ON_OFF" if vs == "true" else "NONE", "positions": []},
            h_swing={"type": "ON_OFF" if hs == "true" else "NONE", "positions": []},
            special=[], verification="candidate"))
    return result


def parse_smartir(path: Path, sha: str) -> list[dict]:
    obj = json.loads(path.read_text(encoding="utf-8"))
    brand = obj.get("manufacturer")
    if not isinstance(brand, str) or not brand.strip(): raise ValueError("missing manufacturer")
    modes = obj.get("operationModes", [])
    fans = obj.get("fanModes", [])
    encoding = obj.get("commandsEncoding")
    if encoding not in ("Base64", "Raw"): raise ValueError(f"unsupported commandsEncoding: {encoding!r}")
    commands = obj.get("commands")
    if not isinstance(commands, dict) or not commands: raise ValueError("missing or invalid commands object")
    low, high = obj["minTemperature"], obj["maxTemperature"]
    if any(isinstance(v, bool) or not isinstance(v, (int, float)) or not math.isfinite(v) for v in (low, high)):
        raise ValueError("temperature range must contain finite numeric limits")
    if not (0 <= low <= high <= 50): raise ValueError("temperature range outside validated Celsius bounds")
    if not isinstance(modes, list) or not all(isinstance(v, str) for v in modes): raise ValueError("operationModes must be a string list")
    if not isinstance(fans, list) or not all(isinstance(v, str) for v in fans): raise ValueError("fanModes must be a string list")
    swing_modes = obj.get("swingModes", [])
    if not isinstance(swing_modes, list) or not all(isinstance(v, str) for v in swing_modes): raise ValueError("swingModes must be a string list")
    # SmartIR's swingModes is preserved verbatim; it does not identify an axis,
    # so do not guess vertical versus horizontal in the canonical capability fields.
    vertical = {"type":"NONE", "positions":[]}
    standard_commands = {"off", "on", *modes, *fans}
    special = sorted(k for k in commands if isinstance(k, str) and k.casefold() not in standard_commands)
    try: source_path = path.relative_to(ROOT / "data/upstreams/snapshots/smartir").as_posix()
    except ValueError: source_path = path.name
    record = base_profile(source="smartir", sha=sha, path=source_path, source_id=path.stem,
        brand=brand, ac_model=", ".join(obj.get("supportedModels", [])) or None,
        remote_model=None, protocol_id=None, variant=None, encoding="IMPORTED_RAW",
        capabilities=[*[f"mode:{x}" for x in modes], *[f"fan:{x}" for x in fans], *(["power"] if "off" in commands or "on" in commands else []), *[f"special:{x}" for x in special]],
        temp={"minC": obj["minTemperature"], "maxC": obj["maxTemperature"]}, fans=fans, modes=modes,
        v_swing=vertical, h_swing={"type":"NONE","positions":[]},
        special=special, verification="candidate")
    record["rawCommands"] = commands
    record["sourceMetadata"] = {"supportedController": obj.get("supportedController"),
                                 "commandsEncoding": obj.get("commandsEncoding"),
                                 "precision": obj.get("precision"), "swingModes": swing_modes}
    return [record]


def parse_flipper_ir(path: Path, sha: str) -> list[dict]:
    try: source_path = path.relative_to(ROOT / "data/upstreams/snapshots/flipper-irdb").as_posix()
    except ValueError: source_path = path.as_posix()
    return parse_flipper_text(path.read_text(encoding="utf-8"), sha, source_path)


def parse_irplus(path: Path, sha: str) -> list[dict]:
    root = ET.parse(path).getroot()
    brand = root.attrib.get("brand") or root.attrib.get("manufacturer")
    if not brand: raise ValueError("missing brand/manufacturer")
    # Keep each remote as a separate candidate. XML code blobs are not guessed into AC states.
    return [base_profile(source="irplus", sha=sha, path=path.as_posix(), source_id=path.stem,
        brand=brand, ac_model=root.attrib.get("model"), remote_model=root.attrib.get("device"),
        protocol_id=None, variant=None, encoding="IMPORTED_RAW", capabilities=[], temp=None,
        fans=[], modes=[], v_swing={"type":"NONE","positions":[]},
        h_swing={"type":"NONE","positions":[]}, special=[], verification="candidate")]


def parse_flipper_text(text: str, sha: str, path: str) -> list[dict]:
    if len(text) > 1_000_000: raise ValueError("file exceeds 1 MB limit")
    # Match the existing app importer exactly; broader Flipper header variants stay unsupported.
    if not re.search(r"(?im)^Filetype: IR signals\s*$", text): raise ValueError("unsupported Flipper header: expected 'Filetype: IR signals'")
    # AC directory gate prevents TV/DVD/audio ingestion; button names are only a conservative hint.
    if not flipper_path_is_ac(path): return []
    chunks = re.split(r"(?m)^#\s*$", text)
    records = []
    for i, chunk in enumerate(chunks):
        fields = dict(re.findall(r"(?m)^([\w_-]+):\s*(.*?)\s*$", chunk))
        if not fields.get("type"): continue
        if fields.get("type") == "parsed":
            record = base_profile(source="flipper-irdb", sha=sha, path=path,
                source_id=f"{Path(path).stem}:{i}:{fields.get('name','parsed')}",
                brand=Path(path).parent.name, ac_model=Path(path).stem, remote_model=None,
                protocol_id=None, variant=None, encoding="FLIPPER_PARSED_UNSUPPORTED", capabilities=[], temp=None,
                fans=[], modes=[], v_swing={"type":"NONE","positions":[]},
                h_swing={"type":"NONE","positions":[]}, special=[], verification="unsupportedEncoding")
            record["sourceMetadata"] = {"flipperType":"parsed", "protocol":fields.get("protocol"),
                "address":fields.get("address"), "command":fields.get("command"),
                "unsupportedReason":"No verified decoder/protocol mapping is available."}
            records.append(record)
            continue
        if fields.get("type") != "raw": raise ValueError(f"unsupported Flipper encoding '{fields.get('type')}'")
        raw = fields.get("data", "").split()
        if not raw or len(raw) > 4096 or len(raw) % 2: raise ValueError("invalid RAW timing count; expected complete mark/space pairs")
        try: vals = [int(x) for x in raw]
        except ValueError as exc: raise ValueError("RAW timing contains a non-integer token") from exc
        if any(x <= 0 or x > 1_000_000 for x in vals) or sum(vals) > 120_000_000: raise ValueError("RAW timing outside safety bounds")
        try: freq = int(fields.get("frequency", "0"))
        except ValueError as exc: raise ValueError("invalid carrier frequency") from exc
        if not 1 <= freq <= 500_000: raise ValueError("invalid carrier frequency")
        record = base_profile(source="flipper-irdb", sha=sha, path=path,
            source_id=f"{Path(path).stem}:{i}:{fields.get('name','raw')}",
            brand=Path(path).parent.name, ac_model=Path(path).stem, remote_model=None,
            protocol_id=None, variant=None, encoding="IMPORTED_RAW", capabilities=[], temp=None,
            fans=[], modes=[], v_swing={"type":"NONE","positions":[]},
            h_swing={"type":"NONE","positions":[]}, special=[], verification="candidate")
        record["rawCommands"] = {fields.get("name", "raw"): {"carrierFrequencyHz":freq, "durationsMicros":vals}}
        record["sourceMetadata"] = {"duty_cycle": fields.get("duty_cycle")}
        records.append(record)
    return records


def find_duplicates(profiles: list[dict]) -> list[dict]:
    # Names are clues only; never auto-merge. Exact same source path and model is a review candidate.
    groups: dict[tuple, list[str]] = {}
    for p in profiles:
        key = (p["normalizedBrand"], p["sourcePath"], p["acModel"], p["remoteModel"], p["protocolId"], p["protocolModel"])
        groups.setdefault(key, []).append(p["id"])
    return [{"identity": list(k), "profileIds": ids} for k, ids in groups.items() if len(ids) > 1]


def build() -> tuple[list[dict], dict]:
    lock = json.loads((ROOT / "upstream-lock.json").read_text(encoding="utf-8"))
    locked = {s["name"]: s for s in lock["sources"]}
    sha_irremote = locked["irremoteesp8266"]["commitSha"]
    profiles = parse_irremote_registry(REGISTRY.read_text(encoding="utf-8"), sha_irremote)
    malformed: list[dict] = []
    exclusion_doc = json.loads((ROOT / "data/upstreams/flipper-exclusions.json").read_text(encoding="utf-8"))
    excluded = exclusion_doc["excludedFiles"]
    excluded_flipper_paths = {item.get("path") for item in excluded if item.get("source") == "flipper-irdb"}
    for source in ("smartir", "flipper-irdb"):
        config = locked[source]
        if config["status"] not in ("ACTIVE", "PARTIAL_LICENSE_SAFE"): continue
        folder = ROOT / "data/upstreams/snapshots" / source
        meta_path = folder / "source.json"
        if not meta_path.exists():
            malformed.append({"source":source,"path":str(folder),"reason":"missing pinned snapshot provenance"}); continue
        meta = json.loads(meta_path.read_text(encoding="utf-8")); sha = meta.get("commitSha", "")
        if sha != config["commitSha"]:
            malformed.append({"source":source,"path":str(meta_path),"reason":"snapshot SHA does not match upstream lock"}); continue
        paths = selected_input_paths(source, folder)
        for path in paths:
            try:
                if source == "smartir": profiles.extend(parse_smartir(path, sha))
                else:
                    if not flipper_path_is_ac(path.as_posix()): continue
                    if path.relative_to(folder).as_posix() in excluded_flipper_paths: continue
                    profiles.extend(parse_flipper_ir(path, sha))
            except Exception as exc: malformed.append({"source":source,"path":path.relative_to(ROOT).as_posix(),"reason":str(exc)})
    profiles.sort(key=lambda p: p["id"])
    counts = {s: sum(p["source"] == s for p in profiles) for s in ("irremoteesp8266", "smartir", "flipper-irdb", "irplus")}
    flipper_excluded = [x for x in excluded if x.get("source") == "flipper-irdb"]
    flipper_cutoff = [x for x in flipper_excluded if "predates CC0 cutoff" in x.get("reason", "")]
    flipper_unproven = [x for x in flipper_excluded if "provenance not proven" in x.get("reason", "")]
    report = {"totalBrands": len({p["normalizedBrand"] for p in profiles}), "totalProfiles": len(profiles),
        "protocolProfiles": sum(p["encodingType"] == "PROTOCOL" for p in profiles),
        "rawProfiles": sum(p["encodingType"] != "PROTOCOL" for p in profiles),
        "profilesBySource": counts, "malformedEntries": malformed, "duplicateCandidates": find_duplicates(profiles),
        "excluded": {"licenseUnclear": [{"source":"irplus","reason":"EXCLUDED_LICENSE_UNCLEAR"}],
            "flipperLicenseCutoff": flipper_cutoff, "malformed": malformed,
            "unsupportedEncoding": [x for x in malformed if "unsupported Flipper encoding" in x.get("reason", "") or "unsupported Flipper header" in x.get("reason", "")],
            "missingProvenance": [*flipper_unproven, *[x for x in malformed if "provenance" in x.get("reason", "")]]},
        "flipperFilesScanned": len(selected_input_paths("flipper-irdb", ROOT / "data/upstreams/snapshots/flipper-irdb")),
        "flipperIngestedProfiles": counts["flipper-irdb"],
        "flipperProfilesImported": counts["flipper-irdb"],
        "flipperRawCommandsImported": sum(len(p["rawCommands"]) for p in profiles if p["source"] == "flipper-irdb" and p["encodingType"] == "IMPORTED_RAW"),
        "flipperParsedUnsupported": sum(p["encodingType"] == "FLIPPER_PARSED_UNSUPPORTED" for p in profiles if p["source"] == "flipper-irdb"),
        "flipperMalformed": sum(item.get("source") == "flipper-irdb" for item in malformed),
        "flipperExcludedByLicense": len(flipper_excluded),
        "flipperTransmittableProfiles": sum(p["source"] == "flipper-irdb" and is_transmittable(p) for p in profiles),
        "flipperCutoffExcludedFiles": len(flipper_cutoff),
        "flipperMissingProvenanceFiles": len(flipper_unproven),
        "flipperLicenseCutoffCommit": exclusion_doc.get("licenseCutoffCommit"),
        "sourceStatus": {k:v["status"] for k,v in locked.items()},
        "pinnedShas": {k:v["commitSha"] for k,v in locked.items()},
        "validationStatus": "PASS" if all(SHA_RE.fullmatch(p["sourceCommitSha"]) and p["sourcePath"] for p in profiles) else "FAIL"}
    return profiles, report


def is_transmittable(profile: dict) -> bool:
    """Fail-closed validation for imported timing payloads; encoded blobs are not waveforms."""
    if profile.get("encodingType") != "IMPORTED_RAW": return False
    commands = profile.get("rawCommands")
    if not isinstance(commands, dict) or not commands: return False
    for command in commands.values():
        if not isinstance(command, dict): return False
        frequency, timings = command.get("carrierFrequencyHz"), command.get("durationsMicros")
        if isinstance(frequency, bool) or not isinstance(frequency, int) or not 1 <= frequency <= 500_000: return False
        if not isinstance(timings, list) or not timings or len(timings) > 4096 or len(timings) % 2: return False
        if any(isinstance(v, bool) or not isinstance(v, int) or not 0 < v <= 1_000_000 for v in timings): return False
        if sum(timings) > 120_000_000: return False
    return True


def to_mobile_index(profiles: list[dict]) -> dict:
    index_keys = ("id", "brand", "normalizedBrand", "aliases", "acModel", "remoteModel", "protocolId", "protocolModel",
                  "encodingType", "capabilities", "temperatureRange", "fanModes", "operationModes", "verticalSwingCapabilities",
                  "horizontalSwingCapabilities", "specialCapabilities", "source", "sourceCommitSha", "sourcePath", "verificationStatus", "sourceMetadata")
    return {"schemaVersion": "1.0", "profiles": [{key: profile[key] for key in index_keys if key in profile} for profile in profiles]}


def main() -> None:
    profiles, report = build(); OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "ir_catalog.json").write_text(json.dumps({"schemaVersion":"1.0","profiles":profiles}, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    (OUT / "catalog_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    index = to_mobile_index(profiles)
    MOBILE_INDEX.parent.mkdir(parents=True, exist_ok=True)
    MOBILE_INDEX.write_text(json.dumps(index, ensure_ascii=False, separators=(",", ":"))+"\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__": main()
