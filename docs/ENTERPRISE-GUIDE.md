# 조직 권한과 요구 변경을 적용하는 방법

2026-10-28 구현. 선택형 FdePolicy와 신뢰된 FdeExtension을 기존 공통 엔진에 연결했다. 아래 범위의 실제 검사와 합성 데모를 제공하며, 모든 기업의 임의 정책을 자동으로 처리하는 제품을 뜻하지 않는다.

## 구현된 범위

- 회사·현재 부서·프로젝트 참여·역할·기간 한정 위임을 서버 DB에서 확인한다. 토큰의 업무 소속 정보나 클라이언트의 회사/소유자 입력을 신뢰하지 않는다.
- SQL 조건으로 목록/페이지/총건수/집계를 제한하고 단건·변경에도 동일한 정책 범위를 적용한다. CSV·이력·출력 필드도 정책 경계를 거친다.
- 내부 메모는 직원 출력/이력/CSV에서 제거하며, 검색 색인에서도 제외해 검색 건수로 내용을 추측하지 못하게 한다. 현재는 관리자도 내부 메모 본문으로 검색하지 않는다.
- 생성자만 제출, 본인 신청 승인 금지, 부서/재무 승인자 분리, 조건부 2차 승인, 종료 상태 재처리 거부를 지원한다.
- 제출 시 유효한 변경 불가 승인 규칙 버전을 기록한다. 기존 제출 건은 기존 버전, 반려 재제출은 현재 버전을 선택한다.
- 위임은 시작 포함/종료 제외, 최대 24시간이며 취소와 현재 위임자의 역할/수임자의 프로젝트 참여를 매번 확인한다. 신청자인 위임자를 우회해 대신 승인하지 못한다.
- 같은 회사의 변경은 회사 행의 잠금을 먼저 얻는다. 권한/위임 변경과 승인, 공유 프로젝트 예산 차감의 순서를 정한다. 최종 승인 시만 차감하며 요청/예산/이력 저장이 함께 롤백된다.
- 업무 키의 DB 고유성과 CSV 중복 검사는 회사 범위로 분리한다. 기존 업무는 빈 business_scope를 사용해 기존 의미를 유지한다.

## 실제로 실행하기

Java 21+, 기존 wrapper와 H2를 사용한다. Docker/외부 AI/SSO가 필요하지 않다. 서버가 실행 중이면 먼저 종료하고 빌드한다.

```bash
./npmw ci --no-audit --no-fund
./npmw run webapp:build:dev
./mvnw -ntp -Dskip.installnodenpm -Dskip.npm verify
./scripts/enterprise-demo.sh
```

데모 주소: http://127.0.0.1:8082/organization 와 http://127.0.0.1:8082/fde. 원래 8080 앱과 DB/서명 키를 분리했다. 데모 파일은 .local-data/enterprise.mv.db와 enterprise-jwt-secret이며 소스/제출 묶음에 넣지 않는다. 자동 시작 서비스는 등록하지 않았다.

| 합성 계정 | 암호 | 회사/역할 |
|---|---|---|
| admin | admin | DEMO-A 조직 설정 관리자·D-A 부서장 |
| user | user | DEMO-A 직원, P-A 참여 |
| fde-manager | practicepass | DEMO-A 부서 승인자 |
| fde-finance | practicepass | DEMO-A 재무 승인자 |
| fde-delegate | practicepass | DEMO-A 위임 연습 대상 |
| fde-outsider | practicepass | DEMO-B 부서장/조직 설정 관리자 |

이 계정들은 practice 프로필에서 fde.enterprise.demo.enabled=true를 명시할 때만 생성한다. 기본 8080 실행에는 추가하지 않는다. 데모는 루프백에 바인딩한다. 초기 admin 소속이 이미 있으면 기존 구성을 덮어쓰지 않는다.

## 설정하고 변경하는 순서

1. admin으로 조직 설정 화면에서 프로젝트·예산과 기존 로그인 계정의 소속/역할/참여 프로젝트를 구성한다. 조직 설정 역할만으로 업무 승인 권한을 주지는 않는다.
2. 직원은 업무 화면에서 프로젝트 코드·제목·금액을 입력해 본인이 제출한다. 회사/부서는 서버가 결정한다.
3. 새 규칙을 게시하기 전에 금액별 승인 단계와 시행 시각을 정한다. 빈 시행 시각은 즉시, 빈 금액 기준은 부서 승인으로 완료다. 과거 시행일과 기존 버전 덮어쓰기는 거부한다.
4. 부서 승인자는 담당 범위에서 승인/반려한다. 고액이면 FINANCE_PENDING으로 가며 다른 재무 승인자가 최종 승인한다. 잔여 예산이 부족하면 완료되지 않는다.
5. 부서장은 같은 회사의 해당 프로젝트 참여자에게 기간 한정 승인 위임을 만든다. 취소 이후 시작하는 승인 요청은 거부된다. 먼저 완료된 승인을 소급 취소하지는 않는다.
6. 계정 접근 비활성화·부서 이동·프로젝트 제외는 기존 화면/기존 토큰을 사용한 다음 요청에도 반영한다. 이 기능은 조직 업무 권한의 비활성화이며 시스템 전체 JWT/SSO 세션 취소 기능은 아니다.

## 처음 보는 사람이 따라갈 확인 시나리오

기존 로그인 세션은 로그아웃하고 역할별로 다시 로그인한다. 합성 데이터만 사용한다.

1. admin의 조직 화면에서 P-A 예산과 현재 멤버 역할을 확인한다. 새 규칙 이름 `PRACTICE-V2`, 재무 기준 `300000`, 시행 시각 빈 값(즉시)으로 게시한다. 규칙 이름은 재사용/덮어쓰기하지 않는다.
2. user로 업무 요청을 제목/프로젝트 P-A/금액 300001로 생성하고 제출한다. 회사 DEMO-A·부서 D-A는 서버가 채우며 적용 규칙이 기록된다.
3. fde-manager로 같은 요청을 승인한다. 내부 메모를 입력할 수 있고 상태는 FINANCE_PENDING이어야 한다.
4. fde-finance로 최종 승인한다. APPROVED와 서로 다른 부서/최종 승인자, 생성·제출·부서·재무의 이력을 확인한다.
5. user로 다시 조회해 내부 메모가 상세/이력/CSV에 없는지 확인한다. fde-outsider에게는 같은 요청과 검색 결과가 보이지 않아야 한다.
6. 다른 제목/금액 300000 요청은 부서 승인으로 완료되어야 한다. manager 본인이 신청한 건은 자신의 승인으로 완료할 수 없어야 한다.

구규칙 보존 실습은 V1로 먼저 제출한 건을 남긴 뒤 V2를 게시하고 처리한다. 구건이 V1을 유지하는지 확인한다. 반려 재제출은 새 규칙을 선택한다. 예산이 이미 사용됐다면 새 프로젝트/새 합성 입력으로 연습하고 기존 잔액을 임의 덮어쓰지 않는다.

규칙 effectiveAt과 위임 from/until API는 UTC 오프셋이 있는 ISO Instant를 사용한다. 화면의 로컬 시각은 브라우저에서 ISO 시각으로 변환한다. 시작 포함/종료 제외이며, 위임은 유효 기간 24시간 이내다. 새 규칙의 과거 시행일은 거부되므로 API 호출 지연을 고려해 즉시(null) 또는 충분한 미래 시각을 사용한다.

설정 API 본문은 아래 키를 사용한다. 회사/actor를 요청 본문으로 추가하지 않는다. 기존 로그인 계정 생성은 이 API의 기능이 아니므로 먼저 계정이 있어야 한다.

| POST 경로 (/api/fde-enterprise 뒤) | 입력 |
|---|---|
| /organization | tenant, department. 초기 소속 없는 시스템 관리자만 |
| /projects | code, department, budgetLimit(0 이상 정수) |
| /members | login, department, roles, projects, active, expectedVersion |
| /rules | key, effectiveAt(null 또는 미래 ISO 시각), financeAbove(null 또는 0 이상 정수) |
| /delegations | to, project, from, until |
| /delegations/{id}/revoke | 본문 없음 |

기존 소속 수정의 expectedVersion은 GET 개요에서 읽은 최신 버전을 사용한다. 현재 API는 프로젝트 생성 후 예산 잔액을 덮어쓰는 수정 API를 제공하지 않는다. 공개 데모 계정 암호를 실제 운영에 재사용하지 않는다.

## 다른 업무에 재사용

공통 엔진의 ObjectType에 policy ID를 지정한다. 미지의 policy ID는 서버 시작 시 거부된다. 일반 업무 정의에는 policy를 생략해 기존 동작을 유지한다.

현재 enterprise 어댑터 계약은 title/project/amount와 서버 전용 tenant/department/policyVersion/submittedAt/departmentApprover/finalApprover/internalNote/reason 필드, submit/approve-department/approve-finance/reject 동작과 정해진 상태들이다. 같은 계약의 새 도메인은 정의만 바꿀 수 있으며 tenant-key-test는 다른 도메인/회사별 업무 키를 실제로 검사한 테스트 전용 정의다.

필드·동작·정책 자체가 다르면 별도 FdePolicy/FdeExtension을 구현해 연결한다. 도메인 이름을 공통 FdeService에 추가하지 않는다. 조직 정책은 EnterpriseAccess, 설정 변경은 EnterpriseAdministration, 이 예제의 승인 규칙/예산은 EnterpriseCommands에 있다.

FdePolicy의 lockForWrite/scope/prepareCreate/stamp/visibleFields/searchText/businessScope/canAct가 확장 경계다. FdeExtension.apply는 변경 값과 선언된 상태 중 다음 상태를 반환하며, 기존 changes 구현은 그대로 동작한다. 정책별 검색 색인·회사별 키 범위를 잊지 않게 함께 구현한다.

## API와 실제 검증

기존 /api/fde의 생성·동작·목록·집계·이력·CSV를 그대로 사용한다. /api/fde-enterprise는 현재 조직 개요, 조직 초기 구성, projects/members/rules/delegations와 위임 취소를 제공한다. actor/tenant는 서버에서 결정하고 설정 변경을 감사 기록에 남긴다. 소속 수정은 expectedVersion을 요구한다.

```bash
./scripts/check.sh
./npmw run vitest-run -- --coverage.enabled=false
```

EnterpriseScenarioTest는 조직/프로젝트 격리, 관리자 자기 승인/대리 제출 금지, 시행 시각/금액 경계, 기존 버전 보존/반려 재제출, 위임 시작/만료/취소/역할 제거, 메모/검색 비노출, 승인자 분리, 서로 다른 요청의 공유 예산 경쟁, 감사 실패 롤백, 소속 충돌, 회사별 키를 검사한다. 실제 실행 수치와 HTTP/화면 근거는 작업 공간 rehearsals/2026-10-28-enterprise에 기록한다.

## 적용 한계

현재 한 계정은 한 회사에 속하고 조직은 평면 부서/명시적 프로젝트 참여를 사용한다. 임의 깊이 조직 트리, 다중 회사 동시 소속, SSO, 동적 규칙 편집 언어, 외부 시스템과의 원자성, 실제 개인정보 운영·부하·모든 DB에 대한 검증은 포함하지 않는다. 회사별 변경 직렬화는 정확성을 우선한 선택이며 고처리량 시스템은 잠금 범위를 별도로 설계해야 한다.

기존 일반 업무에 조직 정책을 새로 적용할 때는 과거 데이터의 회사/부서/프로젝트와 정책 버전을 명시적으로 이관해야 한다. 메타데이터가 없는 기존 행을 추측으로 다른 회사에 배정하지 않는다. 필드 타입/상태 목록 자체의 변경도 별도 데이터 migration이 필요하다.

시험에서의 반입·제공 런타임·AI/Computer Use 규칙은 별도 확인 대상이다. 여기서 수행한 화면 검증은 사전 로컬 소프트웨어 검증이다.
