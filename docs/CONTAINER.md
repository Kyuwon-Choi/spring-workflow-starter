# 생성 앱의 선택형 Docker 구성

Dockerfile이 제출물에 필요할 때만 가져온다. 기존 프로젝트에는 그 프로젝트의 빌드/실행 계약을 우선한다. 기본 최소 앱은 Docker 파일이나 Docker 의존성을 포함하지 않는다.

```bash
# 새 앱
python3 scripts/create-solution.py /실제/작업폴더/solution --docker
# 이미 생성한 앱에 추가
python3 scripts/create-solution.py /실제/작업폴더/solution --add --docker
```

업무 모듈과 함께 선택해도 된다. 수정된 README 등 공유 파일과 충돌하면 생성기가 쓰기 전에 중단하므로 실제 차이를 병합한다. 기존 앱의 자동 업그레이드 도구는 아니다.

## 빌드와 실행

생성 앱 폴더에서:

```bash
./scripts/container.sh build
./scripts/container.sh run 8080
```

Docker CLI와 실행 중인 daemon, 이미지와 Maven/npm 다운로드 네트워크가 필요하다. 이 방식은 Docker 안에서 JDK·Maven wrapper·Node/npm·프론트 빌드·JAR 패키징을 수행한다. 호스트 JDK/Node는 이 경로에 필요하지 않다. `eclipse-temurin:21-jdk-jammy`에서 빌드하고 `21-jre-jammy`에 JAR만 복사한다. 태그의 패치 버전은 갱신될 수 있으므로 완전한 이미지 재현성이 요구되면 실제로 확인한 digest를 고정한다. [Docker 다단계 빌드](https://docs.docker.com/build/building/multi-stage/), [Temurin 이미지 소스](https://github.com/adoptium/containers).

실행 사용자는 UID/GID 10001이며 내부 포트는 8080, 바인딩은 0.0.0.0이다. 외부 미리보기에는 로그와 helper에 지정한 호스트 포트를 확인하고 최소 앱 `/`, workflow `/fde`를 입력한다. `run`은 foreground이므로 터미널을 유지한다. helper는 프레임을 허용하는 `app.preview=true`를 넘기며, 일반 직접 실행의 기본 프레임 보호와 구분한다. `EXPOSE` 선언만으로 호스트 포트가 연결되는 것은 아니다.

이미지와 데이터 볼륨의 기본 이름은 `solution-local`, `solution-local-data`다. 여러 앱을 다룰 때에는 두 실행 전에 `SOLUTION_IMAGE`, `SOLUTION_DATA_VOLUME`을 프로젝트별 값으로 설정한다. 이름을 바꿨다면 빌드·실행에 같은 이미지 이름을 사용한다.

auth 비밀번호는 실행 터미널에서 설정한 `APP_ADMIN_PASSWORD`, `APP_USER_PASSWORD`를 helper가 전달한다. 값은 Dockerfile·작업 기록에 쓰지 않는다. 앱의 `.local-data`는 named volume에 보관한다. 컨테이너를 종료/삭제해도 볼륨은 남으며 DB·JWT 서명 키가 함께 유지된다. 이전 데이터의 UID/권한은 자동 변경하지 않는다. 로컬 `.local-data`, 환경 파일, 의존성 캐시는 `.dockerignore`로 빌드 컨텍스트에서 제외한다. 입력 자료를 이미지에 포함해야 하는지는 과제 지침에 맞춘다.

## 실제 완료 확인

이미지 빌드 성공 → 컨테이너 실행 → `/api/health`와 정적 파일 → 선택한 인증/업무 API → 화면 직접 확인 순서로 기록한다. 영속 상태가 필요하면 중지 후 같은 볼륨으로 다시 실행해 데이터와 계정/키 동작을 확인한다. 관련 포트·명령·실제 결과를 WORK에 남긴다. 이 명령은 로컬 실행이며 외부 배포를 하지 않는다.

2026-11-02 준비 환경에는 Docker CLI/daemon이 없어 실제 이미지 빌드·컨테이너 실행은 **미검증**이다. 생성/추가와 helper 인자 연결의 자동 검사는 Docker 엔진 검증을 대신하지 않는다. 실제 사용 환경에서 위 절차를 실행한 뒤 그 결과로 갱신한다.
