# FDE Workbench · Spring Boot + React

업무의 **객체·필드·관계·상태·동작**을 정의하고, 필요한 계산과 조직 정책을 연결하는 개인 개발 키트다. 로그인·레이아웃 외에도 입력 검증, 조회, 권한, 승인, 버전 충돌, 이력과 CSV 처리를 재사용한다. JHipster 공식 React 샘플의 고정 커밋에서 출발했다.

고객 문의·구매 요청·재고는 기본 예제다. 회사/부서/프로젝트 권한·기간 위임·다단계 승인·규칙 버전·공유 예산은 선택형 조직 정책으로 제공한다. 새 업무에 맞는 정책·계산·검증은 직접 연결해야 한다. Palantir Foundry와 동등한 제품이나 미지 과제의 80% 해결을 보장하는 도구는 아니다.

공개 소스: [Kyuwon-Choi/spring-workflow-starter](https://github.com/Kyuwon-Choi/spring-workflow-starter). 아래 브랜치를 복제하면 원본 샘플 대신 이 키트의 소스와 가이드를 받는다.

```bash
git clone --branch codex/public-starter https://github.com/Kyuwon-Choi/spring-workflow-starter.git
cd spring-workflow-starter
```

## 처음 실행

Java 21+, 네트워크, OpenSSL이 필요하다. Maven/Node는 wrapper로 준비한다. 로컬 검증은 macOS, Java 21.0.4 / Maven 3.9.16 / Node 24.21.0 / npm 12.0.2 / H2에서 수행했다. 다른 버전·운영체제의 호환성을 보장하지 않는다. Docker·별도 DB·AI API 키는 필요 없다.

저장소 최상위에서 실행한다.

```bash
./npmw ci --no-audit --no-fund
./npmw run webapp:build:dev
./scripts/dev.sh
```

서버 준비 메시지 후 <http://127.0.0.1:8080/fde>에 접속한다. 로컬 합성 계정은 `user/user`, `admin/admin`이다. `.local-data/`에 DB와 생성된 서명 키가 저장된다. 공개 데모 계정으로 외부에 서비스하지 않는다.

조직 권한 데모는 프론트 빌드 후 JAR를 먼저 만든다. 빌드 전 실행 중인 서버는 해당 터미널에서 종료한다.

```bash
./mvnw -ntp -Dskip.installnodenpm -Dskip.npm verify
./scripts/enterprise-demo.sh
```

<http://127.0.0.1:8082/organization>에서 구성하고 <http://127.0.0.1:8082/fde>에서 승인한다. 기본 앱과 DB·키를 분리한다. 역할별 계정과 확인 순서는 [조직 업무 가이드](docs/ENTERPRISE-GUIDE.md)를 따른다.

## 사용 가이드

| 지금 할 일 | 문서 |
|---|---|
| 전체 문서에서 필요한 것 찾기 | [문서 목차](docs/README.md) |
| 설치·실행·종료·재빌드·데이터 보존 | [빠른 시작](docs/QUICKSTART.md) |
| 담당자 질문 → 요구 → 계획 → 구현 → 제출 | [작업 절차와 skill](docs/WORKFLOW.md) |
| 새 업무를 직접 붙여보기 | [업무 적용 실습](docs/ADAPTATION.md) |
| JSON 필드·권한·동작과 API 확인 | [계약/API 참고](docs/DOMAIN-REFERENCE.md) |
| 회사·부서·프로젝트·위임·조건부 결재 | [조직 업무](docs/ENTERPRISE-GUIDE.md) |
| 기존 데이터와 새 요구를 함께 유지 | [요구 변경](docs/CHANGE-CONTROL.md) |
| 구조·재사용 경계 이해 | [FDE 구조](docs/FDE-GUIDE.md) |
| 분석·문서·최적화 등 다른 기반 선택 | [고정 GitHub 선택 카드](docs/REPO-CATALOG.md) |
| 권한·정보 노출·입력·트랜잭션 점검 | [보안과 검증](docs/SECURITY.md) |
| 테스트 명령과 실제 검증 범위 | [검증 기록](docs/VALIDATION.md) |
| 실행/로그인/도메인/버전 충돌 문제 | [오류 해결](docs/TROUBLESHOOTING.md) |
| 하루 1시간 연습과 전체 리허설 | [연습 가이드](docs/REHEARSAL.md) |
| 평가 환경에서 준비물을 사용하기 전 확인 | [평가 환경 적용](docs/ASSESSMENT.md) |
| 내 GitHub로 가져가기 전 검토 | [GitHub 게시 준비](docs/GITHUB-PUBLISH.md) |

## 수정 위치

- 업무 정의: `src/main/resources/fde/domains/*.json`.
- 계산·상태 분기: `FdeExtension`; 조직/출력/조회 범위: `FdePolicy`.
- 예제 구현: `StockAdjustment`, `EnterpriseAccess`, `EnterpriseCommands`, `EnterpriseAdministration`.
- 공통 엔진/API: `src/main/java/io/github/jhipster/sample/fde/`.
- 공통 화면: `src/main/webapp/app/modules/fde/`.
- DB 변경: 새 Liquibase changeSet. 기존 changeSet은 수정하지 않는다.
- 질문·계획: [skills](skills/README.md), 기록: [WORK 템플릿](docs/templates/WORK.md).
- 분석 CLI: [DuckDB recipe](recipes/analytics/README.md).

정의는 서버 시작 시 검증한다. 임의 JSON/스크립트를 HTTP로 등록하는 기능은 없다. 기존 저장소 수정 과제라면 제공 코드부터 실행하고 필요한 패턴만 적용한다.

## 검증과 한계

2026-10-28 로컬 기준 백엔드 **241개**, 프론트 **191개** 통과. 별도로 합성 계정 HTTP 59요청, 브라우저 다단계 승인과 재시작 보존을 확인했다. 서로 다른 검사 수를 단순 합산해 요구 충족률로 환산하지 않는다. [검증 기록](docs/VALIDATION.md)에 증거와 재현 명령이 있다.

한 계정 한 회사·평면 부서·명시적 프로젝트까지 구현했다. SSO, 임의 조직 트리, 다중 회사 소속, 외부 시스템과 원자적 처리, 운영 부하/장애, 모든 DB 엔진은 검증하지 않았다. 사전 작성 개인 키트의 평가 반입 허용도 별도 확인해야 한다.

[원본/커밋](UPSTREAM.json) · [변경 고지](NOTICE-KIT.md) · [Apache-2.0](LICENSE.txt) · [원본 README](UPSTREAM-README.md)
