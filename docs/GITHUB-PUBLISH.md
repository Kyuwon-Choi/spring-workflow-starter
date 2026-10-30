# 내 GitHub 게시 준비

이 가이드는 로컬 공개 후보를 검토하고 자신의 GitHub로 연결하는 절차다. 이 저장소에는 실제 시험 문제/데이터/답안을 넣지 않는다. 예시 계정은 합성 로컬 계정이다. 출처/변경 내역/라이선스를 유지한다.

## 저장소와 게시 방식

이 키트의 저장소는 [Kyuwon-Choi/spring-workflow-starter](https://github.com/Kyuwon-Choi/spring-workflow-starter)이며 키트 브랜치는 codex/public-starter다. JHipster 공식 저장소의 fork에서 고정 커밋 이후의 개인 변경을 관리한다. 개인 스타터 공개가 평가 반입 허용을 확정하지는 않는다. 아래는 본인의 다른 저장소로 옮기거나 후속 변경을 게시할 때의 절차다.

공식 저장소 fork 또는 자신의 빈 저장소를 사용할 수 있다. 기존 저장소를 덮어쓰지 않는다. 원본은 [UPSTREAM](../UPSTREAM.json), 변경은 [NOTICE](../NOTICE-KIT.md), 라이선스는 [Apache-2.0](../LICENSE.txt)에 남겼다. README의 기능/검증 범위를 검토하고 자신이 설명할 수 있는 상태로 게시한다.

## 포함과 제외

포함: wrapper/잠금 파일, 소스·테스트, 업무 정의, 실행기, 사용 가이드, 질문/계획 skill, 합성 연습 자료, 고정 레포 카드, 검증 요약, 라이선스·변경 고지.

제외: `.git` 자체를 ZIP에 포함하지 않으며, 의존성·빌드 결과·`.local-data`·실제 키/토큰·개인 DB·서버 로그·개인 시험 PDF·감독 자료·실제 업무/시험 자료·개인 절대 경로를 공개 파일에 넣지 않는다. raw 전체 실행 로그 대신 정리된 검증 근거를 사용한다.

## 로컬에서 마지막 확인

```bash
git status --short
git diff --check
git remote -v
git branch --show-current
```

[게시 전 검토 스냅샷](verification/publication-review.json)과 [검증 기록](VALIDATION.md)을 확인한다. 검토 스냅샷은 그 시점의 로컬 상태이며 실제 게시/브랜치 상태는 GitHub의 기록으로 확인한다. 현재 소스와 실행 검증한 소스가 같은지 비교하고, 문서의 링크·명령·실행 권한·민감 경로를 검사한다. 패턴 검사는 모든 비밀값/취약점 부재를 보증하지 않는다.

소스 ZIP에는 Git 이력이 없다. 위 로컬 후보는 원본 이력이 있는 clone이다. ZIP만 받았고 원본 이력을 유지하려면 공식 저장소를 clone한 뒤 소스를 적용한다. ZIP 디렉터리가 이미 Git 저장소라고 가정하지 않는다.

## 원격 연결과 push

아래는 게시할 저장소와 공개 범위를 사람이 확정한 뒤 실행할 예시다. `<OWN_REPOSITORY_URL>`은 실제 본인 저장소 URL로 바꾼다. 기존 origin이 있다면 새로 덮어쓰지 말고 소유/URL부터 확인한다. upstream은 공식 JHipster로 유지한다.

```bash
git remote add origin <OWN_REPOSITORY_URL>
git add --all
git diff --cached --stat
git diff --cached --check
git commit -m "Add reusable FDE workbench and usage guides"
git push -u origin codex/public-starter
```

이 예시는 로컬 후보 브랜치 `codex/public-starter`를 사용한다. 다른 브랜치를 선택했다면 실제 이름으로 바꾼다. --force를 사용하지 않는다. fork라면 기존 기본 브랜치와 차이를 검토한 뒤 본인 저장소 안에서 PR/병합할 수 있다. 새 빈 저장소에서는 README/라이선스를 별도로 생성해 이력 충돌을 만들지 않는 편이 단순하다.

push 후에는 GitHub의 실제 파일 목록과 README 링크를 확인하고, 복제한 새 디렉터리에서 설치/관련 검사를 실행한다. 이 마지막 원격/새 환경 재현은 로컬 문서·파일 일치 검사와 별개다. 원격 주소가 정해지기 전에는 README에 가짜 본인 URL이나 검증되지 않은 CI 배지를 넣지 않는다.
