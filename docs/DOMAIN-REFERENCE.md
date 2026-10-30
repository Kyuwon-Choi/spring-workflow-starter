# 정의와 API 참고

실제 계약의 기준은 FdeSchema, FdeRegistry, FdeResource, FdeService다. JSON은 저장소에 포함하는 신뢰된 코드이며 서버 시작 시 읽는다. 임의 스키마·SQL·스크립트를 요청으로 실행하지 않는다.

## 도메인·타입·필드

| 항목 | 의미 |
|---|---|
| Domain.id / label / types | 도메인 ID, 표시 이름, 타입 맵 |
| ObjectType.labelField | 객체 표시용 필드명 |
| keyField | 필수·생성 입력 가능한 필드 하나를 고유 키로 사용. 없으면 null |
| fields | 최대 40필드. string/integer/decimal/boolean/date/enum/reference |
| states / initialState | 선언된 상태 목록과 초기 상태 |
| readRoles / createRoles | 인증된 사용자의 시스템 역할 중 하나와 일치해야 함 |
| readScope | ALL 또는 OWN(생성자) |
| managerRoles | 기본 OWN 범위를 넓히는 시스템 역할 |
| actions | 동작 ID→Action 정의 |
| policy | 선택형 FdePolicy id. 생략/null이면 기본 정책 |

도메인/타입/동작 ID는 소문자 시작의 소문자·숫자·하이픈(최대 64자), 필드명은 소문자 시작 영문·숫자·밑줄(최대 40자)이다. 상태는 대문자 시작 대문자·숫자·밑줄(최대 64자)이다.

Field는 label/type/required와 선택 maxLength/min/max/options/reference/readOnly를 갖는다. date는 ISO 날짜 문자열, reference는 **같은 도메인**의 대상 타입 ID와 해당 객체의 양의 정수 ID를 사용한다. API 숫자는 JSON number, boolean은 JSON true/false로 전달한다. 숫자 문자열을 API에 보내지 않는다. CSV 문자열은 서버가 형변환한다.

readOnly:true는 생성 API/CSV/폼 입력을 막고 required:false여야 한다. 신뢰된 writes/확장 계산은 기록할 수 있다. 읽기 비공개는 FdePolicy 출력 정책으로 별도 구현한다. 기본 문자열 상한은 500자, 명시 maxLength는 최대 4000자다. 숫자는 precision 18/scale 6 이내다.

## 동작과 권한

| Action | 의미 |
|---|---|
| from / target | 실행 가능 상태와 기본 도착 상태 |
| roles | 허용 시스템 역할 |
| scope | ALL / OWN(관리 역할 예외 포함) / OWNER_ONLY(관리 역할 예외 없음) |
| inputs | 동작별 입력 필드 정의 |
| writes | 저장 속성명→입력 필드명 |
| requires | 현재 객체에서 값이 있어야 하는 필드명 목록. 조건식 언어가 아님 |
| extension | 선택형 신뢰된 Java FdeExtension id |

OWNER_ONLY는 “본인만 동작”이고 “본인 승인 금지”는 아니다. 후자는 업무 정책이다. availableActions는 UI 후보이며 매 요청에서 서버가 현재 상태/역할/정책을 다시 확인한다. 업무 ORG_ADMIN/MANAGER/FINANCE/STAFF는 시스템 ROLE_ADMIN/ROLE_USER와 다르다.

## 공통 API

모든 경로는 인증이 필요하다. `{domain}`과 `{type}`은 정의의 ID, `{id}`는 서버 객체 ID다.

| 메서드·경로 (/api/fde 뒤) | 용도 |
|---|---|
| GET /catalog | 현재 계정이 발견 가능한 정의 |
| GET /{domain}/{type}/objects | state, q, page(0부터), size로 조회 |
| GET /{domain}/{type}/summary | 현재 접근 범위의 상태별 건수. 목록의 q/state 필터를 받는 API는 아님 |
| GET /{domain}/{type}/objects/{id} | 단건·version·availableActions |
| POST /{domain}/{type}/objects | 생성 |
| POST /{domain}/{type}/objects/{id}/actions/{action} | 버전 포함 상태 동작 |
| GET /{domain}/{type}/objects/{id}/history | 최근 200개 변경 이력 |
| POST /{domain}/{type}/import/preview | CSV를 검사하고 타입 변환한 행/오류 반환 |
| POST /{domain}/{type}/import | 1~100개 행을 생성, 한 행 실패 시 전체 롤백 |
| GET /{domain}/{type}/export | q/state 필터와 권한을 적용한 CSV |

생성 본문 예시(위 실습 정의를 등록한 뒤):

```json
{"data":{"ticketNo":"REQ-001","title":"자료 검토 요청","estimatedCost":10000}}
```

제출 본문 예시:

```json
{"expectedVersion":0,"data":{}}
```

expectedVersion은 예시 값을 고정하지 말고 **가장 최근 응답 version**을 사용한다. 객체 응답에는 id/domain/objectType/data/state/owner/version/createdAt/availableActions가 있다. state/owner/version은 생성 data에 넣지 않는다. 정의 외 필드와 알 수 없는 envelope 필드는 거부한다. 임의 PATCH/DELETE API나 CSV upsert는 제공하지 않는다.

## CSV와 오류

preview 본문은 `{"csv":"열이름,...\n값,..."}`, 응답은 valid/rows/errors이며 오류는 row/detail로 나타낸다. 저장은 `{"rows":[{"필드":"값"}]}` 형태의 타입 변환된 생성 데이터다. preview는 예약/잠금이 아니므로 import에서 다시 검증한다. reference 열에는 이름 대신 ID를 넣는다. 필수·고유 키·readOnly 규칙은 API와 같다.

CSV 입력은 **100000자**와 100행 제한이다(정확한 바이트 용량 제한과 다름). 목록 size 최대 100, 검색 q 최대 120자, 객체 JSON 최대 32000자, export 최대 1000건이다. 내보내기는 정의된 업무 필드만 포함하고 별도 state/owner/version 열은 없다. 민감 값은 정책에 따라 가리며 수식처럼 해석될 CSV 값은 보호한다.

400은 입력/타입 오류, 401은 인증 실패, 403은 역할/업무 정책 거부, 404는 없는 객체 또는 접근 불가, 409는 상태/버전/고유 키/예산 등 충돌일 수 있다. 모든 권한 거부가 403인 것은 아니다. 실패 응답과 실제 데이터 불변을 함께 확인하고 자동 재시도 전에 원인을 구분한다.

조직 설정 API/역할/시각 규칙은 [조직 업무](ENTERPRISE-GUIDE.md)를 따른다. 외부 API 도구를 사용한다면 발급 토큰·실제 자격 증명을 소스나 로그에 기록하지 않는다.
