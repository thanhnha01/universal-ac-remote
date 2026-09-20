#!/usr/bin/env python3
"""Summarize Detailed A/C changes between two IRremoteESP8266 SupportedProtocols snapshots."""
from __future__ import annotations

import argparse
import html
import re
from collections import defaultdict
from pathlib import Path

ID_RE = re.compile(r"\(([A-Z][A-Z0-9_]+)\)")


def clean(value: str) -> str:
    value = re.sub(r"\[([^]]+)\]\([^)]*\)", r"\1", value)
    value = re.sub(r"<br\s*/?>", "; ", value, flags=re.I)
    return html.unescape(re.sub(r"\*\*", "", value)).strip()


def parse(path: Path) -> dict[str, dict[str, set[str]]]:
    result: dict[str, dict[str, set[str]]] = defaultdict(
        lambda: {"brands": set(), "models": set(), "ac_models": set(), "ids": set()}
    )
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.startswith("|"):
            continue
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        if len(cells) != 5 or cells[4].strip() != "Yes":
            continue
        family, brand, models, ac_models, _ = cells
        family = clean(family)
        if not family or family.casefold() in {"protocol", "protocol family"}:
            continue
        record = result[family]
        record["brands"].add(clean(brand))
        for raw in re.split(r"<BR\s*/?>", models, flags=re.I):
            item = clean(raw)
            if item:
                record["models"].add(item)
                if not re.search(r"\b(projector|\bTV\b|stand fan|soundbar|bluray|transmitter IC)\b", item, re.I):
                    record["ids"].update(ID_RE.findall(item))
        for raw in re.split(r"<BR\s*/?>", ac_models, flags=re.I):
            item = clean(raw)
            if item:
                record["ac_models"].add(item)
                record["ids"].update(ID_RE.findall(item))
    return dict(result)


def flatten_ids(data: dict[str, dict[str, set[str]]]) -> set[str]:
    out: set[str] = set()
    for record in data.values():
        out.update(record["ids"])
    return out


def fmt(values: set[str], limit: int = 12) -> str:
    items = sorted(v for v in values if v)
    if not items:
        return "none"
    if len(items) <= limit:
        return ", ".join(items)
    shown = ", ".join(items[:limit])
    return f"{shown}, … (+{len(items) - limit} more)"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("old", type=Path)
    parser.add_argument("new", type=Path)
    args = parser.parse_args()

    old = parse(args.old)
    new = parse(args.new)
    old_families, new_families = set(old), set(new)
    added_families = new_families - old_families
    removed_families = old_families - new_families
    added_ids = flatten_ids(new) - flatten_ids(old)
    removed_ids = flatten_ids(old) - flatten_ids(new)

    changed = []
    for family in sorted(old_families & new_families):
        deltas = {}
        for key in ("brands", "models", "ac_models", "ids"):
            plus = new[family][key] - old[family][key]
            minus = old[family][key] - new[family][key]
            if plus or minus:
                deltas[key] = (plus, minus)
        if deltas:
            changed.append((family, deltas))

    print("### IRremoteESP8266 A/C change summary")
    print()
    print(f"- Detailed A/C protocol families: **{len(old)} → {len(new)}**")
    print(f"- Added families: {fmt(added_families)}")
    print(f"- Removed families: {fmt(removed_families)}")
    print(f"- New A/C protocol IDs referenced upstream: {fmt(added_ids)}")
    print(f"- Removed A/C protocol IDs referenced upstream: {fmt(removed_ids)}")
    print()

    if added_families:
        print("#### Added A/C families")
        print()
        for family in sorted(added_families):
            record = new[family]
            print(f"- **{family}** — brands: {fmt(record['brands'])}; protocol IDs: {fmt(record['ids'])}")
        print()

    if changed:
        print("#### Changed existing A/C families")
        print()
        labels = {"brands": "brands", "models": "models/remotes", "ac_models": "A/C variants", "ids": "protocol IDs"}
        for family, deltas in changed[:25]:
            pieces = []
            for key, (plus, minus) in deltas.items():
                if plus:
                    pieces.append(f"added {labels[key]}: {fmt(plus, 6)}")
                if minus:
                    pieces.append(f"removed {labels[key]}: {fmt(minus, 6)}")
            print(f"- **{family}** — " + "; ".join(pieces))
        if len(changed) > 25:
            print(f"- … and {len(changed) - 25} more changed families; inspect docs/PROTOCOL_CATALOG.md in the PR.")
        print()

    if not (added_families or removed_families or added_ids or removed_ids or changed):
        print("No Detailed A/C catalog rows changed; the upstream commit may contain implementation fixes or non-A/C changes.")
        print()

    print("> This is an upstream inventory diff. A newly listed family/protocol is **not automatically enabled in the app**; native build flags, registry metadata, capabilities, and tests still require review.")


if __name__ == "__main__":
    main()
