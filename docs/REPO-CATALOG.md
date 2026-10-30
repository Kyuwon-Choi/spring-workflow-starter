# 문제 유형별 GitHub 선택 카드

기준: 2026-10-28 KST. 링크 수를 늘리는 대신 선택 상황·구체적 진입점·고정 커밋·라이선스·수정 지점·제약·검증 상태를 기록한다. 개인 코드 사용 허용과 평가 런타임은 별도 확인 대상이다. 공개 저장소 접근 가능과 즉시 실행 가능을 구분한다.

- **A:** 적힌 범위에서 로컬 실행/테스트 검증. 시험 환경·사용자 숙련·모든 기능을 보증하지 않음.
- **B:** 관련 소스/예제·출처 확인. 후보를 찾는 시간을 줄이지만 런타임 설치·기동은 아직 검증하지 않음.
- **C:** 라이선스·운영 비용 등 때문에 보류/구조 참고.

## 먼저 선택

| 문제 신호 | 먼저 꺼낼 것 | 가져올 범위 |
|---|---|---|
| 제공 저장소가 있음 | 제공 저장소 | 계약/테스트 우선, 아래 모듈만 필요한 만큼 참고 |
| 객체·관계·처리·승인·업무 상태 | Spring FDE Workbench (A) | 공통 엔진+도메인 정의+계산 확장 |
| 파일 정제·정답 계산·집계·리포트 | DuckDB recipe (A) | 입력 검증·SQL·JSON/CSV 출력 |
| 배정·일정·자원·제약 최적화 | Timefold Spring quickstart (A) | planning model·constraints·solver API |
| 표/관리 폼이 주된 개발 비용 | Refine / TanStack (B) | 선택 예제/컴포넌트, 서버 계약은 유지 |
| 분석 화면·차트·필터 | Streamlit (B) | Python UI, 계산은 DuckDB 경로 검토 |
| 문서 추출 | Apache Tika (B) | parser/CLI와 실제 파일 검증 |
| 관계/공정/의존성 시각화 | React Flow (B) | nodes/edges UI |
| 실제 모델/RAG 기능 요구 | Spring AI (B) | 허용된 모델 연결/출력 검증 모듈 |
| Python 특수 라이브러리 중심 웹앱 | FastAPI template (B) | 환경 호환 확인 뒤 베이스 선택 |

CSV는 공통 키트에 Apache Commons CSV(A)를 연결했다. NocoBase(C)는 플랫폼 구조 참고로 보류했다.

새 레포를 선택하기 전 기존 베이스보다 절약할 시간과 설치/통합 비용을 비교한다. 두 인증 체계를 합치지 않고, 외부 예제의 공개 API 주소에 과제 파일/데이터를 보내지 않는다. task 파일·결과물은 외부로 push하지 않는다.

## 선택 카드

### 1. 업무 객체·관계·승인·처리 도구

- **선택:** 주력 베이스 — [jhipster/jhipster-sample-app-react](https://github.com/jhipster/jhipster-sample-app-react)
- **고정:** `88535990553312f138136313c1a8df254c55cb31` · 라이선스 `Apache-2.0` ([원문](https://github.com/jhipster/jhipster-sample-app-react/blob/88535990553312f138136313c1a8df254c55cb31/LICENSE.txt))
- **진입점:** [README.md](https://github.com/jhipster/jhipster-sample-app-react/tree/88535990553312f138136313c1a8df254c55cb31/README.md)
- **재사용:** 이 키트의 공통 엔진/API/화면 + resources/fde/domains 정의. 원본에서는 인증·Spring/React 연결·저장소·빌드를 재사용.
- **문제 공개 후 변경:** 실제 객체·필드·관계·역할/범위·동작을 정의하고 계산을 FdeExtension으로 추가.
- **환경:** Java 21+, Maven wrapper, 전용 Node 24.21.0, H2. 최초 다운로드 네트워크.
- **시작 경로:** ./npmw ci --no-audit --no-fund → ./npmw run webapp:build:dev → ./scripts/dev.sh
- **검증:** 로컬 A: 백엔드 241개/프론트 191개 통과. 세 도메인 JWT 흐름, 조직 확장 HTTP 59요청·브라우저 다단계 승인·재시작 보존 확인. 제공 환경에서의 호환성은 별도 확인 대상.
- **선택하지 않거나 조심할 상황:** 기존 과제 저장소를 통째로 교체하지 않는다. 이미지/OCR/최적화 문제만 있을 때 전체 앱을 먼저 세우지 않는다.

### 2. CSV·표의 정제·집계·검증·리포트

- **선택:** 검증한 분석 경로 — [duckdb/duckdb](https://github.com/duckdb/duckdb)
- **고정:** `8482a57cba609afefaecf96e53dd06314dcb0541` · 라이선스 `MIT` ([원문](https://github.com/duckdb/duckdb/blob/8482a57cba609afefaecf96e53dd06314dcb0541/LICENSE))
- **진입점:** [README.md](https://github.com/duckdb/duckdb/tree/8482a57cba609afefaecf96e53dd06314dcb0541/README.md)
- **재사용:** recipes/analytics/analyze.py와 DuckDB의 read_csv/SQL. DuckDB C++ 저장소를 통째로 빌드할 필요 없음.
- **문제 공개 후 변경:** 실제 열과 계산식·단위·누락·중복 기준. 그룹/측정값 인자는 바로 교체 가능.
- **환경:** 검증: Python 3.13 + duckdb==1.4.1. DB 서버/AI 키 불필요.
- **시작 경로:** recipes/analytics에서 venv 생성 → pip install -r requirements.txt → analyze.py 입력.csv --group 그룹열 --measure 수치열 --out result
- **검증:** 로컬 A: 4개 검사, 알려진 합계·평균과 잘못된 숫자/열·CSV 수식 처리, CLI 출력 확인. 원본 조사 커밋과 설치한 릴리스 버전은 별도 기록.
- **선택하지 않거나 조심할 상황:** 실시간 다중 사용자 업무 상태 관리/권한이 핵심이면 분석 경로만으로 대체하지 않는다.

### 3. 배정·일정·자원 할당·충돌 최소화

- **선택:** 검증한 특수 문제 베이스 — [TimefoldAI/timefold-quickstarts](https://github.com/TimefoldAI/timefold-quickstarts)
- **고정:** `76993a35123a127f9b630a8f7a37a1fbe76a24b0` · 라이선스 `Apache-2.0` ([원문](https://github.com/TimefoldAI/timefold-quickstarts/blob/76993a35123a127f9b630a8f7a37a1fbe76a24b0/LICENSE.txt))
- **진입점:** [getting-started/spring-boot-integration](https://github.com/TimefoldAI/timefold-quickstarts/tree/76993a35123a127f9b630a8f7a37a1fbe76a24b0/getting-started/spring-boot-integration)
- **재사용:** domain 모델, TimetableConstraintProvider의 hard/soft 제약, SolverManager와 REST 흐름.
- **문제 공개 후 변경:** 수업/교실/시간을 실제 작업/설비/슬롯으로 매핑하고 제약·목표·시간 제한을 정의.
- **환경:** 검증: Java 21.0.4 / Spring Boot 4.1.1 / Timefold 2.7.0 / Maven wrapper.
- **시작 경로:** scripts/fetch-timefold.py 대상폴더 → ./mvnw -ntp -f 대상폴더/pom.xml test. 실행은 같은 pom으로 spring-boot:run.
- **검증:** 로컬 A: 원본 예제 10개 중 7개 실행 통과/3개 조건부 skip. REST로 실제 solver 실행과 제약 검사. slow 2개·enterprise 1개 미실행.
- **선택하지 않거나 조심할 상황:** 일반 CRUD 문제에 solver를 도입하지 않는다. 기존 예제의 인증 없는 API를 그대로 운영에 노출하지 않는다. 라이선스가 필요한 enterprise 프로파일은 사용하지 않음.

### 4. 안전한 CSV 파싱·작성·행 오류 처리

- **선택:** 공통 모듈 — [apache/commons-csv](https://github.com/apache/commons-csv)
- **고정:** `4dd81ad5ab15438de5bad509bcaa164c697258e5` · 라이선스 `Apache-2.0` ([원문](https://github.com/apache/commons-csv/blob/4dd81ad5ab15438de5bad509bcaa164c697258e5/LICENSE.txt))
- **진입점:** [src/main/java/org/apache/commons/csv/CSVParser.java](https://github.com/apache/commons-csv/tree/4dd81ad5ab15438de5bad509bcaa164c697258e5/src/main/java/org/apache/commons/csv/CSVParser.java)
- **재사용:** RFC4180 파서/프린터. 쉼표·따옴표·필드 내부 줄바꿈·중복 헤더 처리.
- **문제 공개 후 변경:** 열 매핑·업무 타입·중복·허용 크기·원자적 저장 기준.
- **환경:** 이 키트 Maven에 commons-csv 1.14.1 고정. Java 모듈.
- **시작 경로:** 키트의 FdeService.preview/importRows/exportCsv와 FdeWorkbenchTest를 재사용.
- **검증:** 로컬 A(라이브러리 통합 범위): CSV 관련 API·통합 테스트와 실제 HTTP 입력/출력 통과. upstream 전체 테스트를 돌렸다는 의미는 아님.
- **선택하지 않거나 조심할 상황:** split(",")로 대체하거나 파싱 성공을 업무 데이터 유효성으로 간주하지 않는다.

### 5. React 관리자·검색·폼 화면이 주된 문제

- **선택:** UI 베이스 후보/부분 참고 — [refinedev/refine](https://github.com/refinedev/refine)
- **고정:** `2352eb5b6539e2f39ad9aef652279ad1dcf2c467` · 라이선스 `MIT` ([원문](https://github.com/refinedev/refine/blob/2352eb5b6539e2f39ad9aef652279ad1dcf2c467/LICENSE))
- **진입점:** [examples/base-antd/src/App.tsx](https://github.com/refinedev/refine/tree/2352eb5b6539e2f39ad9aef652279ad1dcf2c467/examples/base-antd/src/App.tsx)
- **재사용:** resource/dataProvider/authProvider, 목록·폼·라우트·Ant Design 구성. examples/base-antd/package.json도 확인.
- **문제 공개 후 변경:** 현재 API 응답 형식·pagination/filter/auth 계약을 dataProvider로 연결. 이 키트는 {items,total}이므로 기본 provider와 대조 필요.
- **환경:** 선택 예제: Node>=20, React 19, Ant Design. 기존 키트의 React Router와 버전 대조 필요.
- **시작 경로:** 상위 monorepo를 전부 가져오기보다 고정 커밋의 examples/base-antd와 의존성을 검토. upstream scripts는 dev=refine dev.
- **검증:** 소스 B: README/라이선스·package.json/App.tsx 확인, 설치·기동 미검증.
- **선택하지 않거나 조심할 상황:** 원본 예제의 외부 fake REST 주소에 실제 과제 자료를 보내지 않는다. 기존 React 앱에 프레임워크를 이중으로 얹는 비용을 검토.

### 6. 큰 표·열 필터·정렬·선택·집계 UI

- **선택:** UI 컴포넌트 참고 — [TanStack/table](https://github.com/TanStack/table)
- **고정:** `c14e0221b1e656c7bb35e7b3d4cdf188719a3599` · 라이선스 `MIT` ([원문](https://github.com/TanStack/table/blob/c14e0221b1e656c7bb35e7b3d4cdf188719a3599/LICENSE))
- **진입점:** [examples/react/basic-use-table/src/main.tsx](https://github.com/TanStack/table/tree/c14e0221b1e656c7bb35e7b3d4cdf188719a3599/examples/react/basic-use-table/src/main.tsx)
- **재사용:** 열 정의·table 상태·렌더링 연결. 현재 main은 새 API 계열이므로 예제와 설치 버전을 일치시킴.
- **문제 공개 후 변경:** 실제 열·서버 페이지 계약·접근 가능한 데이터 범위.
- **환경:** React/TypeScript/Vite 예제, @tanstack/react-table 등 선택 예제 package.json 확인.
- **시작 경로:** 고정 커밋의 examples/react/basic-use-table/package.json과 main.tsx를 함께 확인; dev/start는 vite.
- **검증:** 소스 B: 실제 예제 경로·package.json·코드 확인, 설치·기동 미검증.
- **선택하지 않거나 조심할 상황:** 전체 데이터를 브라우저로 내려받아 권한을 숨기기만 하지 않는다. 작은 표라면 공통 키트 표를 유지.

### 7. Python 데이터 탐색·차트·내부 대시보드

- **선택:** 분석 UI 후보 — [streamlit/streamlit](https://github.com/streamlit/streamlit)
- **고정:** `dd3ab9df1b06d47e953e025f14bbc116dab5d755` · 라이선스 `Apache-2.0` ([원문](https://github.com/streamlit/streamlit/blob/dd3ab9df1b06d47e953e025f14bbc116dab5d755/LICENSE))
- **진입점:** [README.md](https://github.com/streamlit/streamlit/tree/dd3ab9df1b06d47e953e025f14bbc116dab5d755/README.md)
- **재사용:** README Quickstart의 streamlit_app.py 패턴과 dataframe/chart/widgets API.
- **문제 공개 후 변경:** 실제 데이터와 필터·집계·차트. 필요하면 검증한 DuckDB 계산 결과를 사용.
- **환경:** 조사 커밋 lib/pyproject.toml은 Python>=3.11. 설치된 Streamlit 버전과 의존성은 별도 검증 필요.
- **시작 경로:** 라이브러리 설치 후 streamlit run streamlit_app.py. 원격은 허용된 address/port 설정 확인.
- **검증:** 소스 B: README·라이선스·Python 요구 확인, 실제 서버 설치/기동 미검증.
- **선택하지 않거나 조심할 상황:** 복잡한 조직 권한·장기 트랜잭션·승인 시스템을 대시보드 UI만으로 구현했다고 하지 않는다.

### 8. PDF·Word·PPT 등 문서에서 텍스트/메타데이터 추출

- **선택:** 문서 모듈 후보 — [apache/tika](https://github.com/apache/tika)
- **고정:** `edbfbf34b0e74a0f35313533f6503b12d3383970` · 라이선스 `Apache-2.0` ([원문](https://github.com/apache/tika/blob/edbfbf34b0e74a0f35313533f6503b12d3383970/LICENSE.txt))
- **진입점:** [README.md](https://github.com/apache/tika/tree/edbfbf34b0e74a0f35313533f6503b12d3383970/README.md)
- **재사용:** Quick Start의 Tika facade/파서 또는 tika-app CLI. 추출 결과와 원본 근거를 보존.
- **문제 공개 후 변경:** 허용 파일 형식·크기·페이지·암호화·스캔/OCR·추출 실패 정책.
- **환경:** 조사 README는 Java 17 기반, 현재 4.x API 변경 안내 존재. 실제 배포 릴리스/의존성은 고정 후 검증.
- **시작 경로:** 공식 배포 tika-app JAR의 --text 또는 README Java 예제부터 확인. 서버 전체를 먼저 띄우지 않음.
- **검증:** 소스 B: README·라이선스 확인. parser/실제 PDF·OCR 실행 미검증.
- **선택하지 않거나 조심할 상황:** 텍스트 추출 성공을 내용 정확성으로 일반화하지 않는다. Tika가 모든 스캔 PDF OCR을 자동 해결한다고 가정하지 않음.

### 9. 객체 관계·의존성·프로세스의 그래프 화면

- **선택:** UI 모듈 후보 — [xyflow/xyflow](https://github.com/xyflow/xyflow)
- **고정:** `c4fdcb1150e247b715545fc1b045a213daed37ce` · 라이선스 `MIT` ([원문](https://github.com/xyflow/xyflow/blob/c4fdcb1150e247b715545fc1b045a213daed37ce/LICENSE))
- **진입점:** [examples/react/src/examples/Basic/index.tsx](https://github.com/xyflow/xyflow/tree/c4fdcb1150e247b715545fc1b045a213daed37ce/examples/react/src/examples/Basic/index.tsx)
- **재사용:** React Flow nodes/edges/interaction. MIT core와 별도 Pro 예제를 구분.
- **문제 공개 후 변경:** 객체·관계 데이터 매핑, 노드 상세·선택·편집 권한.
- **환경:** React/TypeScript, @xyflow/react와 CSS import. 키트와 버전 호환은 별도 확인.
- **시작 경로:** README Installation/Usage와 고정 Basic 예제를 함께 참조.
- **검증:** 소스 B: 실제 예제 코드·라이선스 확인, 설치·기동 미검증.
- **선택하지 않거나 조심할 상황:** 그래프 렌더러를 workflow 실행 엔진이나 그래프 DB로 간주하지 않는다.

### 10. 문서 검색·구조화 생성·요약 등 실제 AI 기능

- **선택:** AI 통합 모듈 후보 — [spring-projects/spring-ai](https://github.com/spring-projects/spring-ai)
- **고정:** `b90fb6f847ff7b35ab5162ced29012320240aaba` · 라이선스 `Apache-2.0` ([원문](https://github.com/spring-projects/spring-ai/blob/b90fb6f847ff7b35ab5162ced29012320240aaba/LICENSE.txt))
- **진입점:** [README.md](https://github.com/spring-projects/spring-ai/tree/b90fb6f847ff7b35ab5162ced29012320240aaba/README.md)
- **재사용:** ChatClient, 구조화 출력·retrieval·tool 경계의 공식 문서. 모델 제안도 서버 권한/검증을 거침.
- **문제 공개 후 변경:** 실제로 허용된 제공자·모델·근거·출력 스키마·timeout·실패 처리.
- **환경:** Spring Boot/AI 버전 호환 및 제공 API를 먼저 확인. 이 키트에는 모델 키/실제 호출이 없음.
- **시작 경로:** README reference docs → ChatClient/getting-started. 승인되지 않은 개인 API 키·유료 구독·모델을 연결하지 않음.
- **검증:** 소스 B: README·라이선스 확인, 모델/RAG/외부 호출 미검증.
- **선택하지 않거나 조심할 상황:** AI 기능 요구가 없으면 도입하지 않는다. 가짜 응답·mock 통과를 실제 모델 연동으로 보고하지 않음.

### 11. Python 전용 라이브러리가 핵심인 업무 웹앱

- **선택:** 대안 베이스 후보 — [fastapi/full-stack-fastapi-template](https://github.com/fastapi/full-stack-fastapi-template)
- **고정:** `b1125d8839482cb7bbae603ddc6d0081bf87532e` · 라이선스 `MIT` ([원문](https://github.com/fastapi/full-stack-fastapi-template/blob/b1125d8839482cb7bbae603ddc6d0081bf87532e/LICENSE))
- **진입점:** [backend/app/api/routes/items.py](https://github.com/fastapi/full-stack-fastapi-template/tree/b1125d8839482cb7bbae603ddc6d0081bf87532e/backend/app/api/routes/items.py)
- **재사용:** FastAPI/Pydantic/React/PostgreSQL 기반. 소유자 검사·DTO·테스트 구조.
- **문제 공개 후 변경:** 실제 객체·관계·동작과 권한, Python 특수 처리.
- **환경:** 고정 조사 커밋은 Python>=3.14,<4.0 및 PostgreSQL. 실행 환경이 맞지 않으면 즉시 베이스로 선택하지 않음.
- **시작 경로:** 고정 커밋 README/development.md와 backend/pyproject.toml 확인 후 기존 실행 절차 사용.
- **검증:** 소스 B: 10/25 clone·고정 커밋 보안 관련 소스 확인, 설치/기동 미검증.
- **선택하지 않거나 조심할 상황:** Spring에 익숙한 상태에서 단순 API 호출만을 이유로 스택 전체를 바꾸지 않음.

### 12. 설정 중심 데이터 모델·폼·권한·워크플로 플랫폼

- **선택:** 보류/제품 구조 참고 — [nocobase/nocobase](https://github.com/nocobase/nocobase)
- **고정:** `fa2db5166197dff6f5ace9b444ee4a7070d586e2` · 라이선스 `NOASSERTION` ([원문](https://github.com/nocobase/nocobase/blob/fa2db5166197dff6f5ace9b444ee4a7070d586e2/LICENSE.txt))
- **진입점:** [README.md](https://github.com/nocobase/nocobase/tree/fa2db5166197dff6f5ace9b444ee4a7070d586e2/README.md)
- **재사용:** 데이터 모델·블록·액션·역할을 구성하는 제품 접근. 라이선스/에디션·설정물 제출을 먼저 검토.
- **문제 공개 후 변경:** 실제 collection·관계·화면·action·권한과 구성물 재현.
- **환경:** 별도 플랫폼 설치와 DB, 플러그인/에디션 의존. 런타임 설치는 미검증.
- **시작 경로:** README Quick Start와 LICENSE.txt부터 확인. 이번 주력 실행 경로로 지정하지 않음.
- **검증:** 소스 C: 공개 소스·커스텀 라이선스 확인만. 단순 Apache-2.0 오픈소스로 표기하지 않음.
- **선택하지 않거나 조심할 상황:** 공개 저장소라는 사실만으로 모든 에디션/사용/재배포가 자유롭다고 판단하지 않음. 시험 직전 새 플랫폼 학습 경로로 전환하지 않음.

## 고정 소스 확인

공개 자료를 가져올 수 있는 환경에서 아래 순서로 특정 커밋을 확인한다. OWNER/REPO/SHA는 위 카드의 실제 값으로 채운다. 전체 monorepo를 설치하기 전에 선택한 진입점·package/pom·원본 고지를 확인한다.

```bash
git init reference
git -C reference remote add origin https://github.com/OWNER/REPO.git
git -C reference fetch --depth 1 origin SHA
git -C reference checkout --detach FETCH_HEAD
```

고정 소스와 설치하는 배포 릴리스 버전은 같은 개념이 아니다. Maven/npm/pip 의존성도 검증한 버전으로 고정한다. 위 명령은 소스 취득 방법이며 B/C 후보의 실제 설치 통과 기록을 뜻하지 않는다.

Timefold는 이 키트의 fetch-timefold.py가 검증한 Spring 하위 예제와 원본 라이선스만 고정 커밋에서 내려받는다. 원본 전체 빌드·enterprise·상용 라이선스 설치는 포함하지 않는다.
