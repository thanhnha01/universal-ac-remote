#!/usr/bin/env python3
"""Build a small, provenance preserving AC catalog from pinned local snapshots."""
from __future__ import annotations

import base64
import hashlib
import html
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
IRREMOTE_SUPPORTED = ROOT / "data/upstreams/snapshots/irremoteesp8266/SupportedProtocols.md"
SHA_RE = re.compile(r"^[0-9a-f]{40}$")

def locked_source_sha(name: str) -> str:
    lock = json.loads((ROOT / "upstream-lock.json").read_text(encoding="utf-8"))
    source = next((item for item in lock["sources"] if item["name"] == name), None)
    if not source or not SHA_RE.fullmatch(source.get("commitSha", "")):
        raise ValueError(f"{name}: upstream lock needs a full commit SHA")
    return source["commitSha"]

PINNED_IRREMOTE_SHA = locked_source_sha("irremoteesp8266")
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
        "id": f"{source}:{source_id}", "sourceProfileId": source_id, "brand": brand,
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


def parse_protocol_registry(text: str, state_text: str | None = None) -> list[dict]:
    """Read the reviewed app allowlist used by both native encoding and catalog import."""
    state_text = state_text if state_text is not None else STATE.read_text(encoding="utf-8")
    mode_match = re.search(r"enum class AcMode[^\{]*\{([^}]+)\}", state_text)
    fan_match = re.search(r"enum class AcFan[^\{]*\{([^}]+)\}", state_text)
    if not mode_match or not fan_match:
        raise ValueError("Could not read AcMode/AcFan registry enums")
    modes = [v.lower() for v in re.findall(r"\b([A-Z][A-Z0-9_]*)\s*\(", mode_match.group(1))]
    fans = ["low" if v == "min" else v.lower() for v in re.findall(r"\b([A-Z][A-Z0-9_]*)\s*\(", fan_match.group(1))]
    pattern = re.compile(
        r'ProtocolDefinition\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*'
        r'(null|"[^"]+")?,\s*(setOf\([^)]*\)|emptySet\(\)),\s*(\d+),\s*(\d+),\s*'
        r'commonModes,\s*commonFans,\s*(true|false),\s*(true|false)\)'
    )
    definitions = []
    for match in pattern.finditer(text):
        app_id, upstream, protocol_name, brand, default_raw, modelset, lo, hi, vs, hs = match.groups()
        default_model = None if not default_raw or default_raw == "null" else default_raw.strip('"')
        model_ids = re.findall(r'"([^"]+)"', modelset)
        if default_model and default_model not in model_ids:
            model_ids.insert(0, default_model)
        definitions.append({
            "appId": app_id,
            "upstreamProtocol": upstream,
            "protocolName": protocol_name,
            "manufacturer": brand,
            "defaultModel": default_model,
            "modelIds": model_ids,
            "minC": int(lo),
            "maxC": int(hi),
            "modes": modes,
            "fans": fans,
            "verticalSwing": vs == "true",
            "horizontalSwing": hs == "true",
        })
    if not definitions:
        raise ValueError("No ProtocolDefinition entries found")
    return definitions


def _definition_capabilities(definition: dict) -> list[str]:
    return [
        "power",
        *[f"mode:{value}" for value in definition["modes"]],
        *[f"fan:{value}" for value in definition["fans"]],
        *(["swing:vertical"] if definition["verticalSwing"] else []),
        *(["swing:horizontal"] if definition["horizontalSwing"] else []),
    ]


def _profile_from_protocol_definition(definition: dict, sha: str) -> dict:
    profile = base_profile(
        source="irremoteesp8266", sha=sha,
        path="app/src/main/java/com/thanhnha/universalacremote/ir/ProtocolRegistry.kt",
        source_id=definition["appId"], brand=definition["manufacturer"], ac_model=None,
        remote_model=definition["defaultModel"], protocol_id=definition["upstreamProtocol"],
        variant=definition["defaultModel"], encoding="PROTOCOL",
        capabilities=_definition_capabilities(definition),
        temp={"minC": definition["minC"], "maxC": definition["maxC"]},
        fans=definition["fans"], modes=definition["modes"],
        v_swing={"type": "ON_OFF" if definition["verticalSwing"] else "NONE", "positions": []},
        h_swing={"type": "ON_OFF" if definition["horizontalSwing"] else "NONE", "positions": []},
        special=[], verification="candidate")
    profile["sourceMetadata"] = {
        "catalogOrigin": "ProtocolRegistry.kt",
        "appProtocolId": definition["appId"],
    }
    return profile


def parse_irremote_registry(text: str, sha: str = PINNED_IRREMOTE_SHA, state_text: str | None = None) -> list[dict]:
    return [_profile_from_protocol_definition(definition, sha)
            for definition in parse_protocol_registry(text, state_text)]


def _irremote_text(value: str) -> str:
    value = re.sub(r"\[([^]]+)\]\([^)]*\)", r"\1", value)
    value = re.sub(r"\*\*", "", value)
    return html.unescape(value).strip()


def _irremote_family_key(upstream_protocol: str) -> str:
    return normalize_brand(re.sub(r"_AC$", "", upstream_protocol, flags=re.I))


def _supported_send_ids(text: str) -> set[str]:
    section = re.search(r"## Send & decodable protocols:\s*\n(.*?)(?=\n## |\Z)", text, re.S)
    if not section:
        raise ValueError("Could not locate IRremoteESP8266 send protocol inventory")
    return set(re.findall(r"^- ([A-Z0-9_]+)$", section.group(1), re.M))


def _annotation_protocol_ids(device_text: str, send_ids: set[str]) -> list[str]:
    annotations = " ".join(re.findall(r"\(([^)]*)\)", device_text))
    tokens = re.findall(r"\b[A-Z][A-Z0-9_]+\b", annotations)
    return [token for token in tokens if token in send_ids]


def _model_variant(device_text: str, definition: dict) -> str | None:
    annotations = " ".join(re.findall(r"\(([^)]*)\)", device_text))
    for model in sorted(definition["modelIds"], key=len, reverse=True):
        if re.search(rf"(?<![A-Za-z0-9]){re.escape(model)}(?![A-Za-z0-9])", annotations, re.I):
            return model
    return definition["defaultModel"]


def _has_unknown_explicit_variant(device_text: str, definition: dict) -> bool:
    """Fail closed when an explicit protocol annotation names a model we have not reviewed."""
    if not definition["modelIds"]:
        return False
    upstream = definition["upstreamProtocol"]
    for annotation in re.findall(r"\(([^)]*)\)", device_text):
        if not re.search(rf"(?<![A-Z0-9_]){re.escape(upstream)}(?![A-Z0-9_])", annotation):
            continue
        if any(re.search(rf"(?<![A-Za-z0-9]){re.escape(model)}(?![A-Za-z0-9])", annotation, re.I)
               for model in definition["modelIds"]):
            return False
        remainder = re.sub(rf"(?<![A-Z0-9_]){re.escape(upstream)}(?![A-Z0-9_])", "", annotation)
        remainder = re.sub(r"^[\s\-:/]+|[\s\-:/]+$", "", remainder)
        if re.search(r"[A-Za-z]", remainder):
            return True
    return False


def _clean_irremote_device_label(device_text: str, definition: dict, send_ids: set[str]) -> str:
    model_ids = set(definition["modelIds"])
    def keep_or_drop(match: re.Match[str]) -> str:
        inner = match.group(1)
        tokens = set(re.findall(r"\b[A-Z][A-Z0-9_]+\b", inner))
        if tokens & send_ids:
            return ""
        if any(re.search(rf"(?<![A-Za-z0-9]){re.escape(model)}(?![A-Za-z0-9])", inner, re.I)
               for model in model_ids):
            return ""
        return match.group(0)
    label = re.sub(r"\(([^)]*)\)", keep_or_drop, device_text)
    label = re.sub(r"\bremote\b", "", label, flags=re.I)
    label = re.sub(r"\bA/C\b", "", label, flags=re.I)
    label = re.sub(r"\s{2,}", " ", label).strip(" -;/")
    return label


def _irremote_reference_profile(
    *,
    sha: str,
    family: str,
    brand: str,
    device_text: str,
    label: str,
    is_remote: bool,
    protocol_ids: list[str],
    reason: str,
) -> dict:
    identity = "|".join([
        family, brand, label, ",".join(protocol_ids), "remote" if is_remote else "ac", "reference"
    ])
    source_id = "catalog-ref-" + hashlib.sha256(identity.encode("utf-8")).hexdigest()[:16]
    profile = base_profile(
        source="irremoteesp8266", sha=sha,
        path="data/upstreams/snapshots/irremoteesp8266/SupportedProtocols.md",
        source_id=source_id, brand=brand,
        ac_model=None if is_remote else label,
        remote_model=label if is_remote else None,
        protocol_id=protocol_ids[0] if len(protocol_ids) == 1 else None,
        variant=None, encoding="IRREMOTE_REFERENCE", capabilities=[],
        temp=None, fans=[], modes=[],
        v_swing={"type": "NONE", "positions": []},
        h_swing={"type": "NONE", "positions": []},
        special=[], verification="needsSupport")
    profile["sourceMetadata"] = {
        "catalogOrigin": "SupportedProtocols.md",
        "upstreamFamily": family,
        "upstreamDeviceText": device_text,
        "upstreamProtocolIds": protocol_ids,
        "supportReason": reason,
    }
    return profile


def parse_irremote_supported_protocols(
    supported_text: str,
    registry_text: str,
    sha: str = PINNED_IRREMOTE_SHA,
    state_text: str | None = None,
) -> list[dict]:
    """Import the complete upstream Detailed A/C inventory.

    Rows backed by an already-reviewed ProtocolRegistry definition become
    transmittable PROTOCOL profiles. Every other A/C model/remote is still
    preserved as an IRREMOTE_REFERENCE profile so the app can search and show
    the library inventory without claiming that the APK can transmit it.
    """
    definitions = parse_protocol_registry(registry_text, state_text)
    by_upstream = {definition["upstreamProtocol"]: definition for definition in definitions}
    family_map = {_irremote_family_key(definition["upstreamProtocol"]): definition
                  for definition in definitions}
    send_ids = _supported_send_ids(supported_text)
    records = []

    for line in supported_text.splitlines():
        if not line.startswith("|"):
            continue
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if len(cells) != 5 or cells[4].strip() != "Yes":
            continue
        family_raw, brand_raw, models_raw, _ac_models_raw, _ = cells
        family = _irremote_text(family_raw)
        brand = _irremote_text(brand_raw)
        fallback = family_map.get(normalize_brand(family))

        for raw_item in re.split(r"<BR\s*/?>", models_raw, flags=re.I):
            device_text = _irremote_text(raw_item)
            if not device_text:
                continue
            if re.search(r"\b(projector|\bTV\b|stand fan|soundbar|blu-?ray|transmitter IC|cooker hood|toilet)\b",
                         device_text, re.I):
                continue

            explicit_ids = _annotation_protocol_ids(device_text, send_ids)
            active_explicit = next((protocol_id for protocol_id in explicit_ids if protocol_id in by_upstream), None)
            definition = by_upstream.get(active_explicit) if active_explicit else (fallback if not explicit_ids else None)
            is_remote = re.search(r"\bremote\b", device_text, re.I) is not None

            if definition is not None and not _has_unknown_explicit_variant(device_text, definition):
                variant = _model_variant(device_text, definition)
                label = _clean_irremote_device_label(device_text, definition, send_ids)
                if not label:
                    continue
                identity = "|".join([
                    family, brand, label, definition["upstreamProtocol"], variant or "", "remote" if is_remote else "ac"
                ])
                source_id = "catalog-" + hashlib.sha256(identity.encode("utf-8")).hexdigest()[:16]
                profile = base_profile(
                    source="irremoteesp8266", sha=sha,
                    path="data/upstreams/snapshots/irremoteesp8266/SupportedProtocols.md",
                    source_id=source_id, brand=brand,
                    ac_model=None if is_remote else label,
                    remote_model=label if is_remote else None,
                    protocol_id=definition["upstreamProtocol"], variant=variant,
                    encoding="PROTOCOL", capabilities=_definition_capabilities(definition),
                    temp={"minC": definition["minC"], "maxC": definition["maxC"]},
                    fans=definition["fans"], modes=definition["modes"],
                    v_swing={"type": "ON_OFF" if definition["verticalSwing"] else "NONE", "positions": []},
                    h_swing={"type": "ON_OFF" if definition["horizontalSwing"] else "NONE", "positions": []},
                    special=[], verification="candidate")
                profile["sourceMetadata"] = {
                    "catalogOrigin": "SupportedProtocols.md",
                    "upstreamFamily": family,
                    "upstreamDeviceText": device_text,
                    "upstreamProtocolIds": explicit_ids,
                    "appProtocolId": definition["appId"],
                    "transmissionSupport": "enabled",
                }
                records.append(profile)
                continue

            # Preserve unsupported/unknown variants as searchable library data.
            # Use a generic label cleaner so protocol annotations do not become
            # part of the user-visible model/remote name.
            label = device_text
            for annotation in re.findall(r"\(([^)]*)\)", device_text):
                tokens = set(re.findall(r"\b[A-Z][A-Z0-9_]+\b", annotation))
                if tokens & send_ids:
                    label = label.replace(f"({annotation})", "")
            label = re.sub(r"\bremote\b", "", label, flags=re.I)
            label = re.sub(r"\bA/C\b", "", label, flags=re.I)
            label = re.sub(r"\s{2,}", " ", label).strip(" -;/")
            if not label:
                continue

            if explicit_ids:
                reason = "Protocol/variant exists upstream but is not enabled in this APK."
            elif fallback is None:
                reason = "Detailed A/C family exists upstream but has no reviewed app protocol mapping yet."
            else:
                reason = "Upstream model variant is not in the reviewed app model allowlist."
            records.append(_irremote_reference_profile(
                sha=sha, family=family, brand=brand, device_text=device_text,
                label=label, is_remote=is_remote, protocol_ids=explicit_ids, reason=reason,
            ))

    unique = {}
    for record in records:
        unique.setdefault(record["id"], record)
    return list(unique.values())

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
    normalized_swing = {v.casefold() for v in swing_modes}
    # SmartIR commonly encodes swing state as an explicit branch such as
    # off/vertical/horizontal/both. This is enough evidence for ON/OFF support
    # of each axis, but not for discrete vane positions.
    vertical = {"type":"ON_OFF" if {"vertical", "both"} & normalized_swing else "NONE", "positions":[]}
    horizontal = {"type":"ON_OFF" if {"horizontal", "both"} & normalized_swing else "NONE", "positions":[]}
    standard_commands = {"off", "on", *modes, *fans}
    special = sorted(k for k in commands if isinstance(k, str) and k.casefold() not in standard_commands)
    try: source_path = path.relative_to(ROOT / "data/upstreams/snapshots/smartir").as_posix()
    except ValueError: source_path = path.name
    record = base_profile(source="smartir", sha=sha, path=source_path, source_id=path.stem,
        brand=brand, ac_model=", ".join(obj.get("supportedModels", [])) or None,
        remote_model=None, protocol_id=None, variant=None, encoding="RAW_PROFILE",
        capabilities=[*[f"mode:{x}" for x in modes], *[f"fan:{x}" for x in fans],
            *(["power"] if "off" in commands or "on" in commands else []),
            *(["swing:vertical"] if vertical["type"] != "NONE" else []),
            *(["swing:horizontal"] if horizontal["type"] != "NONE" else []),
            *[f"special:{x}" for x in special]],
        temp={"minC": obj["minTemperature"], "maxC": obj["maxTemperature"]}, fans=fans, modes=modes,
        v_swing=vertical, h_swing=horizontal,
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
        if not raw or len(raw) > 4096: raise ValueError("invalid RAW timing count")
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
        key = (p["normalizedBrand"], p["sourcePath"], p.get("sourceProfileId"), p["acModel"], p["remoteModel"], p["protocolId"], p["protocolModel"])
        groups.setdefault(key, []).append(p["id"])
    return [{"identity": list(k), "profileIds": ids} for k, ids in groups.items() if len(ids) > 1]


def build() -> tuple[list[dict], dict]:
    lock = json.loads((ROOT / "upstream-lock.json").read_text(encoding="utf-8"))
    locked = {s["name"]: s for s in lock["sources"]}
    sha_irremote = locked["irremoteesp8266"]["commitSha"]
    registry_text = REGISTRY.read_text(encoding="utf-8")
    profiles = parse_irremote_registry(registry_text, sha_irremote)
    try:
        profiles.extend(parse_irremote_supported_protocols(
            IRREMOTE_SUPPORTED.read_text(encoding="utf-8"),
            registry_text,
            sha_irremote,
        ))
    except Exception as exc:
        raise ValueError(f"irremoteesp8266 SupportedProtocols import failed: {exc}") from exc
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
    for profile in profiles:
        if profile["encodingType"] in ("RAW_PROFILE", "IMPORTED_RAW"):
            profile["verificationStatus"] = "transmittable" if is_transmittable(profile) else "unsupported"
    profiles.sort(key=lambda p: p["id"])
    counts = {s: sum(p["source"] == s for p in profiles) for s in ("irremoteesp8266", "smartir", "flipper-irdb", "irplus")}
    flipper_excluded = [x for x in excluded if x.get("source") == "flipper-irdb"]
    flipper_cutoff = [x for x in flipper_excluded if "predates CC0 cutoff" in x.get("reason", "")]
    flipper_unproven = [x for x in flipper_excluded if "provenance not proven" in x.get("reason", "")]
    report = {"totalBrands": len({p["normalizedBrand"] for p in profiles}),
        "transmittableBrands": len({p["normalizedBrand"] for p in profiles if p["encodingType"] == "PROTOCOL" or is_transmittable(p)}),
        "totalProfiles": len(profiles),
        "protocolProfiles": sum(p["encodingType"] == "PROTOCOL" for p in profiles),
        "rawProfiles": sum(p["encodingType"] != "PROTOCOL" for p in profiles),
        "irremoteesp8266GenericProfiles": sum(
            p["source"] == "irremoteesp8266" and p["sourcePath"].endswith("ProtocolRegistry.kt") for p in profiles
        ),
        "irremoteesp8266CatalogProfiles": sum(
            p["source"] == "irremoteesp8266" and p["sourcePath"].endswith("SupportedProtocols.md") for p in profiles
        ),
        "irremoteesp8266UsableCatalogProfiles": sum(
            p["source"] == "irremoteesp8266" and p["sourcePath"].endswith("SupportedProtocols.md")
            and p["encodingType"] == "PROTOCOL" for p in profiles
        ),
        "irremoteesp8266ReferenceProfiles": sum(
            p["source"] == "irremoteesp8266" and p["encodingType"] == "IRREMOTE_REFERENCE" for p in profiles
        ),
        "smartirTotal": sum(p["source"] == "smartir" for p in profiles),
        "smartirTransmittable": sum(p["source"] == "smartir" and is_transmittable(p) for p in profiles),
        "smartirUnsupported": sum(p["source"] == "smartir" and not is_transmittable(p) for p in profiles),
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


def decode_broadlink(command: str) -> list[int]:
    """Decode one SmartIR Broadlink Base64 command into validated microseconds."""
    try:
        packet = base64.b64decode(command, validate=True)
    except (ValueError, base64.binascii.Error) as exc:
        raise ValueError("invalid Broadlink Base64 payload") from exc
    if len(packet) < 4 or packet[0] != 0x26:
        raise ValueError("unsupported Broadlink packet type")
    data_length = int.from_bytes(packet[2:4], "little")
    if data_length <= 0 or data_length + 4 > len(packet):
        raise ValueError("malformed Broadlink packet length")
    if any(packet[data_length + 4:]):
        raise ValueError("malformed Broadlink trailing data")
    result: list[int] = []
    index, end = 4, data_length + 4
    while index < end:
        value = packet[index]
        index += 1
        if value == 0:
            if index + 1 >= end:
                raise ValueError("truncated Broadlink timing")
            value = int.from_bytes(packet[index:index + 2], "big")
            index += 2
        if value <= 0:
            raise ValueError("non-positive Broadlink timing")
        result.append(int(value * 32.84))
    return result


def _command_leaves(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from _command_leaves(child)
    else:
        raise ValueError("malformed SmartIR command tree")


def _valid_timings(frequency: int, timings) -> bool:
    if not isinstance(timings, list) or not timings or len(timings) > 4096: return False
    if any(isinstance(v, bool) or not isinstance(v, int) or not 0 < v <= 1_000_000 for v in timings): return False
    return 1 <= frequency <= 500_000 and sum(timings) <= 120_000_000


def is_transmittable(profile: dict) -> bool:
    """Fail-closed validation for raw timings and SmartIR Broadlink commands."""
    if profile.get("encodingType") == "RAW_PROFILE":
        metadata = profile.get("sourceMetadata") or {}
        if str(metadata.get("supportedController", "")).casefold() != "broadlink": return False
        if str(metadata.get("commandsEncoding", "")).casefold() != "base64": return False
        try:
            commands = profile.get("rawCommands")
            if not isinstance(commands, dict) or not commands: return False
            for encoded in _command_leaves(commands):
                if not _valid_timings(38_000, decode_broadlink(encoded)): return False
            return True
        except (TypeError, ValueError):
            return False
    if profile.get("encodingType") != "IMPORTED_RAW": return False
    commands = profile.get("rawCommands")
    if not isinstance(commands, dict) or not commands: return False
    for command in commands.values():
        if not isinstance(command, dict): return False
        frequency, timings = command.get("carrierFrequencyHz"), command.get("durationsMicros")
        if isinstance(frequency, bool) or not isinstance(frequency, int): return False
        if not _valid_timings(frequency, timings): return False
    return True


def to_mobile_index(profiles: list[dict]) -> dict:
    index_keys = ("id", "sourceProfileId", "brand", "normalizedBrand", "aliases", "acModel", "remoteModel", "protocolId", "protocolModel",
                  "encodingType", "capabilities", "temperatureRange", "fanModes", "operationModes", "verticalSwingCapabilities",
                  "horizontalSwingCapabilities", "specialCapabilities", "source", "sourceCommitSha", "sourcePath", "verificationStatus", "sourceMetadata")
    mobile_profiles = []
    for profile in profiles:
        item = {key: profile[key] for key in index_keys if key in profile}
        if profile.get("encodingType") != "PROTOCOL" and "rawCommands" in profile:
            item["rawCommands"] = profile["rawCommands"]
        mobile_profiles.append(item)
    return {"schemaVersion": "1.0", "profiles": mobile_profiles}


def main() -> None:
    profiles, report = build(); OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "ir_catalog.json").write_text(json.dumps({"schemaVersion":"1.0","profiles":profiles}, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    (OUT / "catalog_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    index = to_mobile_index(profiles)
    MOBILE_INDEX.parent.mkdir(parents=True, exist_ok=True)
    MOBILE_INDEX.write_text(json.dumps(index, ensure_ascii=False, separators=(",", ":"))+"\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__": main()
