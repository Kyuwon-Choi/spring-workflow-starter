# Attribution and changes

This starter adapts the Apache-2.0-licensed official JHipster React sample:
https://github.com/jhipster/jhipster-sample-app-react

The exact upstream commit is in UPSTREAM.json. Original licensing is retained in LICENSE.txt; the original README is preserved in UPSTREAM-README.md. Dependency licenses remain those of their authors.

Local changes: the kit Java package and integration tests; equipment/request relationships; owner/admin access; versioned state transitions; transactional audit events; React request board; local-only launcher and persistent H2 profile; generated local signing key; strict unknown-field validation; closed unrelated sample CRUD APIs and matching negative API tests; reduced optional generator/browser-audit dependencies; commons-text dependency management; type-check configuration; smoke/check scripts and preparation documentation.

This is a local rehearsal starter, not a security-certified or production-deployed application. Original sample CRUD boundary tests were replaced with tests of the intentionally closed endpoints; the original source/tests remain at the pinned upstream commit.

2026-10-28: added a definition-driven FDE workbench (typed fields/references, role/scope policies, actions, atomic audit/versioned mutations, CSV preview/batch/export, generic React forms/list/detail, three example definitions and a test-only fourth); a small stock-calculation extension; a DuckDB batch analysis recipe; pinned reference-selection cards; and a downloader for the separately licensed official Timefold Spring example. Workbench code is a personal adaptation, not upstream JHipster functionality or an assessment answer. The older equipment module is retained as an independent legacy example.

Apache Commons CSV 1.14.1 is consumed as a dependency under Apache-2.0. DuckDB 1.4.1 is a separately installed MIT-licensed dependency. The optional Timefold downloader preserves the official Apache-2.0 license and pinned source metadata. Other referenced repositories are not automatically incorporated into this application; consult each reference's license before reuse.

2026-10-28 follow-up: protected server-calculated creation fields and owner-only actions; added optional FdePolicy boundaries, company-scoped business keys/search/output filtering, relational company/department/project membership and delegation, immutable effective-dated approval rules, conditional two-stage approvals with reviewer separation, transactional shared-budget accounting, incremental Liquibase migration, organization UI and regression tests. Added standalone usage/API/adaptation/change-control/security/troubleshooting/rehearsal guides and optional personal discovery/planning skills. These are personal adaptations; they are not JHipster or Palantir features or official assessment policy.
