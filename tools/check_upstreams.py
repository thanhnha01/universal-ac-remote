#!/usr/bin/env python3
"""Read-only check of active upstream branch tips against exact locked SHAs."""
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SHA = re.compile(r"^[0-9a-f]{40}$")


def check() -> dict:
    lock = json.loads((ROOT / "upstream-lock.json").read_text(encoding="utf-8"))
    results = []
    for source in lock["sources"]:
        if source.get("status") not in ("ACTIVE", "PARTIAL_LICENSE_SAFE"):
            results.append({"name": source["name"], "status": "SKIPPED_LICENSE_OR_INACTIVE"})
            continue
        locked, branch = source.get("commitSha", ""), source.get("branch", "")
        if not SHA.fullmatch(locked) or not branch:
            raise ValueError(f"{source.get('name')}: lock needs a full SHA and branch")
        output = subprocess.check_output(
            ["git", "ls-remote", source["repositoryUrl"], f"refs/heads/{branch}"],
            text=True, timeout=30,
        ).split()
        if not output or not SHA.fullmatch(output[0]):
            raise ValueError(f"{source['name']}: could not resolve branch tip")
        head = output[0]
        results.append({"name": source["name"], "lockedSha": locked, "candidateSha": head,
                        "status": "UNCHANGED" if head == locked else "CHANGED"})
    return {"changed": any(item["status"] == "CHANGED" for item in results), "sources": results}


if __name__ == "__main__":
    try:
        result = check()
        print(json.dumps(result, indent=2))
        sys.exit(0)
    except Exception as error:
        print(f"upstream check failed: {error}", file=sys.stderr)
        sys.exit(1)
