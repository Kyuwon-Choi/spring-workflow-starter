# 엔터프라이즈 확장 결과

이 문서는 2026-10-28 로컬 검증 당시의 기록이다. 이후 게시 여부와 커밋은 GitHub 저장소의 기록을 기준으로 한다.

사용자가 요청한 복잡한 권한과 추가 요구 대응 범위를 실제 구현했다. **회사/부서/프로젝트·기간 위임·자기 승인 금지·조건부 2차 승인·규칙 버전·공유 예산**을 선택형 정책과 동작 확장으로 연결했다. 일반 업무의 기존 권한 의미는 유지했다.

## 실제 검사

- Maven verify: **241개**, Surefire 123 + Failsafe 118, 실패/오류/skip 0.
- 프론트 전체: **191개/23파일** 통과. 타입 검사·변경 UI lint·Vite/JAR 빌드 통과.
- 기본 check.sh: 관련 백엔드 **75개**, UI **10개** 통과. 위 전체 검사와 겹치는 수치다.
- EnterpriseScenarioTest **16개**: 두 회사/부서/프로젝트·전역 관리자 예외 금지·구규칙/신규 고액 건·시행 시각/금액 경계·반려 재제출·위임 시작/만료/취소/역할 변경·비공개 메모/검색 건수·승인자 분리·서로 다른 요청의 예산 경쟁·이력 실패 롤백·소속 충돌·회사별 키.
- 합성 계정 실제 JWT **59개 요청** 통과. 역할 변경 뒤 기존 토큰을 사용한 다음 요청도 거부됨을 확인했다. 토큰/암호 응답은 증거에 기록하지 않았다.
- 브라우저: admin 규칙 UI-V3 게시→user 요청 생성/제출→fde-manager 부서 승인→FINANCE_PENDING→fde-finance 최종 승인→4개 변경 이력 확인.
- 재기동: 고정 규칙/승인 상태/예산 70원/위임 취소/직원용 이력 비노출과 화면 검증 건 #7의 서로 다른 승인자를 재조회했다.
- 기존 파일 DB의 support 객체 #66 CLOSED 상태가 migration 뒤 유지됨을 확인했고, 기존 세 업무의 실제 HTTP smoke도 통과했다.

## 고정 요구의 근거

| 시나리오 항목 | 근거 |
|---|---|
| 1~3 현재 소속·회사/부서/프로젝트 격리 | tenantDepartmentAndProjectAreEnforcedOnEveryReadSurface, clientCannotForgeTenantOrCreateOutsideCurrentProject, membershipChangesAreScopedVersionedAndAudited |
| 4 출력/검색 비노출 | fieldRedactionAlsoAppliesToHistoryExportAndSearchCounts, 실제 HTTP의 상세/이력/CSV/검색 |
| 5 본인 제출·승인자 분리 | administratorsCannotSubmitOrApproveTheirOwnOrOtherPersonsDraft, onePersonsTwoRolesCannotSupplyBothApprovals, 실제 화면 |
| 6 조건부 2차 승인 | oldSubmittedPolicySurvivesNewRuleAndNewHighAmountNeedsTwoApprovals, policyTimeAndAmountBoundariesAreExactAndVersionsCannotBeOverwritten, 실제 화면 |
| 7~8 구규칙 보존·재제출 | 위 규칙 검사와 rejectedResubmissionChoosesNewVersionAndRetainsOldAudit, 실제 구규칙→새규칙 게시→구건 처리 |
| 9~10 위임 유효성 | delegationStartsInclusivelyEndsExclusivelyAndCannotApproveGrantorsOwn, revokedDelegationAndChangedMembershipImmediatelyInvalidateOldScreens, projectRevocationAndGrantorRoleRemovalInvalidateDelegation |
| 11~12 순서/예산/롤백 | 회사 잠금 경계, simultaneousDifferentRequestsCannotOverspendSharedBudget, auditFailureRollsBackBudgetRequestAndRetryDoesNotDoubleSpend, HTTP 동시 결과 200/409 |
| 13 설정 범위/버전/이력 | membershipChangesAreScopedVersionedAndAudited, 규칙 덮어쓰기/과거 시행일 거부, HTTP 회사별 설정 범위 |
| 14 기존 계약·다른 도메인 재사용 | 전체 회귀 검사, aDifferentDomainReusesThePolicyWithCompanyScopedBusinessKeys, 기존 DB/HTTP |
| 15 재현 가능한 결과 | README, 원본 소스, 고정 시나리오 해시, 공개 검증 요약 및 로컬 원본 로그·실제 화면·소스 묶음 |

위 표는 이 가상 정책과 선택한 경계 사례의 검사 근거다. 모든 기업의 임의 정책이나 모든 조합을 완전 증명한 수치로 환산하지 않는다. 시나리오와 구현자는 동일하며 독립 블라인드 평가는 아니다.

## 구조와 변경 대응

FdePolicy가 SQL 조회 범위·단건/동작·생성 메타데이터·출력 필드·검색 색인·업무 키 범위를 담당한다. 회사 공통 잠금은 객체 잠금보다 먼저 획득한다. EnterpriseCommands는 변경 불가 규칙 버전 선택·조건부 다음 상태·최종 예산 차감을 구현한다. 요청/예산/감사 저장은 같은 트랜잭션이다.

기존 nullable 정책 정의/기존 changes 확장은 그대로 지원한다. DB에는 새로운 incremental changeSet을 추가했으며 기존 changeSet을 수정하지 않았다. 업무 키는 business_scope의 기본 빈 값으로 기존 의미를 유지한다. 실제 기존 파일 DB에서 업그레이드와 이전 데이터 보존을 확인했다.

앞으로 요구가 추가되면 적용 시점/기존 건 처리/유지할 조건을 먼저 확정하고, 변경 전후 기대 사례를 남긴 뒤 관련 정책/확장과 회귀 검사를 함께 바꾼다. 이것이 임의 요구에 대한 자동 구현을 보장하지는 않는다.

## 남은 경계

한 계정 한 회사·평면 부서·명시적 프로젝트의 준비용 구현이다. SSO/전체 JWT 취소·임의 조직 트리·다중 회사 동시 소속·외부 시스템 원자성·운영 부하/장애·다른 DB 엔진은 검증하지 않았다. 회사별 변경 직렬화는 고처리량 검증을 대신하지 않는다. 내부 메모는 모든 역할에서 검색 대상에서 제외한다. 일반 도메인에 조직 정책을 나중에 연결할 때의 과거 데이터 이관과 필드/상태 자체 변경은 명시적 migration이 필요하다.

공식 시험 반입/런타임과 사용자 직접 적용/시간 관리/AI 없는 사후 답변은 별도 확인 대상이다. 공개 저장소에 올리지는 않았다.

공개판에는 합성 시나리오와 보고서만 포함했다. 실행 로그·화면·DB·키는 원래 개발 작업 공간에 보관하고, 공개 요약은 [enterprise-validation](../enterprise-validation.json)을 본다.
