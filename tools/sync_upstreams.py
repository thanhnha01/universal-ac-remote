#!/usr/bin/env python3
"""Pin candidate upstream SHAs or materialize only the selected locked paths."""
import argparse
import json
import os
import shutil
import subprocess
import tempfile
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOCK = ROOT / "upstream-lock.json"
CANDIDATES = ROOT / "data/upstreams/candidate-shas.json"
SNAPSHOTS = ROOT / "data/upstreams/snapshots"


def git(repo: Path, *args: str, capture=True) -> str:
    result = subprocess.run(["git", "-C", str(repo), *args], check=True,
                            text=True, stdout=subprocess.PIPE if capture else None)
    return result.stdout.strip() if capture else ""


def clone_repo(source: dict, work: Path) -> Path:
    repo = work / source["name"]
    subprocess.run(["git", "clone", "--filter=blob:none", "--no-checkout", source["repositoryUrl"], str(repo)], check=True)
    sha = source["commitSha"]
    git(repo, "fetch", "--filter=blob:none", "origin", sha)
    return repo


def update_candidates() -> None:
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    candidates = []
    for source in lock["sources"]:
        # ls-remote obtains the branch tip; this only records a candidate and never promotes it.
        output = subprocess.check_output(["git", "ls-remote", source["repositoryUrl"], f"refs/heads/{source['branch']}"], text=True)
        sha = output.split()[0]
        candidates.append({"name": source["name"], "branch": source["branch"], "candidateSha": sha,
                           "observedAt": datetime.now(timezone.utc).isoformat()})
    CANDIDATES.parent.mkdir(parents=True, exist_ok=True)
    CANDIDATES.write_text(json.dumps({"candidates": candidates}, indent=2)+"\n", encoding="utf-8")


def promote_candidates(validation_report: Path) -> None:
    report = json.loads(validation_report.read_text(encoding="utf-8"))
    candidates_doc = json.loads(CANDIDATES.read_text(encoding="utf-8"))
    if report.get("validationStatus") != "PASS":
        raise ValueError("cannot promote: catalog validationStatus is not PASS")
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    by_name = {s["name"]: s for s in lock["sources"]}
    validated = report.get("pinnedShas", {})
    for candidate in candidates_doc["candidates"]:
        name, sha = candidate["name"], candidate["candidateSha"]
        if validated.get(name) != sha:
            raise ValueError(f"cannot promote {name}: validation report does not validate candidate SHA {sha}")
        by_name[name]["commitSha"] = sha
        by_name[name]["fetchedAt"] = candidate["observedAt"][:10]
    LOCK.write_text(json.dumps(lock, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")


def first_add_map(repo: Path) -> dict[str, str]:
    # One history traversal for the directory, rather than spawning git once per file.
    output = git(repo, "log", "--reverse", "--diff-filter=A", "--format=COMMIT:%H", "--name-only", "--", "ACs")
    current = None
    added = {}
    for line in output.splitlines():
        if line.startswith("COMMIT:"):
            current = line.split(":", 1)[1]
        elif line.strip() and current and line.startswith("ACs/"):
            added.setdefault(line.strip(), current)
    return added


def first_add_is_cc0_safe(ancestry_exit_code: int | None) -> bool:
    # Unknown/error is fail-closed. The cutoff commit must be an ancestor of the add.
    return ancestry_exit_code == 0


def sync_locked() -> None:
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    work_root = ROOT / "data/upstreams"
    work_root.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix=".sync-work-", dir=work_root) as temp_name:
        temp = Path(temp_name)
        staged = temp / "snapshots"
        staged.mkdir()
        exclusions = []
        flipper_cutoff_sha = None
        for source in lock["sources"]:
            name = source["name"]
            target = staged / name
            target.mkdir()
            if source["status"] == "EXCLUDED_LICENSE_UNCLEAR":
                (target / "exclusion.json").write_text(json.dumps({"status": source["status"], "reason": "license unclear; no files ingested"}, indent=2)+"\n", encoding="utf-8")
                continue
            with tempfile.TemporaryDirectory(prefix=f"{name}-") as repo_temp:
                repo = clone_repo(source, Path(repo_temp))
                if name == "flipper-irdb":
                    git(repo, "sparse-checkout", "init", "--cone")
                    git(repo, "sparse-checkout", "set", "ACs")
                    git(repo, "checkout", "--detach", source["commitSha"])
                    cutoff = git(repo, "rev-parse", "2319685^{commit}")
                    flipper_cutoff_sha = cutoff
                    first_adds = first_add_map(repo)
                    paths = subprocess.check_output(["git", "-C", str(repo), "ls-tree", "-r", "--name-only", source["commitSha"], "--", "ACs"], text=True).splitlines()
                    for rel in paths:
                        if not rel.lower().endswith(".ir"): continue
                        try:
                            added = first_adds.get(rel)
                            if not added: raise ValueError("no first-add commit found in AC history")
                            ancestry = subprocess.run(["git", "-C", str(repo), "merge-base", "--is-ancestor", cutoff, added]).returncode
                            safe = first_add_is_cc0_safe(ancestry)
                            if not safe:
                                exclusions.append({"source": name, "path": rel, "reason": "first-add commit predates CC0 cutoff 2319685"}); continue
                            dest = target / rel
                            dest.parent.mkdir(parents=True, exist_ok=True)
                            dest.write_bytes((repo / rel).read_bytes())
                        except Exception as exc:
                            exclusions.append({"source": name, "path": rel, "reason": f"first-add provenance not proven: {exc}"})
                else:
                    git(repo, "sparse-checkout", "init", "--cone")
                    git(repo, "sparse-checkout", "set", "LICENSE", "codes/climate")
                    git(repo, "checkout", "--detach", source["commitSha"])
                    # Use explicit tree listing and only copy climate JSON plus license text.
                    for rel in subprocess.check_output(["git", "-C", str(repo), "ls-tree", "-r", "--name-only", source["commitSha"]], text=True).splitlines():
                        if name == "smartir" and (rel.startswith("codes/climate/") and rel.endswith(".json") or rel == "LICENSE"):
                            dest = target / rel; dest.parent.mkdir(parents=True, exist_ok=True); dest.write_bytes((repo / rel).read_bytes())
                metadata = {"source": name, "repositoryUrl": source["repositoryUrl"], "commitSha": source["commitSha"], "licenseId": source["licenseId"], "status": source["status"], "exclusions": [e for e in exclusions if e["source"] == name]}
                if name == "flipper-irdb": metadata["licenseCutoffRef"] = "2319685"; metadata["licenseCutoffCommit"] = flipper_cutoff_sha
                (target / "source.json").write_text(json.dumps(metadata, indent=2)+"\n", encoding="utf-8")
        final = SNAPSHOTS
        backup = temp / "previous"
        if final.exists(): shutil.move(str(final), str(backup))
        final.parent.mkdir(parents=True, exist_ok=True)
        shutil.move(str(staged), str(final))
        (ROOT / "data/upstreams/flipper-exclusions.json").write_text(json.dumps({"licenseCutoffCommit": flipper_cutoff_sha, "excludedFiles": exclusions}, indent=2)+"\n", encoding="utf-8")
        if os.name == "nt":
            user = os.environ.get("USERNAME")
            if user:
                subprocess.run(["icacls", str(final), "/inheritance:e", "/grant", f"{user}:(OI)(CI)M", "/T", "/Q"], check=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--update-candidates", action="store_true", help="resolve branch tips to candidate SHAs; does not change lock")
    group.add_argument("--sync-locked", action="store_true", help="fetch exact lock SHAs and stage selected source files")
    group.add_argument("--promote-candidates", action="store_true", help="promote only candidate SHAs explicitly validated by a PASS report")
    parser.add_argument("--validation-report", type=Path)
    args = parser.parse_args()
    if args.update_candidates: update_candidates()
    elif args.sync_locked: sync_locked()
    else:
        if not args.validation_report: parser.error("--promote-candidates requires --validation-report")
        promote_candidates(args.validation_report)


if __name__ == "__main__": main()
