# 생성 앱의 설치 의존성

새 과제는 [최소 앱 생성](MODULAR-STARTER.md)으로 시작한다. 아래는 생성 앱 기준이며 저장소 루트의 기존 전체 참고 앱 의존성과 다르다. 20개 오픈소스 카드는 후보 목록으로 자동 설치하지 않는다.

## 환경과 자동 다운로드

| 항목 | 준비 방식 |
|---|---|
| Python 3.10+ | 생성/모듈 추가 도구 실행에 필요. 생성 앱 런타임에는 불필요 |
| JDK 21 · Bash | 실행 환경에서 준비. Java 설치 자체는 wrapper가 하지 않음 |
| Git | 공개 원본 Clone에 사용 |
| 다운로드/압축 도구 | Maven wrapper가 curl/wget 또는 Java 다운로드, unzip/tar 경로 사용 |
| Maven 3.9.16 | 복사된 mvnw와 wrapper 설정으로 다운로드 |
| Node 24.21.0 · npm 12.0.2 | npmw와 frontend-maven-plugin 2.0.2로 target/node에 다운로드 |
| Java 라이브러리 | 선택 결과 pom.xml을 Maven이 내려받음 |
| React와 빌드 라이브러리 | 선택 결과 package.json/package-lock.json을 npm ci로 내려받음 |

기본 실행에는 OpenSSL·Docker·별도 DB·AI 키를 요구하지 않는다. 설치에는 허용된 다운로드 네트워크와 저장 공간이 필요하다. 캐시 없는 최초 설치 시간과 공식 평가 환경 호환성은 측정/검증하지 않았다.

## 선택에 따라 달라지는 직접 의존성

| 선택 | Java 직접 추가 | 프론트 실행 추가 |
|---|---|---|
| 기본 | Spring Boot 4.1.1의 web, validation starter | react 19.3.0, react-dom 19.3.0 |
| auth | spring-boot-starter-security, spring-boot-starter-oauth2-resource-server | axios 1.20.0 |
| workflow | spring-boot-starter-data-jpa, spring-boot-starter-liquibase, H2 | bootstrap 5.3.8; auth도 자동 포함 |
| csv | commons-csv 1.14.1 | 추가 없음; workflow/auth 포함 |
| enterprise | workflow 위에 새 직접 의존성 없음 | 추가 없음 |
| stock-adjustment | workflow 위에 새 직접 의존성 없음 | 추가 없음 |

버전을 별도 기재하지 않은 Java 항목은 Spring Boot 4.1.1 부모의 dependency management를 따른다. 전체 간접 의존성은 Maven 해석 결과와 npm lock을 구분해 확인한다. `modules/catalog.json`이 모듈 간 의존 관계와 직접 추가 목록을 관리한다. 생성 결과의 `DEPENDENCIES.md`에는 실제 선택된 직접 목록이 작성된다.

프론트 빌드에는 TypeScript 6.0.3, Vite 8.3.0, @vitejs/plugin-react 6.1.1과 Node/React 타입 선언이 필요하다. devDependencies라고 제거하면 빌드가 깨질 수 있으므로 `npm ci --omit=dev`를 기본 설치로 사용하지 않는다.

## 설치 명령

생성 앱 폴더에서:

```bash
java -version
javac -version
./scripts/build.sh
./scripts/preview.sh 8080
```

build.sh는 npm ci → 프론트 타입 검사/빌드 → Maven package 순서다. 라이브러리는 그대로인 소스 변경에서는 필요한 빌드만 반복한다. 선택 모듈의 라이브러리 추가/변경은 manifest와 lock을 같이 유지하고 실제 검증 결과를 남긴다.

## 기능이 불필요해졌을 때

처음부터 선택하지 않으면 그 모듈의 소스·라우트·직접 의존성이 생성되지 않는다. 이미 추가한 기능을 제거할 때는 메뉴만 숨기거나 패키지부터 삭제하지 말고 API·서비스·설정·DB 변경 이력·화면·참조를 함께 확인한다. 현재 생성기는 추가와 충돌 보호만 지원하며 기존 앱의 자동 제거/축소 기능은 제공하지 않는다.

새 외부 도구는 [선택 카드](REPO-CHOOSER.md)와 [검증 범위/고정 버전](REPO-CATALOG.md)을 보고 요구가 있을 때만 추가한다.
