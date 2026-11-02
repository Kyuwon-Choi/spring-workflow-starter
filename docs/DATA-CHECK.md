# 여러 CSV의 데이터 계약 확인

`scripts/check-data.py`는 Python 3.10+ 표준 라이브러리만 사용한다. 생성 앱에 자동 포함되지 않는 참고 도구다. 먼저 자료의 구조를 보고, 확인한 요구와 원본 설명으로 계약을 작성한다.

```bash
# UTF-8/BOM CSV의 헤더·행 수·빈 셀 수만 출력; 업무 검증 없음(exit 2)
python3 scripts/check-data.py --profile /실제/자료.csv
# 포함된 작은 예제(exit 0)
python3 scripts/check-data.py --contract recipes/data-check/contract.json
# 실제 계약과 원본 폴더, 출력 파일
python3 scripts/check-data.py --contract /실제/data-contract.json --root /실제/자료폴더 --out /실제/기록/data-report.json
```

전체 형식은 [예제 계약](../recipes/data-check/contract.json)을 본다. `schema_version: 1`, 비어 있지 않은 `files`, 파일별 `path`와 `columns`가 필요하다. `path`는 root 아래 상대 경로만 허용하며 `..`/외부 symlink를 거부한다. 인코딩은 기본 `utf-8-sig`, 필요하면 파일별 `encoding`으로 실제 인코딩을 지정한다.

열 설정은 `type`(string/integer/number/datetime), `nullable`(기본 false), 숫자 `minimum`/`maximum`, datetime의 `timezone: required`, 선언용 `unit`을 지원한다. 선언한 열은 반드시 있어야 하며 추가 열은 허용한다. 빈 문자열/공백만 있는 셀은 null로 검사한다. 숫자는 유한값만 허용하고 날짜시간은 ISO 형식을 검사한다. 단위의 의미·시간창·업무상 용량 초과 같은 조건을 추론하거나 검증하지 않는다. `unit`은 선언을 기록할 뿐 원본 숫자가 그 단위로 측정됐는지 증명하지 않는다.

`primary_key`는 선언한 열들의 고유·비어 있지 않은 조합을 검사한다. `foreign_keys`는 파일/열 조합의 존재와 참조 대상의 유일성을 확인한다. 키는 원문 문자열로 비교하므로 `01`과 `1`, 앞뒤 공백을 자동 통합하지 않는다. 선택한 모든 FK 열이 nullable이고 값이 모두 비었을 때만 빈 참조를 허용한다. 기본은 헤더만 있는 데이터도 실패하며 실제 요구가 허용하면 `allow_empty: true`로 명시한다.

기본 최대 100,000행/파일까지 메모리에 읽어 중복·참조를 확인한다. `--max-rows`로 조정할 수 있지만 잘린 데이터는 통과로 쓰지 않는다. 오류는 최대 100개 위치를 출력하고 전체 오류 수를 별도로 기록한다. 원본 셀 값은 출력하지 않는다. 원본을 바꾸거나 DB에 적재하지 않는다.

종료 코드는 `0=선언한 계약 통과`, `1=파일/형식/계약 위반`, `2=구조 요약만 수행 또는 행 제한으로 미완료`다. 구조 확인이나 이 도구의 PASS를 전체 업무 정답으로 바꾸지 않는다. 어떤 파일이 원본인지, 거리 방향·시간대·단위와 잘못된 행의 처리 정책은 질문/합의로 확정해 구현과 검증에 연결한다.
