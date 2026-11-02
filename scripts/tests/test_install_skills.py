"""Check installation, idempotence and refusal before any partial copy."""

from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SOURCE = Path(__file__).resolve().parents[2]
NAMES = ("fde-discovery", "fde-planning", "fde-core-review")


class InstallSkillsTest(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.kit = self.root / "kit"
        self.task = self.root / "task"
        (self.kit / "scripts").mkdir(parents=True)
        shutil.copy2(SOURCE / "scripts/install-skills.sh", self.kit / "scripts")
        shutil.copytree(SOURCE / "skills", self.kit / "skills")
        self.task.mkdir()
        self.destination = self.task / ".agents/skills"

    def install(self):
        return subprocess.run(
            ["sh", str(self.kit / "scripts/install-skills.sh"), str(self.task)],
            text=True, capture_output=True, check=False,
        )

    def test_install_all_resources_and_idempotent_rerun(self):
        sentinel = self.task / "README.md"
        sentinel.write_text("provided task instructions\n")
        first = self.install()
        self.assertEqual(first.returncode, 0, first.stderr)
        before = {}
        for name in NAMES:
            for source in (self.kit / "skills" / name).rglob("*"):
                if source.is_file():
                    target = self.destination / name / source.relative_to(self.kit / "skills" / name)
                    self.assertEqual(source.read_bytes(), target.read_bytes())
                    before[target] = target.stat().st_mtime_ns
        second = self.install()
        self.assertEqual(second.returncode, 0, second.stderr)
        self.assertEqual(before, {path: path.stat().st_mtime_ns for path in before})
        self.assertEqual(sentinel.read_text(), "provided task instructions\n")

    def test_conflicting_last_skill_prevents_partial_install(self):
        conflict = self.destination / NAMES[-1]
        conflict.mkdir(parents=True)
        custom = conflict / "SKILL.md"
        custom.write_text("existing custom skill\n")
        result = self.install()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(custom.read_text(), "existing custom skill\n")
        for name in NAMES[:-1]:
            self.assertFalse((self.destination / name).exists())

    def test_missing_last_source_prevents_partial_install(self):
        (self.kit / "skills" / NAMES[-1] / "SKILL.md").unlink()
        result = self.install()
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse((self.task / ".agents").exists())

    def test_symlink_parent_does_not_write_outside_task(self):
        outside = self.root / "outside"
        outside.mkdir()
        (self.task / ".agents").symlink_to(outside, target_is_directory=True)
        result = self.install()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(list(outside.iterdir()), [])


if __name__ == "__main__":
    unittest.main()
