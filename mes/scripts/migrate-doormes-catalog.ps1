[CmdletBinding()] param([switch]$Apply)
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0'){throw 'Unexpected workspace.'}
$taskPrevious=Get-Content (Join-Path $taskRoot 'runtime-local/design-data/schema-v002.json') -Raw|ConvertFrom-Json
if($taskPrevious.database -ne 'doormes_local'){throw 'Unexpected database.'}
$taskMarker=Join-Path $taskRoot 'runtime-local/design-data/schema-v003.json'
if(Test-Path -LiteralPath $taskMarker){throw 'Catalog schema already applied; do not repeat.'}
$taskCheck=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN ('dm_material_catalog','dm_material_catalog_version'); SELECT COUNT(*) FROM doormes_local.system_menu WHERE id IN(96000110,96000111); SELECT COUNT(*) FROM doormes_local.system_menu WHERE id=96000004 AND permission='doormes:catalog:query';"
if(($taskCheck -split '\r?\n') -join ',' -ne '0,0,1'){throw 'Unexpected schema or menu baseline; inspect first.'}
if(!$Apply){'Preview: two catalog tables, real catalog page, R&D/purchase edit; publishing administrator only. doormes_local only.';return}
$taskSqlPath=Join-Path $taskRoot 'db/doormes/V003_catalog.sql'
Invoke-DoormesAdminQuery -Sql (Get-Content -LiteralPath $taskSqlPath -Raw)|Out-Null
$taskVerified=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN ('dm_material_catalog','dm_material_catalog_version'); SELECT COUNT(*) FROM doormes_local.system_menu WHERE id IN(96000110,96000111); SELECT COUNT(*) FROM doormes_local.system_role_menu WHERE menu_id=96000110 AND role_id IN(1003,1005) AND tenant_id=1 AND deleted=0; SELECT COUNT(*) FROM doormes_local.system_menu WHERE id=96000004 AND component='doormes/catalog/index';"
if(($taskVerified -split '\r?\n') -join ',' -ne '2,2,2,1'){throw 'Migration verification failed; inspect committed state before retry.'}
[IO.File]::WriteAllText($taskMarker,(@{version='V003';database='doormes_local';appliedAt=(Get-Date).ToString('o');sqlSha256=(Get-FileHash $taskSqlPath).Hash;tables=2}|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
'Applied V003 only to doormes_local. Restart dedicated backend to load code and permissions.'
