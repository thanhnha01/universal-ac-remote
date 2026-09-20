#!/usr/bin/env python3
"""Materialize a reviewed IRremoteESP8266 candidate while preserving local SWIGLIB hooks."""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import tempfile
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCK = ROOT / "upstream-lock.json"
UPSTREAM_DIR = ROOT / "app/src/main/cpp/upstream"
SNAPSHOT_DIR = ROOT / "data/upstreams/snapshots/irremoteesp8266"
MANIFEST = ROOT / "app/src/main/cpp/UPSTREAM_MANIFEST.txt"
LICENSES = ROOT / "data/upstreams/licenses.json"
DOCS = ROOT / "docs/UPSTREAM_SOURCES.md"
SHA_RE = re.compile(r"^[0-9a-f]{40}$")


def run(*args: str, cwd: Path | None = None, capture: bool = True) -> str:
    result = subprocess.run(
        list(args), cwd=cwd, check=True, text=True,
        stdout=subprocess.PIPE if capture else None,
    )
    return result.stdout.strip() if capture else ""


def source_record() -> dict:
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    source = next(item for item in lock["sources"] if item["name"] == "irremoteesp8266")
    if not SHA_RE.fullmatch(source["commitSha"]):
        raise ValueError("irremoteesp8266 lock does not contain a full SHA")
    return source


def resolve_candidate() -> str:
    source = source_record()
    output = run("git", "ls-remote", source["repositoryUrl"], f"refs/heads/{source['branch']}")
    parts = output.split()
    if not parts or not SHA_RE.fullmatch(parts[0]):
        raise ValueError("could not resolve IRremoteESP8266 branch tip")
    return parts[0]


def patch_irac(text: str) -> str:
    text = text.replace(
        "extern std::vector<int> timingList;\n",
        "extern std::vector<int> timingList;\nextern std::vector<uint32_t> carrierFrequencyList;\n",
        1,
    )
    old = "void IRac::resetTiming(void) { timingList.clear(); }"
    new = """void IRac::resetTiming(void) {
  timingList.clear();
  carrierFrequencyList.clear();
}"""
    if old not in text:
        raise ValueError("IRac.cpp SWIGLIB resetTiming hook no longer matches upstream; manual review required")
    return text.replace(old, new, 1)


def patch_irsend(text: str) -> str:
    text = text.replace(
        "std::vector<int> timingList;\n",
        "std::vector<int> timingList;\nstd::vector<uint32_t> carrierFrequencyList;\n",
        1,
    )
    marker = "#endif  // UNIT_TEST\n  uint32_t period = calcUSecPeriod(freq);"
    replacement = """#endif  // UNIT_TEST
#ifdef SWIGLIB
  carrierFrequencyList.push_back(freq);
#endif  // SWIGLIB
  uint32_t period = calcUSecPeriod(freq);"""
    if marker not in text:
        raise ValueError("IRsend.cpp enableIROut hook no longer matches upstream; manual review required")
    return text.replace(marker, replacement, 1)


def materialize(candidate: str) -> None:
    if not SHA_RE.fullmatch(candidate):
        raise ValueError("candidate must be a full 40-character SHA")
    source = source_record()
    with tempfile.TemporaryDirectory(prefix="irremote-update-") as temp_name:
        temp = Path(temp_name)
        repo = temp / "upstream"
        run("git", "clone", "--filter=blob:none", "--no-checkout", source["repositoryUrl"], str(repo), capture=False)
        run("git", "fetch", "--filter=blob:none", "origin", candidate, cwd=repo, capture=False)

        current_files = sorted(path.relative_to(UPSTREAM_DIR) for path in UPSTREAM_DIR.rglob("*") if path.is_file())
        for rel in current_files:
            upstream_path = f"src/{rel.as_posix()}"
            try:
                text = run("git", "show", f"{candidate}:{upstream_path}", cwd=repo)
            except subprocess.CalledProcessError as exc:
                raise ValueError(f"upstream removed required vendored file: {upstream_path}") from exc
            if rel.as_posix() == "IRac.cpp":
                text = patch_irac(text)
            elif rel.as_posix() == "IRsend.cpp":
                text = patch_irsend(text)
            dest = UPSTREAM_DIR / rel
            dest.write_text(text, encoding="utf-8")

        try:
            license_text = run("git", "show", f"{candidate}:LICENSE.txt", cwd=repo)
            protocols_text = run("git", "show", f"{candidate}:SupportedProtocols.md", cwd=repo)
        except subprocess.CalledProcessError as exc:
            raise ValueError("required upstream provenance files are missing") from exc
        if "GNU LESSER GENERAL PUBLIC LICENSE" not in license_text.upper():
            raise ValueError("IRremoteESP8266 license text no longer matches the reviewed LGPL provenance")
        (ROOT / "app/src/main/cpp/IRremoteESP8266-LICENSE.txt").write_text(license_text + "\n", encoding="utf-8")
        SNAPSHOT_DIR.mkdir(parents=True, exist_ok=True)
        (SNAPSHOT_DIR / "SupportedProtocols.md").write_text(protocols_text + "\n", encoding="utf-8")

    today = datetime.now(timezone.utc).date().isoformat()
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    source = next(item for item in lock["sources"] if item["name"] == "irremoteesp8266")
    old_sha = source["commitSha"]
    source["commitSha"] = candidate
    source["fetchedAt"] = today
    LOCK.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    source_json = {
        "source": "irremoteesp8266",
        "repositoryUrl": source["repositoryUrl"],
        "commitSha": candidate,
        "licenseId": source["licenseId"],
        "status": source["status"],
        "exclusions": [],
    }
    (SNAPSHOT_DIR / "source.json").write_text(json.dumps(source_json, indent=2) + "\n", encoding="utf-8")

    licenses = json.loads(LICENSES.read_text(encoding="utf-8"))
    lic = next(item for item in licenses["licenses"] if item["source"] == "irremoteesp8266")
    lic["commitSha"] = candidate
    LICENSES.write_text(json.dumps(licenses, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    manifest = MANIFEST.read_text(encoding="utf-8")
    manifest = re.sub(r"(?m)^Commit: [0-9a-f]{40}$", f"Commit: {candidate}", manifest, count=1)
    MANIFEST.write_text(manifest, encoding="utf-8")

    docs = DOCS.read_text(encoding="utf-8")
    docs = docs.replace(old_sha, candidate)
    DOCS.write_text(docs, encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--candidate", help="materialize this exact full SHA")
    parser.add_argument("--resolve", action="store_true", help="print current branch tip")
    args = parser.parse_args()
    if args.resolve:
        print(resolve_candidate())
        return
    if not args.candidate:
        parser.error("provide --resolve or --candidate SHA")
    materialize(args.candidate)


if __name__ == "__main__":
    main()
