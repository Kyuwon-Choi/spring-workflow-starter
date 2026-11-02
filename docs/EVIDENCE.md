# 제출 전 요구와 검증 근거 대조

`scripts/verify-evidence.py`는 Python 3.10+ 표준 라이브러리로 **기록의 누락·버전·파일 무결성**을 확인한다. 테스트나 명령을 대신 실행하지 않고, 테스트 내용의 충분성이나 업무 정답·사용자의 실제 관찰을 증명하지 않는다. 자동 채점기가 아니다.

실제 공식 요구에서 필수 ID 목록을 정한다. [REQUIRED 양식](templates/REQUIRED.json)과 [EVIDENCE 양식](templates/EVIDENCE.json)은 미실행 상태이며 그대로 검사하면 통과하지 않는다. 제공 파일은 덮어쓰지 않는다.

```bash
python3 scripts/verify-evidence.py --requirements /실제/required.json --evidence /실제/evidence.json --root /실제/과제폴더 --out /실제/기록/evidence-report.json
```

`required.json`은 `{"required": ["R1", "R2"]}`처럼 공식 필수 요구를 누락 없이 적는다. `evidence.json`은 `schema_version: 1`, `requirements` 목록을 가지며 각 항목은 다음 구조다. 값은 실제 근거로 채운다.

| 필드 | 의미 |
|---|---|
| id / status | 필수 ID, pass/partial/fail/not_run/skip |
| sources | 해당 결과에 영향을 주는 코드·설정·테스트·입력 파일의 상대 경로→현재 SHA-256 |
| checks | 이 요구의 실제 검사 목록 |
| checks[].kind / status | automated 또는 manual, pass/fail/skip/not_run |
| expected / observed | 독립 기대값과 실제 관찰 결과의 비어 있지 않은 설명 |
| artifact / sha256 | 실제 로그·결과·관찰 기록의 상대 경로와 SHA-256 |
| command / exit_code | 자동 검사의 실제 실행 명령과 정수 종료 코드 |
| counts | 자동 검사의 executed/passed/failed/skipped 정수 개수 |

`executed`는 passed+failed이며 skip은 실제 실행에 포함하지 않는다. 자동 검사가 0건이거나 전체 skip이면 종료 코드가 0이어도 근거가 부족하다. 수동 확인은 실제 관찰 기록을 연결하고 가짜 테스트 개수를 만들지 않는다. 경로는 root 안으로 제한하며 파일 내용·비밀번호·실제 입력 값은 출력하지 않고 요구 ID와 문제 코드만 보고한다.

누락·미실행·부분 충족·skip·빈 검사·근거 없음·현재 소스와 다른 해시는 `INCOMPLETE`(exit 2), 명시적 실패·0이 아닌 종료 코드·변조된 결과 파일·잘못된 입력은 `FAIL`(exit 1)이다. 모두 연결됐으면 `PASS`(exit 0)지만 이는 **제공한 기록에 대한 대조**다. 적지 않은 필수 요구나 sources에서 빠진 관련 파일, 거짓 기록, 잘못된 기대값을 도구가 찾아준다고 주장하지 않는다. 코드를 바꾼 뒤 이전 결과를 새 해시로만 갱신하지 말고 영향을 받는 검사를 실제로 다시 실행한다.

Codex 요청 예:

```text
공식 필수 요구와 WORK를 대조해 required.json의 ID를 정리해줘.
실제로 실행한 관련 검사와 내가 전달한 화면 관찰만 evidence.json에 연결해줘.
관련 소스·설정·입력과 실제 결과 파일의 SHA-256을 기록하고,
미실행/실패/skip은 그대로 남겨줘. 명령과 개수도 실제 결과에서 가져와줘.
verify-evidence.py로 기록을 대조하고 부족한 항목을 알려줘.
실행 없이 상태나 해시만 바꿔서 통과시키지 마.
```
