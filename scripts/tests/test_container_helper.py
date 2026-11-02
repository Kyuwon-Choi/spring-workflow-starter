"""Command wiring with a fake CLI; these are not Docker engine integration tests."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


class ContainerHelperTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.folder = Path(temporary.name)
        self.app = self.folder / "task app"
        shutil.copytree(ROOT / "starter/docker", self.app)
        self.bin = self.folder / "bin"
        self.bin.mkdir()
        self.log = self.folder / "docker.jsonl"
        fake = self.bin / "docker"
        fake.write_text("#!/usr/bin/env python3\nimport json,os,sys\nfrom pathlib import Path\nwith Path(os.environ['FDE_DOCKER_TEST_LOG']).open('a') as f: f.write(json.dumps({'args':sys.argv[1:],'cwd':os.getcwd()})+'\\n')\nif sys.argv[1:] == ['info'] and os.environ.get('FDE_DOCKER_TEST_FAIL_INFO'): sys.exit(1)\n")
        fake.chmod(0o755)
        self.env = dict(os.environ, PATH=str(self.bin) + os.pathsep + os.environ["PATH"], FDE_DOCKER_TEST_LOG=str(self.log))

    def run_helper(self, *args):
        return subprocess.run(["bash", str(self.app / "scripts/container.sh"), *args], env=self.env, capture_output=True, text=True)

    def calls(self):
        return [json.loads(line) for line in self.log.read_text().splitlines()] if self.log.exists() else []

    def test_build_uses_generated_app_directory_and_explicit_tag(self):
        self.env["SOLUTION_IMAGE"] = "my-task:test"
        result = self.run_helper("build")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.calls()[-1], {"args": ["build", "--tag", "my-task:test", "."], "cwd": str(self.app.resolve())})

    def test_run_uses_data_volume_fixed_container_port_and_no_password_values(self):
        self.env.update(SOLUTION_DATA_VOLUME="my-task-data", APP_ADMIN_PASSWORD="private-password")
        result = self.run_helper("run", "8123")
        self.assertEqual(result.returncode, 0, result.stderr)
        args = self.calls()[-1]["args"]
        self.assertIn("8123:8080", args)
        self.assertIn("type=volume,source=my-task-data,target=/app/.local-data", args)
        self.assertIn("--server.address=0.0.0.0", args)
        self.assertNotIn("private-password", self.log.read_text() + result.stdout + result.stderr)

    def test_bad_port_is_rejected_before_docker_and_daemon_error_stops_build(self):
        result = self.run_helper("run", "70000")
        self.assertEqual(result.returncode, 2)
        self.assertEqual(self.calls(), [])
        self.env["FDE_DOCKER_TEST_FAIL_INFO"] = "1"
        result = self.run_helper("build")
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual([call["args"] for call in self.calls()], [["info"]])


if __name__ == "__main__":
    unittest.main()
