#!/usr/bin/env python3
"""Profile CSVs or check an explicit multi-file contract. Never infer business rules."""
from __future__ import annotations

import argparse
import csv
from datetime import datetime
from decimal import Decimal, InvalidOperation
import json
from pathlib import Path
import sys


def inside(root: Path, value: str) -> Path:
    path = Path(value)
    if path.is_absolute() or ".." in path.parts:
        raise ValueError("Expected a project-relative path")
    result = (root / path).resolve()
    if not result.is_relative_to(root.resolve()):
        raise ValueError("Path escapes data root")
    return result


def scan(path: Path, encoding: str, limit: int) -> tuple[list[str], list[dict], bool]:
    with path.open(encoding=encoding, newline="") as stream:
        reader = csv.DictReader(stream, strict=True)
        headers = reader.fieldnames
        if not headers or any(not h.strip() for h in headers) or len(headers) != len(set(headers)):
            raise ValueError("Missing, blank or duplicate CSV headers")
        rows = []
        for row in reader:
            if len(rows) == limit:
                return headers, rows, True
            if None in row or any(value is None for value in row.values()):
                raise ValueError("CSV row width differs from headers")
            rows.append(row)
        return headers, rows, False


def typed(value: str, spec: dict) -> None:
    if not value.strip():
        if spec.get("nullable", False):
            return
        raise ValueError("NULL")
    kind = spec.get("type", "string")
    if kind in {"integer", "number"}:
        number = Decimal(value)
        if not number.is_finite() or (kind == "integer" and number != number.to_integral_value()):
            raise ValueError("TYPE")
        if "minimum" in spec and number < Decimal(str(spec["minimum"])):
            raise ValueError("RANGE")
        if "maximum" in spec and number > Decimal(str(spec["maximum"])):
            raise ValueError("RANGE")
    elif kind == "datetime":
        if "T" not in value and " " not in value:
            raise ValueError("TYPE")
        timestamp = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if spec.get("timezone") == "required" and timestamp.utcoffset() is None:
            raise ValueError("TIMEZONE")


def check(contract: dict, root: Path, limit: int) -> dict:
    if not isinstance(contract, dict) or type(contract.get("schema_version")) is not int or contract["schema_version"] != 1 or not isinstance(contract.get("files"), dict) or not contract["files"]:
        raise ValueError("Contract needs schema_version=1 and nonempty files")
    if set(contract) - {"schema_version", "files", "foreign_keys"} or not isinstance(contract.get("foreign_keys", []), list):
        raise ValueError("Unsupported contract fields")
    report = {"status": "PASS", "scope": "declared-data-contract", "checks": 0,
              "error_count": 0, "issues": [], "files": {}}
    tables = {}

    def issue(code, alias, row=None, columns=None):
        report["error_count"] += 1
        if len(report["issues"]) < 100:
            report["issues"].append({"code": code, "file": alias, "record": row, "columns": columns})

    incomplete = False
    for alias, spec in contract["files"].items():
        if not isinstance(spec, dict) or set(spec) - {"path", "encoding", "columns", "primary_key", "allow_empty"}:
            raise ValueError("Unsupported file fields")
        columns = spec.get("columns")
        if not isinstance(columns, dict) or not columns:
            raise ValueError("Each file needs nonempty columns")
        for column in columns.values():
            if not isinstance(column, dict) or column.get("type", "string") not in {"string", "integer", "number", "datetime"}:
                raise ValueError("Unsupported column type")
            if set(column) - {"type", "nullable", "minimum", "maximum", "timezone", "unit"}:
                raise ValueError("Unsupported column fields")
            if type(column.get("nullable", False)) is not bool or column.get("timezone") not in {None, "required"}:
                raise ValueError("Invalid nullable/timezone setting")
            if column.get("timezone") and column.get("type") != "datetime":
                raise ValueError("timezone applies to datetime only")
            for bound in ("minimum", "maximum"):
                if bound in column and (column.get("type") not in {"integer", "number"} or not Decimal(str(column[bound])).is_finite()):
                    raise ValueError("Bounds require finite numeric column values")
            if "minimum" in column and "maximum" in column and Decimal(str(column["minimum"])) > Decimal(str(column["maximum"])):
                raise ValueError("minimum exceeds maximum")
        primary = spec.get("primary_key", [])
        if not isinstance(primary, list) or len(primary) != len(set(primary)) or any(c not in columns for c in primary):
            raise ValueError("primary_key must reference declared columns")
        if type(spec.get("allow_empty", False)) is not bool:
            raise ValueError("allow_empty must be boolean")
        path = inside(root, spec["path"])
        report["checks"] += 1
        try:
            headers, rows, truncated = scan(path, spec.get("encoding", "utf-8-sig"), limit)
        except (OSError, UnicodeError, csv.Error, ValueError, LookupError):
            issue("READ_OR_CSV_ERROR", alias)
            continue
        incomplete |= truncated
        report["files"][alias] = {"headers": headers, "records_scanned": len(rows), "truncated": truncated,
                                  "units_declared": {c: v["unit"] for c, v in columns.items() if "unit" in v}}
        missing = set(columns) - set(headers)
        if missing:
            issue("MISSING_COLUMNS", alias, columns=sorted(missing))
            continue
        if not rows and not spec.get("allow_empty", False):
            issue("EMPTY_DATA", alias)
        keys = set()
        for index, row in enumerate(rows, 1):
            for name, column in columns.items():
                report["checks"] += 1
                try:
                    typed(row[name], column)
                except (ValueError, InvalidOperation, OverflowError):
                    issue("INVALID_CELL", alias, index, [name])
            if primary:
                report["checks"] += 1
                key = tuple(row[c] for c in primary)
                if any(not value.strip() for value in key):
                    issue("EMPTY_PRIMARY_KEY", alias, index, primary)
                elif key in keys:
                    issue("DUPLICATE_PRIMARY_KEY", alias, index, primary)
                keys.add(key)
        tables[alias] = rows
    for relation in contract.get("foreign_keys", []):
        if not isinstance(relation, dict) or set(relation) != {"file", "columns", "references"} or not isinstance(relation["references"], dict) or set(relation["references"]) != {"file", "columns"}:
            raise ValueError("Invalid foreign key shape")
        source, target = relation["file"], relation["references"]["file"]
        left, right = relation["columns"], relation["references"]["columns"]
        if source not in contract["files"] or target not in contract["files"] or not isinstance(left, list) or not isinstance(right, list) or not left or len(left) != len(right):
            raise ValueError("Invalid foreign key files/columns")
        if any(c not in contract["files"][source]["columns"] for c in left) or any(c not in contract["files"][target]["columns"] for c in right):
            raise ValueError("Foreign keys must reference declared columns")
        if source not in tables or target not in tables:
            incomplete = True
            continue
        report["checks"] += 1
        target_keys = [tuple(row[c] for c in right) for row in tables[target]]
        if len(target_keys) != len(set(target_keys)):
            issue("AMBIGUOUS_REFERENCE", target, columns=right)
        lookup = set(target_keys)
        for index, row in enumerate(tables[source], 1):
            key = tuple(row[c] for c in left)
            if all(not v.strip() for v in key) and all(contract["files"][source]["columns"][c].get("nullable", False) for c in left):
                continue
            report["checks"] += 1
            if key not in lookup:
                issue("MISSING_REFERENCE", source, index, left)
    report["status"] = "FAIL" if report["error_count"] else "INCOMPLETE" if incomplete else "PASS"
    return report


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--contract", type=Path)
    mode.add_argument("--profile", type=Path)
    parser.add_argument("--root", type=Path)
    parser.add_argument("--max-rows", type=int, default=100000)
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    protected = {p.resolve() for p in [args.contract, args.profile] if p is not None}
    try:
        if args.max_rows < 1:
            raise ValueError("max-rows must be positive")
        if args.profile:
            headers, rows, truncated = scan(args.profile, "utf-8-sig", args.max_rows)
            report = {"status": "PROFILE_ONLY", "headers": headers, "records_scanned": len(rows),
                      "truncated": truncated, "empty_cells": {h: sum(not row[h].strip() for row in rows) for h in headers},
                      "checks": 0, "note": "No business contract checked; values are not printed."}
        else:
            contract = json.loads(args.contract.read_text())
            data_root = args.root or args.contract.parent
            if isinstance(contract, dict) and isinstance(contract.get("files"), dict):
                for spec in contract["files"].values():
                    if isinstance(spec, dict) and isinstance(spec.get("path"), str):
                        protected.add((data_root / spec["path"]).resolve())
            report = check(contract, data_root, args.max_rows)
    except (OSError, ValueError, TypeError, KeyError, InvalidOperation, UnicodeError, csv.Error) as error:
        report = {"status": "FAIL", "code": "INPUT_OR_CONTRACT_ERROR", "error_type": type(error).__name__}
    if args.out and args.out.resolve() in protected:
        report = {"status": "FAIL", "code": "OUTPUT_OVERLAPS_INPUT"}
        args.out = None
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0 if report["status"] == "PASS" else 2 if report["status"] in {"PROFILE_ONLY", "INCOMPLETE"} else 1


if __name__ == "__main__":
    sys.exit(main())
