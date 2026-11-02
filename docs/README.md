# 실행·적용 문서

각 문서가 지정한 실행 폴더를 따른다. 생성기·참고 도구는 참고 저장소에서, 생성 앱의 build/preview/container 명령은 해당 앱 폴더에서 실행한다. 제공 코드가 있으면 기존 계약·스택을 우선한다.

| 작업 | 문서 |
|---|---|
| 새 폴더에 최소 앱 생성·선택 모듈 추가 | [MODULAR-STARTER](MODULAR-STARTER.md) |
| 환경 준비·선택별 설치 의존성 | [DEPENDENCIES](DEPENDENCIES.md) |
| 선택형 Docker 생성·로컬 빌드/실행 | [CONTAINER](CONTAINER.md) |
| 역할별 세션·파일 소유·변경/재접속 인계 | [SESSION-COORDINATION](SESSION-COORDINATION.md) |
| 여러 CSV의 구조·형식·키/참조 계약 | [DATA-CHECK](DATA-CHECK.md) |
| 제출 전 필수 요구·실행 근거·현재 버전 대조 | [EVIDENCE](EVIDENCE.md) |
| 핵심 계산의 작은 정답·기준선 비교 예제 | [core-validation](../recipes/core-validation/README.md) |
| 시작→인터뷰→합의→계획→구현→확인→제출 | [START-TO-FINISH](START-TO-FINISH.md) |
| 설치·기본 실행·종료·재빌드·데이터 보존 | [QUICKSTART](QUICKSTART.md) |
| 0.0.0.0·실제 포트·/fde·터미널 유지 | [PREVIEW](PREVIEW.md) |
| 담당자 질문→요구→계획→구현·검증 | [WORKFLOW](WORKFLOW.md) |
| 객체·관계·상태·동작과 확장 매핑 | [ADAPTATION](ADAPTATION.md) |
| 필드·권한·API·CSV 형식/한도 | [DOMAIN-REFERENCE](DOMAIN-REFERENCE.md) |
| 회사·부서·프로젝트·위임·조건부 결재 | [ENTERPRISE-GUIDE](ENTERPRISE-GUIDE.md) |
| 추가 요구·규칙 적용 시점·과거 데이터 | [CHANGE-CONTROL](CHANGE-CONTROL.md) |
| 공통 엔진/정의/계산/정책 구조 | [FDE-GUIDE](FDE-GUIDE.md) |
| 문제별 도구 선택 | [REPO-CHOOSER](REPO-CHOOSER.md) · [20개 카드](REPO-CATALOG.md) |
| 권한·정보 노출·실패 원자성 | [SECURITY](SECURITY.md) |
| 접속·로그인·정의·409·DB 오류 | [TROUBLESHOOTING](TROUBLESHOOTING.md) |
| 테스트 명령·현재 검증 범위 | [VALIDATION](VALIDATION.md) |
| 제공 환경·허용 도구·제출 | [ASSESSMENT](ASSESSMENT.md) |
| 단계별 구현 AI 지시문 | [PROMPTS](PROMPTS.md) |
| 요구·계획·검증·인수인계 기록 | [WORK](templates/WORK.md) |
| 질문·서면 합의·계획·핵심 기능 설명 skill 설치/호출 | [skills](../skills/README.md) |

기존 전체 참고 앱의 기본 정의는 support/procurement/inventory이며 enterprise는 업무 소속이 있는 계정에만 노출된다. 신규 최소 생성 앱에는 업무 정의가 없다. 필요한 정의 한 개를 명시하고 실제 요구에 맞게 바꾼다.
