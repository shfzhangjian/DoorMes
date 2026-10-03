[CmdletBinding()]
param([switch]$Apply)
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0'){throw 'Unexpected workspace.'}
$taskPrevious=Get-Content (Join-Path $taskRoot 'runtime-local/design-data/schema-v001.json') -Raw | ConvertFrom-Json
if($taskPrevious.database -ne 'doormes_local'){throw 'Unexpected database.'}
$taskMarker=Join-Path $taskRoot 'runtime-local/design-data/schema-v002.json'
if(Test-Path -LiteralPath $taskMarker){throw 'Drawing schema already applied; do not repeat.'}
$taskCheck=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN ('dm_drawing','dm_drawing_version'); SELECT COUNT(*) FROM doormes_local.system_menu WHERE id=96000105; SELECT COUNT(*) FROM doormes_local.system_role WHERE id=1003 AND code='factory_design' AND tenant_id=1;"
if(($taskCheck -split '\r?\n') -join ',' -ne '0,0,1'){throw 'Unexpected schema or permission baseline; inspect before applying.'}
if(!$Apply){'Preview: two drawing tables and one R&D save permission in doormes_local only.';return}
$taskSqlPath=Join-Path $taskRoot 'db/doormes/V002_drawings.sql'
Invoke-DoormesAdminQuery -Sql (Get-Content -LiteralPath $taskSqlPath -Raw) | Out-Null
$taskVerified=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN ('dm_drawing','dm_drawing_version'); SELECT COUNT(*) FROM doormes_local.system_menu WHERE id=96000105 AND permission='doormes:design:drawing-save'; SELECT COUNT(*) FROM doormes_local.system_role_menu WHERE role_id=1003 AND menu_id=96000105 AND tenant_id=1 AND deleted=0;"
if(($taskVerified -split '\r?\n') -join ',' -ne '2,1,1'){throw 'Migration verification failed; inspect committed state before retry.'}
[IO.File]::WriteAllText($taskMarker,(@{version='V002';database='doormes_local';appliedAt=(Get-Date).ToString('o');sqlSha256=(Get-FileHash $taskSqlPath).Hash;tables=2;buttonPermissions=1}|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
'Applied V002 only to doormes_local. Restart dedicated backend to reload permission caches.'
