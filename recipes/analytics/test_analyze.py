import tempfile
import unittest
from pathlib import Path
from analyze import analyze

class AnalysisRecipeTest(unittest.TestCase):
    def test_aggregation_is_known_and_sorted(self):
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder); (p/'in.csv').write_text('kind,value\nB,8\nA,2\nA,4\n')
            result = analyze(p/'in.csv', 'kind', 'value', p/'out')
            self.assertEqual(result['rows'], 3)
            self.assertEqual(result['results'][0]['group'], 'A')
            self.assertEqual(result['results'][0]['sum'], '6.0000')
            self.assertEqual(result['results'][0]['mean'], 3)
            self.assertTrue((p/'out/summary.csv').is_file())
    def test_bad_numeric_values_fail_before_output(self):
        with tempfile.TemporaryDirectory() as folder:
            p = Path(folder); (p/'in.csv').write_text('kind,value\nA,bad\n')
            with self.assertRaises(ValueError): analyze(p/'in.csv','kind','value',p/'out')
            self.assertFalse((p/'out').exists())
    def test_column_names_are_quoted_and_csv_formula_is_neutralized(self):
        with tempfile.TemporaryDirectory() as folder:
            p=Path(folder); (p/'in.csv').write_text('group name,measure\n=1+1,2\n')
            analyze(p/'in.csv','group name','measure',p/'out')
            self.assertIn("'=1+1",(p/'out/summary.csv').read_text())
    def test_missing_columns_are_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            p=Path(folder); (p/'in.csv').write_text('kind,value\nA,2\n')
            with self.assertRaises(ValueError): analyze(p/'in.csv','missing','value',p/'out')

if __name__ == '__main__': unittest.main()
