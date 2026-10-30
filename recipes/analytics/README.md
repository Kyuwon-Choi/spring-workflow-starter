# CSV 분석 경로

Python과 DuckDB를 사용하는 로컬 입력 → 품질 검사 → 집계 → JSON/CSV 출력 경로다. Spring/JDK가 필요하지 않은 분석 과제의 별도 출발점이다. Python 3.13 / DuckDB 1.4.1에서 4개 테스트와 예제 CLI 실행을 확인했다. 네트워크 connector·AI 공급자를 설정하지 않았다.

```bash
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
.venv/bin/python -m unittest -v
.venv/bin/python analyze.py example.csv --group category --measure value --out result
```

실제 데이터의 그룹 열·수치 열을 인자로 바꾸고 요구된 계산에 맞게 SQL/검증을 수정한다. 임의 SQL 문자열을 API에서 실행하는 구조로 바꾸지 않는다. 숫자·그룹 누락을 거절하고 CSV 출력의 수식 시작 문자를 처리한다. 이 버전은 배치 집계이며 데이터 저장 UI·인증·다중 조직 격리는 제공하지 않는다.

출처: [DuckDB](https://github.com/duckdb/duckdb), MIT. 분석 스크립트는 이 키트에서 작성한 예제다. 외부 데이터·문제·결과물의 전송은 하지 않는다.
