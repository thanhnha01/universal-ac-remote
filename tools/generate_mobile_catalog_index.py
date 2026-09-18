#!/usr/bin/env python3
"""Generate the app's compact searchable index from the validated Unified Catalog."""
from __future__ import annotations

import json
from pathlib import Path

import build_ir_catalog

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "data/generated/ir_catalog.json"
OUTPUT = ROOT / "app/src/main/assets/catalog-index.json"


def main() -> None:
    catalog = json.loads(SOURCE.read_text(encoding="utf-8"))
    index = build_ir_catalog.to_mobile_index(catalog["profiles"])
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(index, ensure_ascii=False, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"Wrote {len(index['profiles'])} indexed profiles to {OUTPUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
