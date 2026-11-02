#!/usr/bin/env python3
"""Check required IDs, recorded outcomes and artifact/source freshness. Executes no commands."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import sys


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def resolve(root: Path, value: str) -> Path:
    relative = Path(value)
    if relative.is_absolute() or ".." in relative.parts:
        raise ValueError("Evidence/source paths must be project-relative")
    path = (root / relative).resolve()
    if not path.is_relative_to(root.resolve()):
        raise ValueError("Evidence/source escapes project root")
    return path


def inspect(required: dict, evidence: dict, root: Path) -> dict:
    if not isinstance(required, dict) or not isinstance(evidence, dict):
        raise ValueError("Requirements and evidence must be objects")
    ids = required.get("required")
    if not isinstance(ids, list) or not ids or any(not isinstance(i, str) or not i.strip() for i in ids) or len(ids) != len(set(ids)):
        raise ValueError("required must be a nonempty list of unique IDs")
    if type(evidence.get("schema_version")) is not int or evidence["schema_version"] != 1 or not isinstance(evidence.get("requirements"), list):
        raise ValueError("Evidence needs schema_version=1 and requirements list")
    entries = {}
    for item in evidence["requirements"]:
        if not isinstance(item, dict):
            raise ValueError("Each requirement must be an object")
        identifier = item["id"]
        if identifier in entries or identifier not in ids:
            raise ValueError("Duplicate or unknown requirement ID")
        entries[identifier] = item
    report = {"status": "PASS", "scope": "evidence-integrity-not-business-correctness", "required": len(ids),
              "verified_requirements": 0, "issues": []}
    failed = False

    def issue(identifier, code, failure=False):
        nonlocal failed
        failed |= failure
        report["issues"].append({"id": identifier, "code": code})

    def file_check(identifier, path, expected, source=False):
        if not isinstance(expected, str) or not re.fullmatch(r"[0-9a-f]{64}", expected):
            raise ValueError("Expected a lowercase SHA-256 digest")
        actual = resolve(root, path)
        if not actual.is_file():
            issue(identifier, "SOURCE_MISSING" if source else "ARTIFACT_MISSING")
        elif sha256(actual) != expected:
            issue(identifier, "SOURCE_CHANGED_RECHECK" if source else "ARTIFACT_CHANGED", not source)
        elif not source and actual.stat().st_size == 0:
            issue(identifier, "EMPTY_ARTIFACT")

    for identifier in ids:
        before = len(report["issues"])
        entry = entries.get(identifier)
        if entry is None:
            issue(identifier, "REQUIREMENT_MISSING")
            continue
        status = entry.get("status")
        if status not in {"pass", "partial", "fail", "not_run", "skip"}:
            raise ValueError("Unsupported requirement status")
        if status != "pass":
            issue(identifier, "REQUIREMENT_" + status.upper(), status == "fail")
        sources = entry.get("sources", {})
        if not isinstance(sources, dict):
            raise ValueError("sources must map relative paths to hashes")
        if not sources:
            issue(identifier, "NO_SOURCE_VERSION")
        for path, digest in sources.items():
            file_check(identifier, path, digest, source=True)
        checks = entry.get("checks", [])
        if not isinstance(checks, list):
            raise ValueError("checks must be a list")
        if not checks:
            issue(identifier, "NO_CHECKS")
        passed = 0
        for check in checks:
            if not isinstance(check, dict):
                raise ValueError("Each check must be an object")
            kind = check.get("kind")
            if kind not in {"automated", "manual"}:
                raise ValueError("check kind must be automated or manual")
            outcome = check.get("status")
            if outcome not in {"pass", "fail", "skip", "not_run"}:
                raise ValueError("Unsupported check status")
            if outcome != "pass":
                issue(identifier, "CHECK_" + outcome.upper(), outcome == "fail")
                continue
            if any(not isinstance(check.get(k), str) or not check[k].strip() for k in ["expected", "observed"]):
                issue(identifier, "NO_EXPECTED_OR_OBSERVED_RESULT")
            if not check.get("artifact") or not check.get("sha256"):
                issue(identifier, "NO_ARTIFACT_REFERENCE")
            else:
                file_check(identifier, check["artifact"], check["sha256"])
            if kind == "automated":
                counts = check.get("counts", {})
                if not isinstance(counts, dict) or any(type(counts.get(k)) is not int or counts[k] < 0 for k in ["executed", "passed", "failed", "skipped"]):
                    issue(identifier, "NO_VALID_EXECUTION_COUNTS")
                elif counts["executed"] != counts["passed"] + counts["failed"] or counts["executed"] == 0:
                    issue(identifier, "NO_EXECUTED_CHECKS_OR_BAD_COUNTS")
                elif counts["failed"] > 0:
                    issue(identifier, "RECORDED_TEST_FAILURE", True)
                if not isinstance(check.get("command"), str) or not check["command"].strip():
                    issue(identifier, "NO_EXECUTION_COMMAND")
                if type(check.get("exit_code")) is not int:
                    issue(identifier, "NO_EXIT_CODE")
                elif check["exit_code"] != 0:
                    issue(identifier, "NONZERO_EXIT", True)
            passed += 1
        if not passed:
            issue(identifier, "NO_PASSED_CHECK")
        if len(report["issues"]) == before:
            report["verified_requirements"] += 1
    report["status"] = "FAIL" if failed else "INCOMPLETE" if report["issues"] else "PASS"
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--requirements", required=True, type=Path)
    parser.add_argument("--evidence", required=True, type=Path)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    protected = {args.requirements.resolve(), args.evidence.resolve()}
    try:
        required = json.loads(args.requirements.read_text())
        evidence = json.loads(args.evidence.read_text())
        if isinstance(evidence, dict) and isinstance(evidence.get("requirements"), list):
            for entry in evidence["requirements"]:
                if not isinstance(entry, dict):
                    continue
                if isinstance(entry.get("sources"), dict):
                    protected.update((args.root / p).resolve() for p in entry["sources"])
                if isinstance(entry.get("checks"), list):
                    protected.update((args.root / c["artifact"]).resolve() for c in entry["checks"] if isinstance(c, dict) and isinstance(c.get("artifact"), str))
        report = inspect(required, evidence, args.root)
    except (OSError, ValueError, TypeError, KeyError) as error:
        report = {"status": "FAIL", "code": "INPUT_OR_EVIDENCE_ERROR", "error_type": type(error).__name__}
    if args.out and args.out.resolve() in protected:
        report = {"status": "FAIL", "code": "OUTPUT_OVERLAPS_INPUT"}
        args.out = None
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report["status"] == "PASS" else 2 if report["status"] == "INCOMPLETE" else 1


if __name__ == "__main__":
    sys.exit(main())
