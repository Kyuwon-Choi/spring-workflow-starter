#!/usr/bin/env python3
"""Create a small task project or safely add selected modules. No network or shell execution."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
CATALOG = json.loads((ROOT / "modules/catalog.json").read_text())["modules"]
STATE = "solution-modules.json"


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def relative(value: str) -> Path:
    path = Path(value)
    if path.is_absolute() or ".." in path.parts or not path.parts or str(path) == ".":
        raise ValueError("Unsafe project-relative path")
    return path


def read_files(folder: Path) -> dict[str, bytes]:
    files = {}
    for path in sorted(folder.rglob("*")):
        rel = path.relative_to(folder)
        if any(part in {".git", "node_modules", "target", ".local-data", "__pycache__"} for part in rel.parts):
            continue
        if path.name == ".DS_Store" or path.name == ".env" or path.name.startswith(".env.") or path.suffix == ".pyc":
            continue
        if path.is_symlink():
            raise ValueError(f"Template symlinks are not supported: {path}")
        if path.is_file():
            files[rel.as_posix()] = path.read_bytes()
    return files


def expand(requested: list[str]) -> list[str]:
    selected = []
    visiting = set()

    def visit(name: str) -> None:
        if name in selected:
            return
        if name not in CATALOG or name in visiting:
            raise ValueError(f"Unknown or cyclic module: {name}")
        visiting.add(name)
        for dependency in CATALOG[name]["requires"]:
            visit(dependency)
        visiting.remove(name)
        selected.append(name)

    for name in requested:
        visit(name)
    return selected


def source_commit() -> str:
    result = subprocess.run(["git", "-C", str(ROOT), "rev-parse", "HEAD"], capture_output=True, text=True)
    return result.stdout.strip() if result.returncode == 0 else "archive-without-git"


def is_linked(target: Path, rel: Path) -> bool:
    return any(target.joinpath(*rel.parts[:index + 1]).is_symlink() for index in range(len(rel.parts)))


def render(selected: list[str], domain: dict | None) -> dict[str, bytes]:
    files = read_files(ROOT / "starter/base")
    for name in selected:
        for path, data in read_files(ROOT / "modules" / name / "files").items():
            if path in files:
                raise ValueError(f"Module file collision: {path}")
            files[path] = data
    package = json.loads(files["package.json"])
    coordinates = []
    properties = files["src/main/resources/application.properties"].decode()
    for name in selected:
        package["dependencies"].update(CATALOG[name]["npm"])
        coordinates.extend(CATALOG[name]["maven"])
        properties += "\n# Module: " + name + "\n" + "\n".join(CATALOG[name]["properties"]) + "\n"
    files["package.json"] = (json.dumps(package, ensure_ascii=False, indent=2) + "\n").encode()
    profile = "workflow" if "workflow" in selected else "auth" if "auth" in selected else "base"
    lock = (ROOT / "starter/locks" / (profile + ".json")).read_bytes()
    locked = json.loads(lock)["packages"][""]
    if locked.get("dependencies", {}) != package["dependencies"] or locked.get("devDependencies", {}) != package["devDependencies"]:
        raise ValueError("Dependency lock does not match module selection; regenerate starter/locks first")
    files["package-lock.json"] = lock
    fragments = []
    for coordinate in dict.fromkeys(coordinates):
        parts = coordinate.split(":")
        version = f"<version>{parts[2]}</version>" if len(parts) == 3 else ""
        fragments.append(f"    <dependency><groupId>{parts[0]}</groupId><artifactId>{parts[1]}</artifactId>{version}</dependency>")
    files["pom.xml"] = files["pom.xml"].replace(b"    <!-- MODULE_DEPENDENCIES -->", "\n".join(fragments).encode())
    files["src/main/resources/application.properties"] = properties.encode()
    imports = ["import React from 'react';"]
    features = []
    routes = []
    if "auth" in selected:
        imports.append("import { AuthGate } from './features/auth/Auth';")
    if "workflow" in selected:
        imports.extend(["import Workbench from './features/workflow/Workbench';", "import 'bootstrap/dist/css/bootstrap.min.css';"])
        if "csv" in selected:
            imports.append("import CsvTools from './features/csv/CsvTools';")
        component = "() => <Workbench tools={CsvTools}/>" if "csv" in selected else "Workbench"
        features.append("  { path: '/fde', label: '업무 처리', component: " + component + " },")
        routes.append("/fde")
        if not domain:
            raise ValueError("workflow requires --domain-file or an explicit --domain example")
        identifier = domain.get("id", "")
        if not re.fullmatch(r"[a-z][a-z0-9-]{0,63}", identifier) or not domain.get("types"):
            raise ValueError("Domain must have a valid id and nonempty types")
        files[f"src/main/resources/fde/domains/{identifier}.json"] = (json.dumps(domain, ensure_ascii=False, indent=2) + "\n").encode()
        changelogs = ["workflow"] + (["enterprise"] if "enterprise" in selected else [])
        master = '<?xml version="1.0" encoding="UTF-8"?>\n<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">\n'
        master += "".join(f'  <include file="db/changelog/{name}.xml"/>\n' for name in changelogs) + "</databaseChangeLog>\n"
        files["src/main/resources/db/changelog/master.xml"] = master.encode()
    elif domain:
        raise ValueError("A domain requires the workflow module")
    if "enterprise" in selected:
        imports.append("import Organization from './features/enterprise/Organization';")
        features.append("  { path: '/organization', label: '조직 설정', component: Organization },")
        routes.append("/organization")
    shell = "AuthGate" if "auth" in selected else "React.Fragment"
    files["src/main/webapp/selected.tsx"] = ("\n".join(imports) + "\n\nexport const Shell = " + shell + ";\nexport const features: { path: string; label: string; component: React.ComponentType }[] = [\n" + "\n".join(features) + "\n];\n").encode()
    if routes:
        mapping = ", ".join(json.dumps(path) for path in routes)
        files["src/main/java/com/example/solution/WebRoutes.java"] = ("package com.example.solution;\n\nimport org.springframework.stereotype.Controller;\nimport org.springframework.web.bind.annotation.GetMapping;\n\n@Controller\npublic class WebRoutes {\n    @GetMapping({" + mapping + "})\n    public String index() { return \"forward:/index.html\"; }\n}\n").encode()
    files["LICENSE.txt"] = (ROOT / "LICENSE.txt").read_bytes()
    files["NOTICE.txt"] = ("Selected components from FDE Workbench, Apache-2.0.\nSource: https://github.com/Kyuwon-Choi/spring-workflow-starter\nThe reference repository retains JHipster upstream attribution.\nSee solution-modules.json for selected modules and managed file hashes.\n").encode()
    modules = ", ".join(selected) or "없음 (Spring + React 기본 골격)"
    auth_note = "\n로그인: 기본 로컬 확인 계정 admin/admin, user/user. APP_ADMIN_PASSWORD/APP_USER_PASSWORD 또는 app.auth.users 설정으로 변경한다. JWT Bearer·메모리 설정 계정이며 SSO/회원가입 기능은 없다. JWT 서명 키는 .local-data/jwt-secret에 생성해 유지하고 Git에 넣지 않는다. 브라우저 새로고침 후 재로그인한다.\n" if "auth" in selected else ""
    files["README.md"] = ("# 과제 결과물\n\n선택 모듈: " + modules + "\n\n```bash\n./scripts/build.sh\n./scripts/preview.sh 8080\n```\n\nJava 21과 Bash, 최초 의존성 다운로드 네트워크가 필요하다. Maven·Node/npm은 wrapper로 준비한다. 실제 서버 포트와 " + ("/fde" if "workflow" in selected else "/") + "를 미리보기에 입력하고 서버 터미널을 유지한다. /api/health로 서버 상태를 확인한다.\n" + auth_note + "\n실제 과제의 필수 실행/제출 문서에 맞춰 이 문서를 작성한다. 재사용한 기능과 직접 바꾼 코드·검증·한계를 기록한다. 원본 키트·다른 예제·개인 준비 문서는 이 결과물에 포함하지 않았다. 설치 목록은 DEPENDENCIES.md, 선택 기록은 solution-modules.json을 본다.\n").encode()
    lines = ["# 선택된 설치 의존성", "", "JDK 21과 Bash는 환경에 있어야 한다. Maven 3.9.16 / Node 24.21.0 / npm 12.0.2는 wrapper가 다운로드한다. Python은 생성 도구에만 필요하며 이 앱 실행에는 필요 없다.", "", "## Maven 직접 의존성", "", "버전 생략 항목은 Spring Boot 4.1.1 부모의 dependency management를 따른다.", "", "- org.springframework.boot:spring-boot-starter-web", "- org.springframework.boot:spring-boot-starter-validation"]
    lines += ["- " + coordinate for coordinate in dict.fromkeys(coordinates)]
    lines += ["", "## 프론트 실행 의존성", ""] + [f"- {name}=={version}" for name, version in package["dependencies"].items()]
    lines += ["", "## 빌드 의존성", ""] + [f"- {name}=={version}" for name, version in package["devDependencies"].items()]
    lines += ["", "Vite·TypeScript 등 devDependencies는 화면 빌드에 필요하므로 npm ci --omit=dev로 설치하지 않는다. package-lock.json이 간접 의존성을 고정한다. 이 목록은 선택 모듈을 생성/추가한 시점의 선언이며, 사용자가 의존성을 변경하면 함께 갱신한다.", ""]
    files["DEPENDENCIES.md"] = "\n".join(lines).encode()
    return files


def write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    if path.name in {"mvnw", "npmw"} or path.suffix == ".sh":
        path.chmod(0o755)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("target", type=Path)
    parser.add_argument("--modules", default="", help="Comma-separated auth,workflow,csv,enterprise,stock-adjustment")
    parser.add_argument("--add", action="store_true", help="Add modules to a generated project without overwriting edited files")
    parser.add_argument("--dry-run", action="store_true")
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--domain-file", type=Path)
    group.add_argument("--domain", choices=["support", "procurement", "inventory", "enterprise"])
    args = parser.parse_args()
    target = args.target.absolute()
    previous = None
    if args.add:
        if target.is_symlink() or (target / STATE).is_symlink():
            raise ValueError("Project and state must not be symlinks")
        previous = json.loads((target / STATE).read_text())
        if previous.get("schema_version") != 1:
            raise ValueError("Unsupported project state")
    elif target.exists() or target.is_symlink():
        raise ValueError("Target already exists; use --add for a generated project")
    if not target.parent.is_dir():
        raise ValueError("Target parent must exist")
    requested = list(previous["modules"]) if previous else []
    requested += [name.strip() for name in args.modules.split(",") if name.strip()]
    domain = None
    if args.domain_file:
        domain = json.loads(args.domain_file.read_text())
    elif args.domain:
        domain = json.loads((ROOT / "modules/examples/domains" / (args.domain + ".json")).read_text())
    elif previous and previous.get("domain_path"):
        domain_relative = relative(previous["domain_path"])
        if is_linked(target, domain_relative):
            raise ValueError("Domain file must not be a symlink")
        domain = json.loads((target / domain_relative).read_text())
    if domain:
        requested.append("workflow")
        types = domain.get("types", {}).values()
        if any(item.get("policy") == "enterprise" for item in types):
            requested.append("enterprise")
        if any(action.get("extension") == "stock-adjustment" for item in domain.get("types", {}).values() for action in item.get("actions", {}).values()):
            requested.append("stock-adjustment")
    selected = expand(requested)
    files = render(selected, domain)
    domain_path = f"src/main/resources/fde/domains/{domain['id']}.json" if domain else None
    if previous and domain_path and domain_path == previous.get("domain_path") and not args.domain and not args.domain_file:
        files[domain_path] = (target / domain_path).read_bytes()  # Preserve task-specific content and formatting.
    managed = {path: digest(data) for path, data in files.items()}
    state = {"schema_version": 1, "source_repository": "https://github.com/Kyuwon-Choi/spring-workflow-starter", "source_commit": source_commit(), "modules": selected, "domain_path": domain_path, "managed_sha256": managed}
    changes = {}
    if previous:
        old_hashes = previous["managed_sha256"]
        for path, data in files.items():
            rel = relative(path)
            destination = target / rel
            if is_linked(target, rel):
                raise ValueError(f"Symlink conflict: {path}")
            if old_hashes.get(path) == digest(data):
                continue  # Preserve task-specific edits to files not changed by this module selection.
            if destination.is_file() and digest(destination.read_bytes()) == digest(data):
                continue
            if destination.exists() and (not destination.is_file() or old_hashes.get(path) != digest(destination.read_bytes())):
                raise ValueError(f"Edited or unmanaged file conflict; nothing changed: {path}")
            changes[path] = data
        # Keep the baseline hashes of unchanged generated files, even if a user edited them.
    else:
        changes = files
    plan = {"target": str(target), "modules": selected, "domain": domain.get("id") if domain else None, "generated_files": len(files) + 1, "files_to_write": len(changes) + 1, "mode": "add" if args.add else "create"}
    if args.dry_run:
        print(json.dumps(plan, ensure_ascii=False, indent=2))
        return
    state_bytes = (json.dumps(state, ensure_ascii=False, indent=2) + "\n").encode()
    if not previous:
        with tempfile.TemporaryDirectory(prefix=".solution-", dir=target.parent) as folder:
            staging = Path(folder) / "project"
            staging.mkdir()
            for path, data in files.items():
                write(staging / relative(path), data)
            write(staging / STATE, state_bytes)
            os.rename(staging, target)
    else:
        changes[STATE] = state_bytes
        backups = {path: ((target / path).read_bytes(), (target / path).stat().st_mode) if (target / path).exists() else None for path in changes}
        try:
            for path, data in changes.items():
                write(target / relative(path), data)
        except BaseException:
            for path, backup in backups.items():
                destination = target / path
                if backup is None:
                    destination.unlink(missing_ok=True)
                else:
                    destination.write_bytes(backup[0]); destination.chmod(backup[1])
            raise
    print(json.dumps(plan, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError, json.JSONDecodeError) as error:
        raise SystemExit(str(error)) from None
