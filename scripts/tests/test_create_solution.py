"""Generator isolation, module selection and preservation checks; no network required."""
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "scripts/create-solution.py"


class CreateSolutionTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="solution tests ")
        self.folder = Path(self.temporary.name)
        self.target = self.folder / "과제 solution"

    def tearDown(self):
        self.temporary.cleanup()

    def run_generator(self, *args, success=True):
        result = subprocess.run([sys.executable, str(SCRIPT), str(self.target), *args], capture_output=True, text=True)
        self.assertEqual(result.returncode == 0, success, result.stdout + result.stderr)
        return result

    def snapshot(self):
        return {str(p.relative_to(self.target)): hashlib.sha256(p.read_bytes()).hexdigest()
                for p in self.target.rglob('*') if p.is_file()}

    def test_default_has_no_business_auth_or_database(self):
        self.run_generator()
        self.assertEqual(json.loads((self.target / 'solution-modules.json').read_text())['modules'], [])
        java = list(self.target.joinpath('src/main/java').rglob('*.java'))
        self.assertEqual({p.stem for p in java}, {'SolutionApplication', 'HealthResource'})
        self.assertFalse((self.target / 'src/main/resources/fde').exists())
        self.assertNotIn('jhipster', (self.target / 'pom.xml').read_text())
        self.assertEqual(set(json.loads((self.target / 'package.json').read_text())['dependencies']), {'react', 'react-dom'})

    def test_workflow_selects_auth_and_only_one_domain_without_csv(self):
        self.run_generator('--modules', 'workflow', '--domain', 'support')
        state = json.loads((self.target / 'solution-modules.json').read_text())
        self.assertEqual(state['modules'], ['auth', 'workflow'])
        self.assertEqual([p.name for p in self.target.joinpath('src/main/resources/fde/domains').glob('*.json')], ['support.json'])
        self.assertNotIn('commons-csv', (self.target / 'pom.xml').read_text())
        self.assertFalse(list(self.target.rglob('FdeCsvResource.java')))
        self.assertFalse(list(self.target.rglob('EnterpriseResource.java')))

    def test_csv_and_enterprise_are_selected_explicitly(self):
        self.run_generator('--modules', 'csv,enterprise', '--domain', 'enterprise')
        self.assertTrue(list(self.target.rglob('FdeCsvResource.java')))
        self.assertTrue(list(self.target.rglob('EnterpriseResource.java')))
        self.assertFalse(list(self.target.rglob('EnterpriseDemoData.java')))
        self.assertNotIn('bank-account', '\n'.join(self.snapshot()))
        self.assertIn('commons-csv', (self.target / 'pom.xml').read_text())

    def test_add_preserves_custom_page_file_and_domain_edits(self):
        self.run_generator('--modules', 'workflow', '--domain', 'support')
        main = self.target / 'src/main/webapp/main.tsx'
        main.write_text(main.read_text() + '\n// task-specific page change\n')
        business = self.target / 'business.txt'; business.write_text('keep my requirement')
        domain = self.target / 'src/main/resources/fde/domains/support.json'
        value = json.loads(domain.read_text()); value['label'] = '실제 과제 업무'; domain.write_text(json.dumps(value, ensure_ascii=False))
        preserved = {p: p.read_bytes() for p in [main, business, domain]}
        self.run_generator('--add', '--modules', 'csv')
        self.assertTrue(list(self.target.rglob('FdeCsvResource.java')))
        for path, content in preserved.items(): self.assertEqual(path.read_bytes(), content)

    def test_conflicting_pom_aborts_all_writes(self):
        self.run_generator()
        pom = self.target / 'pom.xml'; pom.write_text(pom.read_text() + '\n<!-- custom dependency -->\n')
        before = self.snapshot()
        self.run_generator('--add', '--modules', 'auth', success=False)
        self.assertEqual(self.snapshot(), before)

    def test_unmanaged_module_destination_is_preserved(self):
        self.run_generator()
        path = self.target / 'src/main/java/com/example/solution/security/AccountResource.java'
        path.parent.mkdir(parents=True); path.write_text('custom account implementation')
        before = self.snapshot()
        self.run_generator('--add', '--modules', 'auth', success=False)
        self.assertEqual(self.snapshot(), before)

    def test_existing_target_is_never_replaced(self):
        self.target.mkdir(); (self.target / 'important.txt').write_text('keep')
        before = self.snapshot(); self.run_generator(success=False)
        self.assertEqual(self.snapshot(), before)

    def test_dry_run_and_invalid_selection_do_not_create_target(self):
        self.run_generator('--dry-run'); self.assertFalse(self.target.exists())
        self.run_generator('--modules', 'not-a-module', success=False); self.assertFalse(self.target.exists())
        self.run_generator('--modules', 'workflow', success=False); self.assertFalse(self.target.exists())

    def test_add_is_idempotent(self):
        self.run_generator('--modules', 'auth'); before = self.snapshot()
        self.run_generator('--add', '--modules', 'auth')
        self.assertEqual(self.snapshot(), before)

    def test_symlinked_domain_is_rejected(self):
        self.run_generator('--modules', 'workflow', '--domain', 'support')
        path = self.target / 'src/main/resources/fde/domains/support.json'
        outside = self.folder / 'outside.json'; path.replace(outside); path.symlink_to(outside)
        before = outside.read_bytes()
        self.run_generator('--add', '--modules', 'csv', success=False)
        self.assertEqual(outside.read_bytes(), before)
        self.assertFalse(list(self.target.rglob('FdeCsvResource.java')))

    def test_installed_artifacts_and_local_secrets_are_not_copied(self):
        spec = importlib.util.spec_from_file_location('solution_generator', SCRIPT)
        module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
        template = self.folder / 'template'; template.mkdir()
        (template / 'source.txt').write_text('source')
        for folder in ['target', 'node_modules', '.local-data', '.git']:
            path = template / folder; path.mkdir(); (path / 'private.txt').write_text('must not copy')
        (template / '.env').write_text('private configuration')
        self.assertEqual(module.read_files(template), {'source.txt': b'source'})


if __name__ == '__main__':
    unittest.main()
