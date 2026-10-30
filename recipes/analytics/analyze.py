#!/usr/bin/env python3
"""Local CSV aggregation recipe. No network connector or AI provider is configured."""
import argparse
import csv
import json
from pathlib import Path
import duckdb


def identifier(name):
    return '"' + name.replace('"', '""') + '"'


def analyze(source, group, measure, output):
    source, output = Path(source), Path(output)
    if not source.is_file():
        raise ValueError('A local CSV file is required')
    con = duckdb.connect(config={'autoinstall_known_extensions': False, 'autoload_known_extensions': False})
    try:
        data = con.read_csv(str(source.resolve()), header=True, all_varchar=True)
        if group not in data.columns or measure not in data.columns:
            raise ValueError('Group or measure column is missing')
        data.create_view('input_data')
        g, m = identifier(group), identifier(measure)
        number = f'TRY_CAST({m} AS DECIMAL(18,4))'
        invalid = con.execute(f'SELECT COUNT(*) FROM input_data WHERE {g} IS NULL OR trim({g}) = \'\' OR {number} IS NULL').fetchone()[0]
        if invalid:
            raise ValueError(f'{invalid} rows have missing groups or invalid numeric values')
        rows = con.execute(f'SELECT {g}, COUNT(*), SUM({number}), AVG({number}), MIN({number}), MAX({number}) FROM input_data GROUP BY {g} ORDER BY {g}').fetchall()
        summary = {'input': source.name, 'group': group, 'measure': measure, 'rows': sum(row[1] for row in rows), 'groups': len(rows),
                   'results': [{'group': r[0], 'count': r[1], 'sum': str(r[2]), 'mean': float(r[3]), 'min': str(r[4]), 'max': str(r[5])} for r in rows]}
        output.mkdir(parents=True, exist_ok=True)
        (output/'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2)+'\n')
        with (output/'summary.csv').open('w', newline='') as stream:
            writer = csv.writer(stream)
            writer.writerow(['group', 'count', 'sum', 'mean', 'min', 'max'])
            for row in summary['results']:
                label = row['group']
                if label and label[0] in '=+-@\t\r\n':
                    label = "'"+label
                writer.writerow([label, row['count'], row['sum'], row['mean'], row['min'], row['max']])
        return summary
    finally:
        con.close()


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('source')
    parser.add_argument('--group', required=True)
    parser.add_argument('--measure', required=True)
    parser.add_argument('--out', required=True)
    args = parser.parse_args()
    result = analyze(args.source, args.group, args.measure, args.out)
    print(json.dumps({'rows': result['rows'], 'groups': result['groups'], 'output': args.out}, ensure_ascii=False))
