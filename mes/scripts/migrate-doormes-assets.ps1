[CmdletBinding()]param([switch]$Apply)
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0'){throw 'Unexpected workspace.'}
$taskPrior=Get-Content (Join-Path $taskRoot 'runtime-local/design-data/schema-v003.json') -Raw|ConvertFrom-Json
if($taskPrior.database -ne 'doormes_local'){throw 'Unexpected database.'}
$taskMarker=Join-Path $taskRoot 'runtime-local/design-data/schema-v004.json'
if(Test-Path -LiteralPath $taskMarker){throw 'V004 already applied; do not repeat.'}
$taskCheck=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN('dm_visual_asset','dm_visual_asset_upload'); SELECT COUNT(*) FROM doormes_local.system_menu WHERE id=96000112; SELECT COUNT(*) FROM doormes_local.system_menu WHERE id=96000004 AND permission='doormes:catalog:query';"
if(($taskCheck -split '\r?\n') -join ',' -ne '0,0,1'){throw 'Unexpected baseline. Inspect current state before migration.'}
if(!$Apply){'Preview: two local asset/grant tables, upload permission for design/purchase. doormes_local only.';return}
$taskSql=Join-Path $taskRoot 'db/doormes/V004_assets.sql'
Invoke-DoormesAdminQuery -Sql (Get-Content -LiteralPath $taskSql -Raw)|Out-Null
$taskResult=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN('dm_visual_asset','dm_visual_asset_upload'); SELECT COUNT(*) FROM doormes_local.system_role_menu WHERE menu_id=96000112 AND role_id IN(1003,1005) AND tenant_id=1 AND deleted=0;"
if(($taskResult -split '\r?\n') -join ',' -ne '2,2'){throw 'Migration verification failed; inspect committed state, do not retry blindly.'}
[IO.File]::WriteAllText($taskMarker,(@{version='V004';database='doormes_local';appliedAt=(Get-Date).ToString('o');sqlSha256=(Get-FileHash $taskSql).Hash;tables=2}|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
'V004 applied to the new database only. Reload the dedicated backend for new code/permission cache.'
