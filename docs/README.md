# 사용 문서 목차

저장소 최상위가 모든 실행 명령의 기준이다. 최초 사용은 1~3 순서로 읽고, 업무 요구에 해당하는 문서로 이동한다.

1. [빠른 시작](QUICKSTART.md): 설치, 기본/조직 데모, 재빌드, 데이터 보존.
2. [작업 절차](WORKFLOW.md): 담당자 질문, 요구별 완료 조건, 계획, 구현 지시, 검증, 설명.
3. [업무 적용 실습](ADAPTATION.md): JSON 정의를 새 업무로 바꾸고 실제 결과 확인.

| 필요한 내용 | 문서 |
|---|---|
| 객체·필드·관계·동작·API의 정확한 형식 | [계약/API](DOMAIN-REFERENCE.md) |
| SQL 범위·조직·위임·다단계 승인·공유 예산 | [조직 업무](ENTERPRISE-GUIDE.md) |
| 진행 중 규칙 변경·과거 건 처리·migration | [변경 대응](CHANGE-CONTROL.md) |
| 공통 엔진/정의/확장 경계 | [FDE 구조](FDE-GUIDE.md) |
| 유형별 다른 출발점과 고정 소스 | [레포 카드](REPO-CATALOG.md) |
| 권한·CSV·민감 필드·원자성 검사 | [보안](SECURITY.md) |
| 실제 실행 수치·명령·남은 한계 | [검증](VALIDATION.md) |
| 로딩 실패·로그인·메뉴·409·DB 잠금 | [오류 해결](TROUBLESHOOTING.md) |
| 1시간 연습과 전체 시뮬레이션 | [리허설](REHEARSAL.md) |
| 준비물 사용 허용·제출·사후 설명 | [평가 환경 적용](ASSESSMENT.md) |
| 라이선스·공개 범위·원격 연결·push 전 확인 | [GitHub 게시](GITHUB-PUBLISH.md) |
| 바로 쓸 AI 지시문 | [단계별 카드](PROMPTS.md) |
| 요구·계획·검증·인수인계 기록 | [WORK 템플릿](templates/WORK.md) |
| 선택형 질문/계획 skill 설치 | [skills 안내](../skills/README.md) |

기본 업무는 `support`, `procurement`, `inventory`다. `enterprise`는 현재 업무 소속이 있는 계정에만 노출되며, 조직 데모 실행기가 합성 소속을 만든다. `training`/`events`/`tenant-key-test`는 테스트용이며 기본 UI 메뉴의 운영 정의가 아니다.
