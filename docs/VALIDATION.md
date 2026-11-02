# 검증 명령과 확인 범위

## 선택형 실행·인계·데이터·검증 보완 · 2026-11-02

- `scripts/tests` 39개 통과: 기존 생성/설치 보호, Docker 선택·추가/유지·이전 상태 호환, helper 인자/중단, CSV 다중 파일/형식/키·참조/미완료, 검증 근거의 누락·0건/skip·해시/현재 버전·실패, 원본 덮어쓰기 방지.
- `recipes/core-validation` 5개 통과: 손계산 기대값, 기준선의 비최적 사례, 탐색 실패와 작은 모델의 불가능 증명 구분, 관련 제약/입력 경계.
- CSV 예제의 선언된 계약 통과. 실제 5개 계산 테스트의 실행 로그·코드 해시를 요구 기록에 연결해 검증 기록 대조 CLI 성공 확인. 미실행 양식/구조 요약만 한 실행은 exit 2, Docker CLI가 없을 때 helper는 미실행으로 종료함을 확인.
- `--docker` 기본 앱은 27개 원본 파일. 별도 생성한 앱을 호스트 Java 21에서 npm/타입/Vite/Maven 빌드하고 0.0.0.0으로 실행해 health·첫 화면·JS 자산 3 HTTP 요청을 확인했다. 기존 다운로드 캐시를 사용했으며 임시 서버는 종료했다.
- Docker helper 테스트는 가짜 CLI로 명령 연결만 검사했다. **이 Mac에 Docker CLI/daemon이 없어 실제 이미지 빌드·컨테이너 실행·컨테이너 UID/volume 권한은 미검증**이다. 호스트 성공을 Docker 성공으로 해석하지 않는다. [실제 Docker 확인 절차](CONTAINER.md).
- 세션 인계는 운영 가이드/양식이며 새 멀티 세션 실행 검증이 아니다. CSV PASS는 명시한 데이터 계약, evidence PASS는 제공한 기록의 무결성/누락 대조이며 업무 정답이나 제출 성공을 보장하지 않는다. 그래프 레시피는 추가하지 않았다.

```bash
python3 -m unittest discover -s scripts/tests -v
python3 -m unittest discover -s recipes/core-validation -v
python3 scripts/check-data.py --contract recipes/data-check/contract.json
```

아래 2026-10-31의 6개 모듈 조합/HTTP 기록은 당시 검증이다. 이번 변경에서 전체 앱 검사를 반복한 것으로 표시하지 않는다.

## 모듈형 생성 앱 · 2026-10-31

기존 전체 참고 앱의 검사와 별개로 다음을 확인했다. 파일 수는 생성 원본만 세며 node_modules/target/.local-data 등 실행 산출물은 제외한다.

| 생성 구성 | 원본 파일 수 | 확인 |
|---|---|---|
| 기본 | 24 | Java·React 빌드, health/정적 화면, 인증·업무·예제 API 없음 |
| auth | 31 | JWT 로그인·서명 거부·Basic 거부·키 파일 권한 |
| workflow + support 정의 | 47 | 참조·본인 범위·상태 변경·이력·오래된 version 거부, CSV 미포함 |
| csv + support 정의 | 50 | CSV 미리보기/입력/출력·수식 이스케이프·중복 실패 시 전체 롤백 |
| enterprise + csv + enterprise 정의 | 59 | 소속·자기 승인 차단·부서/재무 승인·버전·공유 예산·회사 격리·민감 필드/이력/검색/CSV 보호 |
| stock-adjustment + inventory 정의 | 48 | 수량 계산과 실패 시 불변 |

- 생성 도구 11개 검사: 기본 분리·선택 의존 관계·업무 파일 보존·충돌 전면 중단·기존 폴더 보호·dry-run·재적용·링크 거부·캐시/비밀 파일 제외.
- 위 6개 조합의 프론트 타입 검사/Vite 빌드와 Maven package 통과. 기본 앱의 npmw 최초 설치도 확인했다. 기존 환경의 다운로드 캐시는 사용했다.
- 실제 서버 HTTP 185요청 통과. 별도 모듈 추가/재시작 검사 7요청에서 기존 H2 데이터·서명 키/토큰·수정 페이지/WORK/정의 보존과 새 조직 테이블 적용 확인.
- 로컬 브라우저에서 선택형 업무 앱 로그인·고객 등록·목록 반영 확인. 공식 평가 환경의 포트 전달/실제 브라우저 제한을 검증한 것은 아니다.

```bash
# 참고 키트 루트: 파일 생성/보존 검사 (네트워크 불필요)
python3 -m unittest discover -s scripts/tests -v

# 생성 앱 폴더: 선택 결과의 실제 빌드/실행
./scripts/build.sh
./scripts/preview.sh 8080
```

Java 21.0.4 / Node 24.21.0 / npm 12.0.2 / Maven 3.9.16 / H2로 확인했다. 모든 모듈/사용자 수정의 조합, 캐시 없는 설치 시간, 공식 환경, SSO/즉시 토큰 폐기·운영 TLS/DB/부하/장애는 별도 검증 대상이다. 선택/추가 후 실제 요구에 맞춰 관련 검증을 수행한다.

## 기존 전체 참고 앱

의존성을 준비한 뒤 변경에 맞는 검사를 실행한다. 프론트 빌드/JAR 생성 전에는 같은 target을 사용하는 서버를 종료한다.

```bash
./scripts/check.sh
./npmw run vitest-run -- --coverage.enabled=false
./mvnw -ntp -Dskip.installnodenpm -Dskip.npm verify
```

check.sh는 관련 백엔드 검사·타입 검사·지정 UI lint·관련 UI 검사·Vite 빌드를 포함한다. JAR에 화면을 포함하려면 Vite 빌드가 먼저 있어야 한다. 새 변경/실패 근거 없이 전체 검사를 반복하지 않는다.

기본 8080 서버를 실행한 뒤 합성 데이터로 실제 API를 확인할 수 있다.

```bash
python3 scripts/fde-smoke.py
```

이 명령은 데이터를 추가한다. [조직 정책](ENTERPRISE-GUIDE.md)은 다른 계정/회사·자기 승인·단계 분리·시행 시각/금액 경계·기존 버전/재제출·위임 취소·민감 출력·공유 예산 경쟁·실패 원자성을 관련 검사로 확인한다.

| 마지막 로컬 확인 | 범위 |
|---|---|
| 2026-10-28 Maven verify | 241개, 실패/오류/skip 0. Surefire 123 + Failsafe 118 |
| 2026-10-28 프론트 | 191개/23파일 통과, coverage disabled |
| 관련 check.sh | 백엔드 75/UI 10. 전체 수치와 중복 |
| 실제 HTTP/브라우저/재시작 | 합성 JWT 59요청, 서로 다른 부서/재무 승인, 데이터/규칙/예산/이력 보존 |
| 2026-10-30 preview.sh | wildcard 리스너·실제 로그 포트·/fde HTTP 200·인증 후 catalog·잘못된 포트 거부 |

Java 21.0.4 / Maven 3.9.16 / Node 24.21.0 / npm 12.0.2 / H2에서 확인했다. 미리보기 실행기 이후 앱 Java/React와 잠금 파일은 그대로다. 문서 정리는 앱 전체 테스트를 다시 실행한 것으로 기록하지 않는다.

공식 제공 환경의 JDK/다운로드/포트 전달·코드 반입 허용, 캐시 없는 설치는 별도 확인 대상이다. 조직 구현은 한 계정 한 회사·평면 부서·명시적 프로젝트다. SSO/전체 JWT 취소·임의 조직 트리·다중 회사·외부 시스템 원자성·운영 DB/부하/장애는 검증하지 않았다. [보안](SECURITY.md)과 [미리보기](PREVIEW.md)의 경계를 따른다.
