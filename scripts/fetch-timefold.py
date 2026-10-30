#!/usr/bin/env python3
"""Fetch the reviewed official Spring example at a pinned commit. Never executes it."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import json
from pathlib import Path
import tempfile
from urllib.request import Request, urlopen


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('destination', help='A new directory; existing directories are not overwritten')
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    manifest = json.loads((root/'docs/timefold-source-manifest.json').read_text())
    destination = Path(args.destination).resolve()
    if destination.exists():
        raise SystemExit('Destination already exists; choose a new directory')
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='.reference-', dir=destination.parent) as temp:
        project = Path(temp)/'project'; project.mkdir()
        def fetch(path):
            relative = path[len(manifest['subtree']):] if path.startswith(manifest['subtree']) else path
            target = project/relative; target.parent.mkdir(parents=True, exist_ok=True)
            url = f'https://raw.githubusercontent.com/{manifest["repository"]}/{manifest["commit"]}/{path}'
            with urlopen(Request(url, headers={'User-Agent': 'pinned-reference-fetcher'}), timeout=30) as response:
                target.write_bytes(response.read())
        with ThreadPoolExecutor(max_workers=6) as pool:
            list(pool.map(fetch, manifest['files']))
        (project/'UPSTREAM.json').write_text(json.dumps(manifest, indent=2)+'\n')
        project.rename(destination)
    print(f'Fetched {len(manifest["files"])} source/license files to {destination}; no build or execution performed.')

if __name__ == '__main__': main()
