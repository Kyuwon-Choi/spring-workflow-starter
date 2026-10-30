# 설치와 실행

이 문서의 명령은 저장소 최상위 터미널에서 실행한다. 검증된 경로는 macOS의 bash/zsh와 Java 21이다. Windows는 별도로 검증하지 않았다.

## 1. 실행 조건

```bash
java -version
openssl version
./mvnw -v
```

Java 21+가 필요하며 실제 검증 버전은 README에 기록했다. Java 설치 자체는 wrapper가 하지 않는다. Maven과 Node/npm은 프로젝트 wrapper가 준비한다. 첫 설치에는 인터넷과 다운로드 공간이 필요하고, 캐시 없는 소요 시간은 측정하지 않았다. Docker/MySQL/AI 키는 기본 연습 경로에 필요 없다.

## 2. 의존성과 프론트 준비

```bash
./npmw ci --no-audit --no-fund
./npmw run webapp:build:dev
```

이후 기본 앱을 시작한다.

```bash
./scripts/dev.sh
```

서버 준비 메시지가 나온 뒤 <http://127.0.0.1:8080/fde>를 연다. 일반 계정 `user/user`, 관리자 `admin/admin`. 기본 고객 문의·구매·재고를 확인한다. `/kit`는 예전 장비 예제이며 새 도메인 구현의 기준은 `/fde`다. 종료는 실행한 터미널에서 Ctrl+C다.

## 3. 조직 정책 데모

JAR 빌드 전에 실행 중인 이 프로젝트 서버를 종료한다. 같은 target을 쓰는 서버 실행 중에는 빌드 결과를 덮어쓰지 않는다.

```bash
./mvnw -ntp -Dskip.installnodenpm -Dskip.npm verify
./scripts/enterprise-demo.sh
```

<http://127.0.0.1:8082/organization>와 <http://127.0.0.1:8082/fde>를 연다. `admin`으로 설정하고 `user`→`fde-manager`→`fde-finance`로 역할을 바꿔 승인한다. 암호와 전체 순서는 [조직 업무](ENTERPRISE-GUIDE.md)에 있다. 이 실행기만 합성 조직 계정을 생성하며 기존 구성은 덮어쓰지 않는다.

빌드가 끝난 같은 JAR로 두 데모를 별도 터미널에서 동시에 실행할 수 있다.

```bash
./scripts/run-jar.sh
```

기본 실행은 8080, 조직 실행은 8082다. 실행 중에는 다시 빌드하지 않는다.

## 4. 개발 변경 반영

| 바꾼 것 | 반영 방법 |
|---|---|
| Java/도메인 JSON, dev.sh 실행 중 | 서버 종료 후 dev.sh 재실행 |
| React, 8080/8082 내장 화면 | 서버 종료 → 프론트 빌드 → 사용하는 실행 방식 재시작. JAR이면 Maven 재빌드도 필요 |
| JAR에 포함된 Java/JSON | 서버 종료 → Maven verify로 재빌드 → run-jar/enterprise-demo 재실행 |
| DB 저장 구조 | 새 Liquibase changeSet 작성 → 관련 검증 → 재시작 |

프론트 실시간 개발은 기본 백엔드 8080을 실행한 채 별도 터미널에서 아래 명령을 실행하고 <http://127.0.0.1:9000/fde>를 연다.

```bash
./npmw start
```

기본 Vite 프록시는 8080을 사용한다. 조직 데모의 8082로 자동 연결된다고 가정하지 않는다.

## 5. 데이터와 서명 키

| 실행 | DB | 서명 키 |
|---|---|---|
| 기본 8080 | .local-data/kit.mv.db | .local-data/jwt-secret |
| 조직 8082 | .local-data/enterprise.mv.db | .local-data/enterprise-jwt-secret |

첫 실행에서 키를 생성하고 이후 재사용한다. 실제 키를 출력하거나 Git에 넣지 않는다. 종료 후 같은 실행기로 다시 시작하면 데이터가 유지된다. 백업이 필요하면 모든 해당 서버를 종료한 상태에서 DB 파일과 관련 키를 개인 로컬에 보관한다. 파일을 지우면 데이터가 복구되지 않으므로 자동 초기화 명령은 제공하지 않는다.

예제 계정은 연습용이다. 외부 바인딩/운영 배포는 이 실행 검증 범위가 아니다. 포트 충돌·JAR 누락·로그인 오류는 [오류 해결](TROUBLESHOOTING.md)을 본다.
