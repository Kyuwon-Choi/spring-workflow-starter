import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


def load(filename):
    spec = importlib.util.spec_from_file_location(filename, ROOT / "scripts" / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


DATA = load("check-data.py")
EVIDENCE = load("verify-evidence.py")


class DataCheckTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        (self.root / "teams.csv").write_text("id,capacity\nT1,2\n")
        (self.root / "jobs.csv").write_text("id,team,at\nJ1,T1,2026-11-02T10:00:00+09:00\n")
        self.contract = {"schema_version": 1, "files": {
            "teams": {"path": "teams.csv", "primary_key": ["id"], "columns": {"id": {"type": "string"}, "capacity": {"type": "integer", "minimum": 0}}},
            "jobs": {"path": "jobs.csv", "primary_key": ["id"], "columns": {"id": {}, "team": {}, "at": {"type": "datetime", "timezone": "required"}}}},
            "foreign_keys": [{"file": "jobs", "columns": ["team"], "references": {"file": "teams", "columns": ["id"]}}]}

    def test_valid_multiple_files(self):
        self.assertEqual(DATA.check(self.contract, self.root, 100)["status"], "PASS")

    def test_duplicate_id_and_missing_reference_without_value_disclosure(self):
        (self.root / "jobs.csv").write_text("id,team,at\nPRIVATE_NAME,UNKNOWN_SECRET,2026-11-02T10:00:00Z\nPRIVATE_NAME,T1,2026-11-02T10:00:00Z\n")
        report = DATA.check(self.contract, self.root, 100)
        codes = {i["code"] for i in report["issues"]}
        self.assertIn("DUPLICATE_PRIMARY_KEY", codes)
        self.assertIn("MISSING_REFERENCE", codes)
        self.assertNotIn("PRIVATE_NAME", json.dumps(report))
        self.assertNotIn("UNKNOWN_SECRET", json.dumps(report))

    def test_numeric_and_timezone_boundaries(self):
        for capacity in ["-1", "1.5", "NaN", "Infinity"]:
            (self.root / "teams.csv").write_text(f"id,capacity\nT1,{capacity}\n")
            self.assertEqual(DATA.check(self.contract, self.root, 100)["status"], "FAIL")
        (self.root / "teams.csv").write_text("id,capacity\nT1,0\n")
        (self.root / "jobs.csv").write_text("id,team,at\nJ1,T1,2026-11-02T10:00:00\n")
        self.assertEqual(DATA.check(self.contract, self.root, 100)["status"], "FAIL")

    def test_bom_and_quoted_delimiter(self):
        (self.root / "teams.csv").write_text('\ufeffid,capacity\n"T,1",2\n')
        (self.root / "jobs.csv").write_text('id,team,at\nJ1,"T,1",2026-11-02T10:00:00Z\n')
        self.assertEqual(DATA.check(self.contract, self.root, 100)["status"], "PASS")

    def test_empty_or_bad_headers_fail(self):
        for text in ["id,capacity\n", "id,id\n1,2\n", "id,capacity\n1,2,3\n"]:
            (self.root / "teams.csv").write_text(text)
            self.assertEqual(DATA.check(self.contract, self.root, 100)["status"], "FAIL")

    def test_truncation_not_passed_and_profile_has_exit_two(self):
        (self.root / "teams.csv").write_text("id,capacity\nT1,2\nT2,2\n")
        self.assertEqual(DATA.check(self.contract, self.root, 1)["status"], "INCOMPLETE")
        result = subprocess.run([sys.executable, str(ROOT / "scripts/check-data.py"), "--profile", str(self.root / "teams.csv")],capture_output=True,text=True)
        self.assertEqual(result.returncode, 2)
        self.assertEqual(json.loads(result.stdout)["checks"], 0)

    def test_path_escape_and_misspelled_rules_rejected(self):
        self.contract["files"]["teams"]["columns"]["capacity"]["mininum"] = 0
        with self.assertRaises(ValueError): DATA.check(self.contract, self.root, 100)
        del self.contract["files"]["teams"]["columns"]["capacity"]["mininum"]
        self.contract["files"]["teams"]["path"] = "../outside.csv"
        with self.assertRaises(ValueError): DATA.check(self.contract, self.root, 100)

    def test_report_never_overwrites_declared_input(self):
        contract = self.root / 'contract.json'
        contract.write_text(json.dumps(self.contract))
        source = self.root / 'teams.csv'
        before = source.read_bytes()
        result = subprocess.run([sys.executable, str(ROOT/'scripts/check-data.py'), '--contract', str(contract), '--out', str(source)], capture_output=True, text=True)
        self.assertEqual(result.returncode, 1)
        self.assertEqual(source.read_bytes(), before)


class EvidenceCheckTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        (self.root / "core.py").write_text("actual version\n")
        (self.root / "run.log").write_text("actual check output\n")
        self.required = {"required": ["R1"]}
        self.record = {"schema_version": 1, "requirements": [{"id": "R1", "status": "pass",
            "sources": {"core.py": EVIDENCE.sha256(self.root / "core.py")},
            "checks": [{"kind": "automated", "status": "pass", "expected": "3", "observed": "3",
                        "artifact": "run.log", "sha256": EVIDENCE.sha256(self.root / "run.log"),
                        "command": "python3 test_core.py", "exit_code": 0,
                        "counts": {"executed": 1, "passed": 1, "failed": 0, "skipped": 0}}]}]}

    def check(self):
        return EVIDENCE.inspect(self.required, self.record, self.root)

    def test_complete_record(self):
        self.assertEqual(self.check()["status"], "PASS")

    def test_no_requirements_is_not_pass(self):
        self.required["required"] = []
        with self.assertRaises(ValueError): self.check()

    def test_missing_requirement_and_zero_checks_incomplete(self):
        self.record["requirements"][0]["checks"] = []
        self.assertEqual(self.check()["status"], "INCOMPLETE")
        self.record["requirements"] = []
        self.assertEqual(self.check()["status"], "INCOMPLETE")

    def test_all_skipped_or_zero_execution_incomplete(self):
        check = self.record["requirements"][0]["checks"][0]
        check["counts"] = {"executed": 0, "passed": 0, "failed": 0, "skipped": 4}
        self.assertEqual(self.check()["status"], "INCOMPLETE")
        check["status"] = "skip"
        self.assertEqual(self.check()["status"], "INCOMPLETE")

    def test_changed_source_requires_related_recheck(self):
        (self.root / "core.py").write_text("new behavior\n")
        self.assertEqual(self.check()["status"], "INCOMPLETE")
        self.assertIn("SOURCE_CHANGED_RECHECK", [i["code"] for i in self.check()["issues"]])

    def test_missing_or_tampered_artifact(self):
        (self.root / "run.log").unlink()
        self.assertEqual(self.check()["status"], "INCOMPLETE")
        (self.root / "run.log").write_text("changed output\n")
        self.assertEqual(self.check()["status"], "FAIL")

    def test_real_failure_overrides_pass_label(self):
        self.record["requirements"][0]["checks"][0]["exit_code"] = 1
        self.assertEqual(self.check()["status"], "FAIL")

    def test_manual_observation_without_fabricated_test_counts(self):
        check = self.record["requirements"][0]["checks"][0]
        check["kind"] = "manual"
        for key in ["counts", "command", "exit_code"]: del check[key]
        self.assertEqual(self.check()["status"], "PASS")

    def test_path_escape_and_unknown_ids_rejected(self):
        self.record["requirements"][0]["checks"][0]["artifact"] = "../outside.log"
        with self.assertRaises(ValueError): self.check()
        self.record["requirements"][0]["id"] = "R2"
        with self.assertRaises(ValueError): self.check()

    def test_report_never_overwrites_source_or_artifact(self):
        required = self.root/'required.json'; required.write_text(json.dumps(self.required))
        record = self.root/'record.json'; record.write_text(json.dumps(self.record))
        for name in ['core.py', 'run.log', 'required.json', 'record.json']:
            target = self.root/name
            before = target.read_bytes()
            result = subprocess.run([sys.executable, str(ROOT/'scripts/verify-evidence.py'), '--requirements', str(required), '--evidence', str(record), '--root', str(self.root), '--out', str(target)], capture_output=True, text=True)
            self.assertEqual(result.returncode, 1)
            self.assertEqual(target.read_bytes(), before)


if __name__ == "__main__":
    unittest.main()
