# 과제 요구를 업무 정의에 적용

신규 과제는 [모듈형 최소 앱](MODULAR-STARTER.md)을 먼저 생성한다. 생성 앱 패키지는 com.example.solution이며 아래 엔진/정의 계약을 선택한 모듈 범위에서 사용한다.

제공 코드가 있으면 기존 구조·API·테스트를 먼저 확인한다. 신규 업무 도구에 이 키트를 적용할 때 아래 순서를 사용한다.

## 1. 요구를 코드에 매핑

WORK에 사용자·입력·객체·관계·상태·동작·권한·출력과 완료 조건을 적는다. 요구마다 그대로 사용/정의 변경/확장 구현/다른 기반을 판단한다.

| 요구 | 수정 위치 | 확인할 조건 |
|---|---|---|
| 필드·필수·범위·enum·반려 입력 | 도메인 JSON | 타입/누락/경계/중복 |
| 객체 간 참조 | 같은 도메인의 reference 정의 | 실제 대상 ID와 접근 범위 |
| 수량·상한·지원액 계산 | FdeExtension | 독립 기대값·음수·경계·실패 롤백 |
| 회사/부서/프로젝트·민감 필드 | FdePolicy | SQL·단건·동작·CSV·이력·검색 |
| 조건부 승인 단계·공유 예산 | EnterpriseCommands 패턴 | 적용 버전·승인자 분리·동시성 |
| 파일 정제·정답/집계 | 분석 recipe | 키·최신 기준·분모·오류 결과 |
| 외부 연동 | 별도 실패/재시도 경계 | DB 롤백으로 외부 동작이 취소되지 않음 |

## 2. 정의 작성

`src/main/resources/fde/domains/*.json`에서 가까운 정의를 참고해 고유 id와 실제 필드·상태·권한·동작을 작성한다. 완전한 형식 예제는 [service-request.json](examples/service-request.json), 정확한 계약은 [DOMAIN-REFERENCE](DOMAIN-REFERENCE.md)다.

공통 엔진이 특정 도메인 이름에 의존하게 수정하지 않는다. JSON은 서버 시작 시 검증하므로 변경 후 [재빌드/재시작](QUICKSTART.md)이 필요하다. 기존 필드/상태와 호환되지 않는 변경은 [migration/변경 대응](CHANGE-CONTROL.md)을 함께 정한다.

형식 예제는 단순 역할 승인이다. 회사 격리·자기 승인 금지·단계별 승인자 분리까지 자동으로 적용되지 않는다. 해당 요구에는 [조직 정책](ENTERPRISE-GUIDE.md) 또는 별도 어댑터를 연결한다. readOnly는 생성 입력 보호이며 읽기 비공개가 아니다.

## 3. 계산과 정책 연결

FdeExtension은 Spring bean의 고유 id를 Action.extension에 연결한다. `changes(current,input)`은 변경할 필드 맵을, `apply`는 변경 맵과 선언된 다음 상태를 반환한다. 공통 엔진이 반환 값을 다시 검증하고 객체/이력을 같은 트랜잭션에 저장한다.

FdePolicy는 SQL scope·생성 메타데이터·업무 키·검색 색인·동작·출력 필드를 함께 구현한다. actor는 서버 인증 컨텍스트에서 구한다. `visibleFields(type,null)`은 CSV 열 후보에도 쓰므로 null을 처리해야 한다. enterprise와 필드/상태/동작 계약이 다르면 policy id만 붙이지 말고 어댑터를 구현한다.

## 4. 실행과 완료 확인

입력→저장/계산→동작→결과 한 흐름을 실제로 실행한다. 관련 정상/실패/권한/상태/고유 키/오래된 version·공유 예산 사례를 요구에 맞춰 검사한다. WORK에 요구→수정 위치→입력/예상값→실제 결과를 기록하고 미실행을 통과로 표시하지 않는다. 제출 실행은 [PREVIEW](PREVIEW.md), 검사 명령은 [VALIDATION](VALIDATION.md)을 따른다.
