# 최소 앱에서 필요한 모듈만 추가

이 저장소는 참고 원본이다. 실제 결과물은 새 `solution` 폴더에 생성한다. 기본 앱에는 Spring·React·상태 확인 API만 있고 로그인·DB·업무 페이지·예제 데이터가 없다. 선택하지 않은 모듈의 소스·API·화면·직접 의존성을 복사하지 않는다. 메뉴만 숨기는 방식이 아니다.

## 처음에는 최소 구성

허용된 작업공간에서 실행한다. 참고 원본은 제출 대상 밖의 허용된 경로(예: `/tmp/fde-reference-kit`)에 두고 결과물은 실제 작업공간의 `solution`에 만든다. 경로는 예시이며 플랫폼의 허용/제출 범위를 따른다. 기존 제공 프로젝트를 덮어쓰지 않는다. 생성 도구에는 Python 3.10+가 필요하다.

```bash
git clone --depth 1 --branch codex/public-starter https://github.com/Kyuwon-Choi/spring-workflow-starter.git /tmp/fde-reference-kit
python3 /tmp/fde-reference-kit/scripts/create-solution.py solution
cd solution
./scripts/build.sh
./scripts/preview.sh 8080
```

생성/추가 도구는 네트워크에 접속하거나 의존성을 설치하지 않는다. build.sh가 wrapper로 설치·프론트 타입 검사/빌드·Java 패키징을 수행한다. JDK 21과 Bash가 있어야 하며 Maven·Node/npm은 wrapper로 준비한다. 최소 앱에 OpenSSL·Docker·외부 DB·AI 키는 필요 없다. 상세 [설치 의존성](DEPENDENCIES.md).

미리보기는 서버 로그의 실제 포트와 `/`를 입력한다. 업무 모듈을 추가하면 `/fde`, 조직 모듈은 `/organization`을 사용할 수 있다. 서버 터미널을 유지한다. 기본 소스 파일은 24개이며 설치 산출물(node_modules, target 등)을 제외한 수치다.

## 모듈 선택

| 모듈 | 가져오는 기능 | 자동으로 필요한 모듈 |
|---|---|---|
| 선택 없음 | Spring 웹 API + React 진입 화면 + /api/health | 없음 |
| auth | 로그인·시스템 역할·서버 인증·서명/만료 검사 | 없음 |
| workflow | 객체/참조·정의 기반 폼/목록·상태 동작·범위 권한·충돌·이력·파일 DB | auth |
| csv | CSV 검증 미리보기·일괄 입력·내보내기 | workflow, auth |
| enterprise | 회사·부서·프로젝트·실시간 소속·위임·시행일 규칙·다단계 승인·공유 예산 | workflow, auth |
| stock-adjustment | 수량 증감 계산의 작은 확장 | workflow, auth |

선택 결과와 직접/빌드 의존성은 생성된 `solution-modules.json`, `DEPENDENCIES.md`에 남는다. 프론트 lock 파일도 선택 조합에 맞게 복사한다. 샘플 회원가입/메일/관리/장비/은행 계좌 페이지와 다른 도메인은 가져오지 않는다. 이 모듈들은 기존 전체 참고 앱의 기능을 분리한 것이며 기존 JHipster 계정 API 전체를 유지하는 배포판은 아니다.

Dockerfile이 필요하면 생성 명령에 `--docker`, 기존 생성 앱에는 `--add --docker`를 사용한다. Dockerfile/.dockerignore/container.sh 세 파일만 선택 추가하며 앱의 직접 의존성은 바꾸지 않는다. [컨테이너 실행과 검증 범위](CONTAINER.md)를 따른다. 참고용 CSV 점검·검증 기록 대조·계산 예제·세션 가이드는 [문서 목차](README.md)에서 필요할 때 사용하며 기본 앱에는 복사하지 않는다.

## 요구사항 확인 뒤 추가

모든 명령은 실제 작업공간에서 실행하는 예다. 참고 원본 경로는 `/tmp/fde-reference-kit`로 가정한다.

```bash
# 기존 최소 앱에 로그인만 추가
python3 /tmp/fde-reference-kit/scripts/create-solution.py solution --add --modules auth

# 실제 요구에 맞춘 신뢰된 업무 정의가 준비된 경우
python3 /tmp/fde-reference-kit/scripts/create-solution.py solution --add --modules workflow --domain-file ./task-domain.json

# 필요할 때만 CSV 추가
python3 /tmp/fde-reference-kit/scripts/create-solution.py solution --add --modules csv

# 조직 권한/조건부 결재가 필요할 때 추가
python3 /tmp/fde-reference-kit/scripts/create-solution.py solution --add --modules enterprise
```

업무 엔진에는 업무 정의 한 개를 명시해야 한다. `--domain-file`은 제공 받은 자료를 무조건 실행하는 옵션이 아니라, 요구를 확인하고 작성한 JSON 정의를 연결하는 옵션이다. 정의 형식은 [DOMAIN-REFERENCE](DOMAIN-REFERENCE.md)를 참고한다. 코드 패키지는 `com.example.solution`, 프론트는 `src/main/webapp/features`다. 새로운 계산/정책 bean도 이 패키지 아래에 추가한다.

처음부터 필요한 모듈이 정해졌다면 한 번에 생성할 수 있다.

```bash
python3 /tmp/fde-reference-kit/scripts/create-solution.py solution --modules csv,enterprise --domain-file ./task-domain.json
```

정의에서 enterprise policy나 stock-adjustment 확장을 사용하면 해당 모듈도 자동 선택되며 결과에 표시된다. 다른 사용자 정의 확장은 별도로 구현해야 한다. `--dry-run`을 붙이면 폴더를 만들거나 수정하지 않고 선택 결과를 확인한다.

## 정의 형식을 먼저 확인할 경우

```bash
python3 /tmp/fde-reference-kit/scripts/create-solution.py solution --modules workflow --domain support
```

이는 support 예제를 **명시적으로 선택**한 경우에만 그 JSON 하나를 가져온다. 선택지는 support/procurement/inventory/enterprise다. 기본 앱에 예제가 자동 포함되지는 않는다. 예제는 요구에 맞게 변경하고 원본 결과물 전체를 과제 풀이로 대체하지 않는다. `--domain inventory`는 수량 계산 확장도 포함한다. enterprise 예제에는 계정/조직/요청 데이터가 자동 생성되지 않는다.

## 이미 구현한 코드 보호

`--add`는 이전 생성 기록과 현재 파일을 대조한다. 모듈 추가와 관계없는 화면·업무 코드·업무 정의의 사용자 수정은 유지한다. 추가 과정에서 함께 바뀌어야 하는 pom/package/config/진입 목록/실행 문서 등을 사용자가 이미 수정했다면 **전체 쓰기 전에 중단**한다. 기존 폴더에 새로 생성하는 명령도 거부한다. 강제 덮어쓰기 옵션은 없다.

충돌 시에는 Codex에게 실제 diff를 보여주고 필요한 변경을 수동 병합한다. 단순히 파일을 지우거나 생성 기록의 hash를 바꾸어 검사를 우회하지 않는다. 이후 추가 모듈 적용도 실제 파일과 변경점을 대조한다. 모듈 제거/자동 업그레이드는 이 도구의 범위가 아니다.

모듈 추가 후에는 실행 중인 생성 앱 서버를 종료하고 다시 `./scripts/build.sh`, `./scripts/preview.sh`로 빌드/실행한다. DB는 `.local-data`에 남으며 임의 초기화하지 않는다. 의존성이 그대로인 소스 변경이면 프론트 빌드/Java 패키징 등 필요한 부분만 실행할 수 있다.

## auth와 enterprise 설정

preview.sh는 미리보기 패널에 넣을 수 있도록 app.preview=true로 실행한다. 이 모드에서는 프레임 삽입을 허용하며, 일반 실행의 기본 프레임 보호와 구분한다.

auth 기본 확인 계정은 admin/admin, user/user다. `APP_ADMIN_PASSWORD`, `APP_USER_PASSWORD`로 비밀번호를 바꾸거나 생성 앱의 `application.properties`에서 `app.auth.users`를 수정한다. 추가 계정은 같은 파일에 연속된 인덱스로 작성한다. 명령행/환경 설정으로 사용자 목록 자체를 덮어쓸 때에는 전체 목록을 제공해야 한다. 비밀번호나 Authorization 값을 로그/WORK에 남기지 않는다.

auth는 메모리 설정 계정 + 30분 JWT Bearer다. 쿠키/Basic/폼 세션 인증을 사용하지 않고 Authorization 헤더로만 인증하므로 CSRF 쿠키에 의존하지 않는다. 서명 키는 .local-data/jwt-secret에 생성하며 외부로 전송하지 않는다. 프론트는 인증 헤더를 메모리에만 보관하고 새로고침하면 다시 로그인한다. 외부 서비스 운영에는 TLS와 계정/비밀 관리 정책을 별도로 적용해야 한다. 시스템 역할은 토큰 발급 시점의 값이므로 계정/역할 변경의 즉시 토큰 폐기는 구현하지 않았다. 조직 업무 소속/위임은 요청마다 DB에서 확인한다. 토큰 만료 시 로그아웃 후 다시 로그인한다. SSO/계정 CRUD/토큰 즉시 폐기가 필수라면 해당 요구에 맞춰 확장하거나 제공 프로젝트의 인증을 유지한다.

enterprise는 admin으로 `/organization`에서 조직을 구성하고 프로젝트를 만든 뒤, 설정에 실제 존재하는 계정에 업무 역할과 프로젝트를 부여한다. 부서/재무 승인자는 서로 다른 계정을 준비한다. 계정 설정 변경은 재시작이 필요하며 업무 소속·위임 등은 DB의 실시간 정책이다. [조직 정책](ENTERPRISE-GUIDE.md)의 기존 데모 실행기/계정은 전체 참고 앱용이므로 생성 앱에 그대로 적용하지 않는다.

## 결과물 경계와 검증

실제 제출 대상 경로는 플랫폼 안내를 따른다. `solution` 폴더만 만들었다고 참고 원본/다른 폴더가 자동 제외되는 것은 아니며 `.gitignore`가 플랫폼 제출 범위를 정하지도 않는다. 필수 README·입력 파일 등 제공 자료는 보존한다.

선택 모듈의 코드만 복사했더라도 과제에 맞는 필드·규칙·권한·첫 화면·설명으로 바꿔야 한다. 실행 가능성은 생성 성공과 별개이며 추가 후 실제 빌드·첫 입력→결과·관련 실패/권한을 검사한다. 검사 범위는 [VALIDATION](VALIDATION.md)을 본다.
