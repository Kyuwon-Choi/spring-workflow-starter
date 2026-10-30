# 문제 유형별 GitHub 선택 카드

기존 12개 카드 기준 2026-10-28, 추가 8개 기준 2026-10-30 KST. 링크 수를 늘리는 대신 선택 상황·구체적 진입점·고정 커밋·라이선스·수정 지점·제약·검증 상태를 기록한다. 개인 코드 사용 허용과 평가 런타임은 별도 확인 대상이다. 공개 저장소 접근 가능과 즉시 실행 가능을 구분한다.

- **A:** 적힌 범위에서 로컬 실행/테스트 검증. 시험 환경·사용자 숙련·모든 기능을 보증하지 않음.
- **B:** 관련 소스/예제·출처 확인. 후보를 찾는 시간을 줄이지만 런타임 설치·기동은 아직 검증하지 않음.
- **C:** 라이선스·운영 비용 등 때문에 보류/구조 참고.

새 도구를 고를 때는 [문제별 빠른 판단](REPO-CHOOSER.md)을 먼저 본다. A는 적힌 최소 실행 범위이며 애플리케이션 통합 완료를 뜻하지 않는다.

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
| Excel 다중 시트·수식·파일 결과 | Apache POI (독립 A) | Java 파일 모듈 |
| PDF 페이지별 원문/근거 | PDFBox (독립 A) | PDF 전용 추출 |
| 이름/품번의 유사 매칭 | RapidFuzz (독립 A) | 후보/점수·모호함 처리 |
| 경로·선후관계·순환 | NetworkX (독립 A) | 그래프 계산 |
| KPI/추이/분포 차트 | ECharts (B) | 기존 화면에 차트 컴포넌트 |
| 중첩 JSON/AI 출력 형식 | networknt JSON Schema (B) | Jackson 호환 검증 모듈 |
| 외부 API 장애/재시도 | Resilience4j (B) | 코어 decorator·멱등성 |
| 스캔 이미지 OCR | Tesseract (B·설치 부담 높음) | 네이티브 OCR·언어 데이터 |

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

### 13. Excel 다중 시트·수식·파일 입출력

- **선택:** Java 파일 처리 모듈 — [apache/poi](https://github.com/apache/poi)
- **고정:** `17de566ac64dee14bc1c132b9b4972b9a21165b3` · 라이선스 `Apache-2.0` ([원문](https://github.com/apache/poi/blob/17de566ac64dee14bc1c132b9b4972b9a21165b3/legal/LICENSE))
- **진입점:** [poi-ooxml/src/main/java/org/apache/poi/xssf/usermodel/XSSFWorkbook.java](https://github.com/apache/poi/blob/17de566ac64dee14bc1c132b9b4972b9a21165b3/poi-ooxml/src/main/java/org/apache/poi/xssf/usermodel/XSSFWorkbook.java), [공식 사용 예](https://poi.apache.org/components/spreadsheet/quick-guide.html)
- **재사용:** XSSFWorkbook/WorkbookFactory, DataFormatter와 FormulaEvaluator. XLSX 읽기/쓰기와 시트·열 매핑.
- **문제 공개 후 변경:** 실제 시트/헤더/날짜/빈 셀/수식·캐시 값·중복 기준. 대용량이면 SAX/SXSSF 경로를 별도 선택.
- **환경/비용:** 독립 실행 확인: Java 21 + org.apache.poi:poi-ooxml:6.0.0. DB/외부 API 불필요. Java 의존성 추가.
- **시작 경로:** poi-ooxml:6.0.0 의존성 → WorkbookFactory.create(input) → 시트/셀 검증·변환 → 요구 파일 출력. 라이브러리 자체를 소스 빌드할 필요 없음.
- **검증:** 독립 실행 A: XLSX 저장/재개방, 2개 시트, 수식 결과 300000, 한글 셀 확인. Spring 통합·실제 제공 파일·암호 파일·대용량은 미검증.
- **선택하지 않거나 조심할 상황:** CSV만 필요하면 기존 DuckDB/Commons CSV를 사용. 모든 수식/서식/차트의 Excel 동등 지원을 가정하지 않음. ZIP/메모리·파일 크기 제한 유지.

### 14. PDF 페이지별 텍스트·근거 추출

- **선택:** Java PDF 전용 모듈 — [apache/pdfbox](https://github.com/apache/pdfbox)
- **고정:** `739c1b9627afd843451bb67c2af749183038f5ff` · 라이선스 `Apache-2.0` ([원문](https://github.com/apache/pdfbox/blob/739c1b9627afd843451bb67c2af749183038f5ff/LICENSE.txt))
- **진입점:** [pdfbox/src/main/java/org/apache/pdfbox/text/PDFTextStripper.java](https://github.com/apache/pdfbox/blob/739c1b9627afd843451bb67c2af749183038f5ff/pdfbox/src/main/java/org/apache/pdfbox/text/PDFTextStripper.java), [공식 사용 예](https://pdfbox.apache.org/3.0/migration.html)
- **재사용:** PDFBox 3 Loader.loadPDF와 PDFTextStripper의 페이지 범위. 추출 텍스트와 원본 페이지 번호 보존.
- **문제 공개 후 변경:** 페이지/문단 근거·읽기 순서·암호화·읽기 실패·텍스트 없음 정책. 표 구조 복원은 별도.
- **환경/비용:** 독립 실행 확인: Java 21 + org.apache.pdfbox:pdfbox:3.0.8. Java 의존성 추가.
- **시작 경로:** pdfbox:3.0.8 → Loader.loadPDF(file) → PDFTextStripper.setStartPage/setEndPage → 원문과 페이지 함께 반환.
- **검증:** 독립 실행 A: 2페이지 PDF, 페이지별 텍스트 분리, 잘못된 PDF 거부 확인. OCR·한글 PDF 글꼴/인코딩·표/스캔 문서는 미검증.
- **선택하지 않거나 조심할 상황:** Tika는 여러 형식 추출, PDFBox는 PDF 페이지 제어가 필요할 때 선택. 스캔 PDF에서 텍스트가 없으면 OCR이 필요하며 빈 추출을 정답으로 처리하지 않음.

### 15. 설비명·품목명 유사 매칭과 중복 후보

- **선택:** Python 매칭 모듈 — [rapidfuzz/RapidFuzz](https://github.com/rapidfuzz/RapidFuzz)
- **고정:** `09328422916b13181238660cd3b23a71905d1ec9` · 라이선스 `MIT` ([원문](https://github.com/rapidfuzz/RapidFuzz/blob/09328422916b13181238660cd3b23a71905d1ec9/LICENSE))
- **진입점:** [README.md](https://github.com/rapidfuzz/RapidFuzz/blob/09328422916b13181238660cd3b23a71905d1ec9/README.md), [공식 사용 예](https://github.com/rapidfuzz/RapidFuzz#usage)
- **재사용:** process.extract/extractOne, fuzz.WRatio와 명시적 processor. 후보 점수·원본 키·검토 상태 보존.
- **문제 공개 후 변경:** 정규화·블로킹 키·임계값·동점·자동 확정/수동 검토 기준을 담당자에게 확인.
- **환경/비용:** 독립 실행 확인: Python 3.13 + RapidFuzz==3.14.6. PyPI 요구 Python>=3.11. 소스 빌드는 C++17이 필요할 수 있으므로 wheel 확인. Python wheel 설치.
- **시작 경로:** pip install RapidFuzz==3.14.6 → process.extract(..., processor=utils.default_process, score_cutoff=기준).
- **검증:** 독립 실행 A: 정규화 후 일치·동점 후보 보존·기준 미달 결과 없음 확인. 실제 업무 식별 정확도나 자동 병합 허용은 미검증.
- **선택하지 않거나 조심할 상황:** 유사도 점수를 동일 개체 확정으로 간주하지 않음. 3.x는 기본 정규화를 자동 수행하지 않음. 동점/유사 품번은 원본을 보존해 검토.

### 16. 최단 경로·선후관계·순환·연결성 계산

- **선택:** Python 그래프 계산 모듈 — [networkx/networkx](https://github.com/networkx/networkx)
- **고정:** `8ff5a974cd03813a676c7fa5fdf4e9b6ee710a7f` · 라이선스 `BSD-3-Clause` ([원문](https://github.com/networkx/networkx/blob/8ff5a974cd03813a676c7fa5fdf4e9b6ee710a7f/LICENSE.txt))
- **진입점:** [README.rst](https://github.com/networkx/networkx/blob/8ff5a974cd03813a676c7fa5fdf4e9b6ee710a7f/README.rst), [공식 사용 예](https://networkx.org/documentation/stable/reference/algorithms/shortest_paths.html)
- **재사용:** DiGraph, shortest_path, topological_sort, is_directed_acyclic_graph. 그래프 결과를 API/파일로 반환.
- **문제 공개 후 변경:** 노드/방향/가중치/도달 불가/순환·동률 기준. 입력 규모와 음수 비용 여부 확인.
- **환경/비용:** 독립 실행 확인: Python 3.13 + networkx==3.7. PyPI 요구 Python>=3.12, 3.14.1 제외. Python 패키지 설치.
- **시작 경로:** pip install networkx==3.7 → 실제 edges로 DiGraph 작성 → 요구 알고리즘 → 원본 노드 ID로 결과 반환.
- **검증:** 독립 실행 A: 양의 가중치 최단 경로/길이, 경로 없음, 위상 순서, 순환 감지 확인. 큰 그래프 성능·음수 비용은 미검증.
- **선택하지 않거나 조심할 상황:** React Flow는 표시 도구, NetworkX는 계산 도구. 최단 경로를 차량/일정 전체 제약 최적화와 동일시하지 않음.

### 17. KPI·추이·분포·공정 대시보드 차트

- **선택:** JavaScript 시각화 모듈 — [apache/echarts](https://github.com/apache/echarts)
- **고정:** `34f4d927eafe251f26f63afd3aeefa30f2feba30` · 라이선스 `Apache-2.0` ([원문](https://github.com/apache/echarts/blob/34f4d927eafe251f26f63afd3aeefa30f2feba30/LICENSE))
- **진입점:** [README.md](https://github.com/apache/echarts/blob/34f4d927eafe251f26f63afd3aeefa30f2feba30/README.md), [공식 사용 예](https://echarts.apache.org/handbook/en/how-to/chart-types/bar/basic-bar/)
- **재사용:** echarts.init, setOption, resize/dispose와 bar/line 등 공식 option 예제. 기존 React 컴포넌트에서 수명주기 연결.
- **문제 공개 후 변경:** 정확한 집계/단위/기간/결측·필터 기준과 서버 데이터 범위. 차트 종류는 목적에 맞춰 선택.
- **환경/비용:** 소스 확인 B: 순수 JavaScript 라이브러리. 실제 설치 버전·브라우저·React 통합은 미검증. 프론트 의존성·컴포넌트 연결.
- **시작 경로:** 공식 Basic Bar option을 최소 컴포넌트에 연결 → 현재 API의 집계 응답으로 교체 → 실제 브라우저 표시/resize 확인.
- **검증:** 소스 B: README·라이선스·공식 Basic Bar 예제 확인. 설치/화면 렌더링·React 통합은 미실행.
- **선택하지 않거나 조심할 상황:** 차트가 집계 정답/권한 검사를 대신하지 않음. 이미 간단한 표로 요구를 충족하면 추가하지 않음.

### 18. 중첩 JSON·외부 API·AI 출력의 구조 검증

- **선택:** Java JSON Schema 검증 모듈 — [networknt/json-schema-validator](https://github.com/networknt/json-schema-validator)
- **고정:** `53a6c00152459b035ae4fa2e26f3253bee52d71e` · 라이선스 `Apache-2.0` ([원문](https://github.com/networknt/json-schema-validator/blob/53a6c00152459b035ae4fa2e26f3253bee52d71e/LICENSE))
- **진입점:** [README.md](https://github.com/networknt/json-schema-validator/blob/53a6c00152459b035ae4fa2e26f3253bee52d71e/README.md), [공식 사용 예](https://github.com/networknt/json-schema-validator#usage)
- **재사용:** SchemaRegistry/스키마 검증, required/type/enum/additionalProperties 등. 승인된 스키마를 코드에서 관리.
- **문제 공개 후 변경:** 허용 필드/중첩 구조/범위/오류 경로/format 적용 여부. 업무 의미·권한은 별도 서버 검사.
- **환경/비용:** 소스 확인 B: 3.x는 Java17+/Jackson3, 2.x는 Java8+/Jackson2. Maven 배포 3.0.8 확인. 이 키트는 Jackson3이지만 실제 통합 미검증. Jackson 버전 호환 확인 필요.
- **시작 경로:** Jackson 계열 확인 → com.networknt:json-schema-validator:3.0.8 후보 → 로컬 고정 schema로 정상/누락/잘못된 type 검사.
- **검증:** 소스 B: README의 버전 분기·사용 예·라이선스와 Maven 배포 버전 확인. 라이브러리 실행/키트 통합은 미실행.
- **선택하지 않거나 조심할 상황:** 스키마 통과가 업무 정답을 보장하지 않음. 사용자 제공 $ref/URI의 임의 원격 취득을 허용하지 말고 로컬 허용 스키마로 제한.

### 19. 외부 API의 재시도·차단·동시 호출 제한

- **선택:** Java 장애 처리 모듈 후보 — [resilience4j/resilience4j](https://github.com/resilience4j/resilience4j)
- **고정:** `aa9aacb8cb5333f2dee2590ff07f05f2c942d602` · 라이선스 `Apache-2.0` ([원문](https://github.com/resilience4j/resilience4j/blob/aa9aacb8cb5333f2dee2590ff07f05f2c942d602/LICENSE.txt))
- **진입점:** [resilience4j-retry/src/main/java/io/github/resilience4j/retry/Retry.java](https://github.com/resilience4j/resilience4j/blob/aa9aacb8cb5333f2dee2590ff07f05f2c942d602/resilience4j-retry/src/main/java/io/github/resilience4j/retry/Retry.java), [공식 사용 예](https://github.com/resilience4j/resilience4j)
- **재사용:** Retry/CircuitBreaker/Bulkhead 코어 decorator 중 필요한 부분. 실제 HTTP 클라이언트 timeout과 조합.
- **문제 공개 후 변경:** 재시도 가능한 오류·시도 횟수/간격·멱등성·제한·실패 표시·복구 기준.
- **환경/비용:** 소스 확인 B: 조사 main 3.x는 Java21 요구. Maven 공개 retry 배포는 2.4.0으로 소스 계열과 구분. Boot4 starter 호환·통합은 미검증. 기존 클라이언트·멱등성·버전 확인 필요.
- **시작 경로:** 먼저 HTTP connect/read timeout 설정 → 사용할 배포 버전의 코어 Retry API 확인 → 허용된 멱등 호출만 감쌈 → 실패/한도 확인.
- **검증:** 소스 B: 코어 Retry 진입점·라이선스·공식 README 패턴/버전 차이 확인. 실제 네트워크 장애/키트 통합은 미실행.
- **선택하지 않거나 조심할 상황:** 승인/결제 같은 비멱등 변경을 무조건 재시도하지 않음. 실패를 가짜 정상값으로 감추지 않음. Spring Boot3 starter를 Boot4에 그대로 추가하지 않음.

### 20. 스캔 이미지·이미지 PDF의 OCR

- **선택:** 조건부 네이티브 OCR 후보 — [tesseract-ocr/tesseract](https://github.com/tesseract-ocr/tesseract)
- **고정:** `7e548e6eff172f38f97664afe7f7ce40db36c8d6` · 라이선스 `Apache-2.0` ([원문](https://github.com/tesseract-ocr/tesseract/blob/7e548e6eff172f38f97664afe7f7ce40db36c8d6/LICENSE))
- **진입점:** [README.md](https://github.com/tesseract-ocr/tesseract/blob/7e548e6eff172f38f97664afe7f7ce40db36c8d6/README.md), [공식 사용 예](https://tesseract-ocr.github.io/tessdoc/Command-Line-Usage.html)
- **재사용:** tesseract CLI와 필요한 언어 traineddata. PDF는 먼저 허용된 도구로 페이지 이미지화.
- **문제 공개 후 변경:** 언어/해상도/회전·페이지 제한·처리시간·인식 실패/검토 기준. OCR 텍스트와 원본 근거 유지.
- **환경/비용:** 소스 확인 B: 네이티브 실행 파일·Leptonica·언어 데이터 필요. kor+eng 사용 전 해당 traineddata 설치 여부 확인. 높음: 네이티브 실행기·언어 데이터.
- **시작 경로:** tesseract --version 및 --list-langs 확인 → 설치돼 있을 때 tesseract input.png stdout -l kor+eng. PDF는 직접 입력하지 않음.
- **검증:** 소스 B: README/CLI/라이선스·이미지 입력/언어 데이터 요구 확인. 네이티브 설치·한국어 OCR·실제 문서 인식은 미실행.
- **선택하지 않거나 조심할 상황:** 텍스트 PDF는 PDFBox/Tika부터 확인. 실행 파일·언어 데이터가 없으면 단순 pip install로 해결됐다고 하지 않음. 문서를 임의 외부 OCR 서비스로 보내지 않음.

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
