# 모듈형 FDE 키트 · Spring Boot + React

이 저장소는 참고 원본이다. 참고 원본은 제출 대상 밖의 허용된 경로에 두고, 결과물만 실제 제출 경로에 배치한다. 실제 과제 앱은 **새 폴더에 최소 구성으로 생성하고 필요한 모듈만 추가**한다. 기본 앱에는 Spring·React·상태 확인 API만 있다. 선택하지 않은 로그인·DB·업무 화면·예제 데이터는 결과물에 복사하지 않는다.

## 최소 앱으로 시작

Python 3.10+는 생성 도구에, JDK 21과 Bash는 앱 빌드/실행에 필요하다. Maven·Node/npm은 wrapper가 준비한다.

```bash
git clone --depth 1 --branch codex/public-starter https://github.com/Kyuwon-Choi/spring-workflow-starter.git /tmp/fde-reference-kit
python3 /tmp/fde-reference-kit/scripts/create-solution.py solution
cd solution
./scripts/build.sh
./scripts/preview.sh 8080
```

미리보기에는 서버 로그의 실제 포트와 `/`를 입력한다. 서버는 0.0.0.0에 바인딩하며 터미널을 유지한다. 기본 생성 원본은 24개 파일이며 설치/빌드 산출물은 제외한 수치다. [전체 생성·추가 방법](docs/MODULAR-STARTER.md) · [설치 의존성](docs/DEPENDENCIES.md).

## 요구에 따라 선택

| 모듈 | 필요한 경우 |
|---|---|
| auth | 로그인·시스템 역할·서버 인증 |
| workflow | 업무 객체·참조·상태 동작·권한·이력·파일 DB |
| csv | CSV 검증/일괄 입력·내보내기 |
| enterprise | 회사·부서·프로젝트·위임·조건부 결재·공유 예산 |
| stock-adjustment | 수량 증감 계산 확장 |

상위 폴더로 돌아와 실제 요구에 맞춘 신뢰된 업무 정의를 연결한다.

```bash
cd ..
python3 /tmp/fde-reference-kit/scripts/create-solution.py solution --add --modules workflow --domain-file ./task-domain.json
# CSV가 필요할 때만:
python3 /tmp/fde-reference-kit/scripts/create-solution.py solution --add --modules csv
```

workflow는 auth를, csv/enterprise는 workflow와 auth를 포함한다. 선택한 정의와 관련 모듈만 복사하고 다른 예제 페이지는 넣지 않는다. 모듈 추가 후 생성 앱을 다시 빌드한다. 이미 수정한 공통 파일과 충돌하면 전체 쓰기를 중단한다. 강제 덮어쓰기나 자동 모듈 제거는 제공하지 않는다.

신규 생성 앱의 인증은 선택형 JWT Bearer·메모리 설정 계정이다. 기존 전체 참고 앱의 회원가입/관리 화면은 가져오지 않는다. 과제가 기존 프로젝트를 주면 그 스택·API·인증을 유지한다. 참고 원본과 결과물의 실제 제출 범위는 플랫폼 안내를 따른다.

## 질문부터 제출까지

[시작부터 제출까지](docs/START-TO-FINISH.md) · [질문·계획·핵심 기능 설명 skill](skills/README.md) · [WORK 양식](docs/templates/WORK.md).

제공 자료와 실제 담당자 답을 요구/완료 조건으로 연결하고, 첫 실제 입력→결과를 만든 뒤 중간 확인·관련 정상/경계/권한/실패 검사를 수행한다. 재사용·직접 구현 범위와 실제 검증·한계를 남긴다.

| 필요한 내용 | 문서 |
|---|---|
| 최소 생성·모듈 추가·기존 코드 보호 | [MODULAR-STARTER](docs/MODULAR-STARTER.md) |
| 직접 준비/자동 설치/선택 의존성 | [DEPENDENCIES](docs/DEPENDENCIES.md) |
| 선택형 Docker 생성·빌드/실행·미검증 범위 | [CONTAINER](docs/CONTAINER.md) |
| 역할별 세션·파일 소유·변경/재접속 인계 | [SESSION-COORDINATION](docs/SESSION-COORDINATION.md) |
| 여러 CSV의 구조·키/참조 계약 점검 | [DATA-CHECK](docs/DATA-CHECK.md) |
| 필수 요구·실제 검증 기록·현재 버전 대조 | [EVIDENCE](docs/EVIDENCE.md) |
| 작은 계산 정답·기준선 검증 예제 | [core-validation](recipes/core-validation/README.md) |
| 전체 사용 문서 | [목차](docs/README.md) |
| JSON·API·CSV 계약 | [DOMAIN-REFERENCE](docs/DOMAIN-REFERENCE.md) |
| 추가 요구·기존 데이터·규칙 적용 시점 | [CHANGE-CONTROL](docs/CHANGE-CONTROL.md) |
| 특수 분석/문서/배정 등 도구 선택 | [선택 기준](docs/REPO-CHOOSER.md) · [20개 카드](docs/REPO-CATALOG.md) |
| 실제 검사와 미검증 범위 | [VALIDATION](docs/VALIDATION.md) |

## 기존 전체 참고 앱

루트의 기존 JHipster 앱과 전체 기능 소스/테스트는 비교·재사용용으로 보존했다. 그 앱의 실행은 [기존 QUICKSTART](docs/QUICKSTART.md), 확장 원리는 [FDE-GUIDE](docs/FDE-GUIDE.md)를 본다. 신규 과제 결과물에 이 저장소 전체를 복사하지 않는다.

공개 자료·사전 코드 사용은 해당 평가 규칙의 범위에서 한다. 실제 과제 데이터/코드/결과물을 외부로 전송하지 않는다. [제공 환경 안내](docs/ASSESSMENT.md).

[원본·고정 커밋](UPSTREAM.json) · [변경 고지](NOTICE-KIT.md) · [Apache-2.0](LICENSE.txt) · [원본 README](UPSTREAM-README.md)
