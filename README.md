# FDE Workbench · Spring Boot + React

업무 객체·관계·상태·동작을 정의하고 필요한 계산·권한 정책을 연결하는 Spring Boot + React 키트다. 입력 검증, 목록/검색, 생성/동작, CSV, 충돌 처리와 변경 이력을 공통 엔진으로 처리한다.

```bash
git clone --branch codex/public-starter https://github.com/Kyuwon-Choi/spring-workflow-starter.git
cd spring-workflow-starter
```

## 실행과 미리보기

Java 21+, OpenSSL, 의존성 다운로드 네트워크가 필요하다. Maven/Node/npm은 wrapper로 준비한다. 기본 실행에 Docker·별도 DB·AI API 키는 필요 없다.

```bash
./npmw ci --no-audit --no-fund
./npmw run webapp:build:dev
./mvnw -ntp -Dskip.installnodenpm -Dskip.npm verify
./scripts/preview.sh 8080
```

제공 환경의 PORT에는 **서버 로그의 실제 포트**, URL 경로에는 **`/fde`**를 입력하고 연결한다. 위 실행기는 0.0.0.0에 바인딩한다. 화면을 사용하는 동안 서버 터미널을 유지한다. [미리보기 상세](docs/PREVIEW.md).

로컬 확인 계정은 `user/user`, `admin/admin`이다. DB·생성된 서명 키는 Git에서 제외한 `.local-data/`에 저장된다. 루프백 실행·조직 데모·재빌드는 [QUICKSTART](docs/QUICKSTART.md)를 따른다.

## 과제에 적용

1. 제공 코드·자료와 실제 담당자 답을 읽고 요구/완료 조건을 기록한다. 기존 프로젝트가 있으면 그 구조와 계약을 우선한다.
2. [질문·계획 절차](docs/WORKFLOW.md)로 기반과 첫 실행 흐름을 정한다. [skill](skills/README.md)과 [WORK 템플릿](docs/templates/WORK.md)을 사용할 수 있다.
3. `src/main/resources/fde/domains/*.json`에 객체·필드·관계·상태·동작을 정의한다. 계산은 FdeExtension, 조직/조회/출력 권한은 FdePolicy로 확장한다.
4. 입력→실제 결과를 먼저 연결하고 관련 정상/경계/권한/실패 검사를 수행한다.
5. 분석·문서·배정·AI 기능이 핵심이면 [문제 유형별 레포 카드](docs/REPO-CATALOG.md)에서 필요한 기반/모듈을 고른다.

| 필요한 내용 | 문서 |
|---|---|
| 전체 사용 순서 | [문서 목차](docs/README.md) |
| 문제에 맞는 도구 빠르게 선택 | [선택 기준](docs/REPO-CHOOSER.md) · [20개 카드](docs/REPO-CATALOG.md) |
| 새 업무 매핑·수정 위치 | [업무 적용](docs/ADAPTATION.md) |
| JSON·API·CSV 계약 | [계약/API](docs/DOMAIN-REFERENCE.md) |
| 회사·부서·프로젝트·위임·다단계 승인·예산 | [조직 정책](docs/ENTERPRISE-GUIDE.md) |
| 진행 중 규칙 변경·기존 데이터 | [요구 변경](docs/CHANGE-CONTROL.md) |
| 보안·권한·예외 검사 | [보안](docs/SECURITY.md) |
| 실행 오류·409·DB 잠금 | [오류 해결](docs/TROUBLESHOOTING.md) |
| 검증 명령과 확인 범위 | [검증](docs/VALIDATION.md) |

사전 작성 코드/자료 사용은 해당 평가 규칙이 허용하는 범위에서 한다. 실제 과제 데이터·결과물을 외부에 전송하지 않는다. [제공 환경 적용](docs/ASSESSMENT.md).

[원본·고정 커밋](UPSTREAM.json) · [변경 고지](NOTICE-KIT.md) · [Apache-2.0](LICENSE.txt) · [원본 README](UPSTREAM-README.md)
