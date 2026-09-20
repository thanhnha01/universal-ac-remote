#!/usr/bin/env python3
"""Generate the pinned upstream Detailed A/C catalog and generic-readiness report.

Usage: python tools/generate_protocol_catalog.py [SupportedProtocols.md] [output.md]
The default input is the reviewed snapshot in data/upstreams/snapshots/irremoteesp8266/SupportedProtocols.md.
This tool intentionally reports candidates; it never enables an unreviewed sender.
"""
from __future__ import annotations

import html
import re
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PIN = "1e2f0f3ef0a93cbf2a8ddb2e95130f8f4c584b3f"
REGISTRY = ROOT / "app/src/main/java/com/thanhnha/universalacremote/ir/ProtocolRegistry.kt"
IRAC = ROOT / "app/src/main/cpp/upstream/IRac.cpp"


def text(value: str) -> str:
    value = re.sub(r"\[([^]]+)\]\([^)]*\)", r"\1", value)
    value = re.sub(r"<br\s*/?>", "; ", value, flags=re.I)
    return html.unescape(re.sub(r"\*\*", "", value)).strip()


def main() -> None:
    source = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "data/upstreams/snapshots/irremoteesp8266/SupportedProtocols.md"
    output = Path(sys.argv[2]) if len(sys.argv) > 2 else ROOT / "docs/PROTOCOL_CATALOG.md"
    protocol_rows: dict[str, dict[str, set[str]]] = defaultdict(lambda: {"brands": set(), "models": set(), "ac_models": set()})
    detailed_ids: set[str] = set()
    detailed_row_count = 0
    for line in source.read_text(encoding="utf-8").splitlines():
        if not line.startswith("|"):
            continue
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if len(cells) != 5 or cells[4].strip() != "Yes":
            continue
        detailed_row_count += 1
        family, brand, models, ac_models, _ = cells
        family = text(family)
        brand = text(brand)
        record = protocol_rows[family]
        record["brands"].add(brand)
        for item in re.split(r"<BR\s*/?>", models, flags=re.I):
            item_text = text(item)
            if item_text:
                record["models"].add(item_text)
            if re.search(r"\b(projector|\bTV\b|stand fan|soundbar|bluray|transmitter IC)\b", item_text, re.I):
                continue
            detailed_ids.update(re.findall(r"\(([A-Z][A-Z0-9_]+)\)", item_text))
        record["ac_models"].update(filter(None, (text(v) for v in re.split(r"<BR\s*/?>", ac_models, flags=re.I))))

    if not protocol_rows:
        raise SystemExit(f"No Detailed A/C rows found in {source}")
    registry = REGISTRY.read_text(encoding="utf-8")
    active_definitions = re.findall(
        r'ProtocolDefinition\("([^"]+)",\s*"([A-Z0-9_]+)",\s*"([^"]+)"', registry
    )
    active_ids = {upstream_id for _, upstream_id, _ in active_definitions}
    active_families = set()
    for protocol in re.findall(r'ProtocolDefinition\("[^"]+",\s*"([A-Z0-9_]+)",\s*"([^"]+)"', registry):
        active_families.add(protocol[1].casefold())
        active_families.add(re.sub(r"_AC$", "", protocol[0], flags=re.I).casefold())

    irac_block = re.search(r"bool IRac::isProtocolSupported\(.*?\n}\n", IRAC.read_text(encoding="utf-8"), re.S)
    if not irac_block:
        raise SystemExit("Could not locate IRac::isProtocolSupported in pinned source")
    irac_ids = set(re.findall(r"case decode_type_t::([A-Z0-9_]+)", irac_block.group(0)))

    catalog_text = source.read_text(encoding="utf-8")
    send_section = re.search(r"## Send & decodable protocols:\s*\n(.*?)(?=\n## |\Z)", catalog_text, re.S)
    if not send_section:
        raise SystemExit("Could not locate the upstream Send & decodable protocols list")
    upstream_send_ids = set(re.findall(r"^- ([A-Z0-9_]+)$", send_section.group(1), re.M))
    detailed_ids.intersection_update(upstream_send_ids)

    generic, special = [], []
    for family, record in sorted(protocol_rows.items(), key=lambda item: item[0].casefold()):
        name = family.casefold()
        if name in active_families or name.replace(" ", "") in active_families:
            generic.append(family)
        else:
            special.append(family)
    not_yet_ids = sorted(protocol_id for protocol_id in detailed_ids if protocol_id not in irac_ids and protocol_id not in active_ids)
    deferred_ids = sorted(protocol_id for protocol_id in detailed_ids if protocol_id in irac_ids and protocol_id not in active_ids)

    out = [
        "# IRremoteESP8266 Detailed A/C protocol catalog",
        "",
        f"Generated from `SupportedProtocols.md` at pinned commit `{PIN}`. This is a support inventory, not a claim that all listed models have been validated on physical hardware.",
        "The upstream document is generated metadata; its embedded source links may point at a moving branch. Use the SHA above and `app/src/main/cpp/UPSTREAM_MANIFEST.txt` for provenance.",
        "",
        f"Detailed A/C rows: {detailed_row_count}; distinct protocol families: {len(protocol_rows)}.",
        "",
        "## SUPPORTED_GENERIC",
        "",
        "These family entries are enabled in this APK and go through the single JNI `encodeAc(protocolId, modelId, state)` path. Exact upstream IDs/model IDs and temp limits are in `ProtocolRegistry.kt`; carrier is collected from upstream `IRsend::enableIROut`.",
        "",
    ]
    for family in generic:
        family_key = re.sub(r"[^a-z0-9]", "", family.casefold())
        matching = [
            f"`{app_id}` → `{upstream_id}`"
            for app_id, upstream_id, _ in active_definitions
            if re.sub(r"[^a-z0-9]", "", re.sub(r"_ac$", "", upstream_id, flags=re.I).casefold()) == family_key
        ]
        out.append(f"- {family}: {', '.join(matching) if matching else 'see ProtocolRegistry'}")
    out += ["", "## SUPPORTED_WITH_SPECIAL_HANDLING", "", "Detailed A/C support exists in the upstream catalog, but the current APK has not enabled these senders. Admission requires source closure/build flags, verified model selection and per-protocol capability/golden vectors. `IRac::isProtocolSupported()` is the authoritative compile-time generic-dispatch allowlist.", ""]
    out.extend(f"- {family}" for family in special)
    out += ["", "Potentially generic through `IRac` after metadata and tests: " + (", ".join(deferred_ids) if deferred_ids else "none detected"), "", "## NOT_YET_SUPPORTED", "", "Protocol variants mentioned by detailed A/C device rows but absent from `IRac::isProtocolSupported()` at the pinned commit cannot use the current common `IRac::sendAc` contract. They need another upstream path or an explicit adapter; the generator never promotes them automatically.", ""]
    out.extend(f"- `{protocol_id}`" for protocol_id in not_yet_ids)
    if not not_yet_ids:
        out.append("- none detected")
    out += ["", "## Source rows", "", "| Upstream family | Brands | Models/devices (upstream text) | A/C model variants |", "|---|---|---|---|"]
    for family, record in sorted(protocol_rows.items(), key=lambda item: item[0].casefold()):
        brands = "; ".join(sorted(record["brands"]))
        models = "; ".join(sorted(record["models"]))
        ac_models = "; ".join(sorted(record["ac_models"]))
        out.append(f"| {family} | {brands} | {models} | {ac_models} |")
    output.write_text("\n".join(out) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
