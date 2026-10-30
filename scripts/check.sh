#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./mvnw -ntp -Dskip.installnodenpm -Dskip.npm -Dtest=FdeWorkbenchTest,TrainingScenarioTest,EnterpriseScenarioTest,KitWorkflowTest,SecurityUtilsUnitTest,TechnicalStructureTest test
./npmw run kit:typecheck
./npmw exec eslint -- src/main/webapp/app/modules/fde/workbench.tsx src/main/webapp/app/modules/fde/workbench.spec.tsx src/main/webapp/app/modules/fde/organization.tsx src/main/webapp/app/modules/fde/organization.spec.tsx src/main/webapp/app/modules/kit/kit-board.tsx src/main/webapp/app/modules/kit/kit-board.spec.tsx src/main/webapp/app/routes.tsx src/main/webapp/app/entities/menu.tsx
./npmw run vitest-run -- --coverage.enabled=false app/modules/fde/workbench.spec.tsx app/modules/fde/organization.spec.tsx app/modules/kit/kit-board.spec.tsx
./npmw run webapp:build:dev
