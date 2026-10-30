# 검증 명령과 확인 범위

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
