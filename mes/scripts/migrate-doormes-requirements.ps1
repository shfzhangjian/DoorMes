[CmdletBinding()]
param([switch]$Apply)
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot
if ($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0') { throw 'Unexpected workspace.' }
$taskBootstrap=Get-Content (Join-Path $taskRoot 'runtime-local/factory-bootstrap/complete.json') -Raw | ConvertFrom-Json
if ($taskBootstrap.database -ne 'doormes_local') { throw 'Factory initialization required.' }
$taskMarker=Join-Path $taskRoot 'runtime-local/design-data/schema-v001.json'
if (Test-Path -LiteralPath $taskMarker) { throw 'Requirement schema is already migrated; do not rerun.' }
$taskCheck=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM doormes_local.system_menu WHERE id BETWEEN 96000101 AND 96000104; SELECT COUNT(*) FROM doormes_local.system_role WHERE (id=1002 AND code='factory_sales') OR (id=1003 AND code='factory_design');"
$taskValues=@($taskCheck -split '\r?\n')
if ($taskValues[0] -ne '0' -or $taskValues[1] -ne '2') { throw 'Unexpected menu or role baseline.' }
if (!$Apply) { 'Preview: add two dm_design_requirement tables, four business action permissions and two real page mappings; no shared users/departments will be changed.'; return }
$taskSqlPath=Join-Path $taskRoot 'db/doormes/V001_design_requirements.sql'
Invoke-DoormesAdminQuery -Sql (Get-Content -LiteralPath $taskSqlPath -Raw) | Out-Null
$taskVerified=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN ('dm_design_requirement','dm_design_requirement_version'); SELECT COUNT(*) FROM doormes_local.system_menu WHERE id BETWEEN 96000101 AND 96000104; SELECT COUNT(*) FROM doormes_local.system_role_menu WHERE deleted=0 AND menu_id BETWEEN 96000101 AND 96000104;"
if ($taskVerified -ne "2`r`n4`r`n4" -and $taskVerified -ne "2`n4`n4") { throw 'Migration verification failed; inspect committed state before retry.' }
[IO.Directory]::CreateDirectory((Split-Path $taskMarker)) | Out-Null
[IO.File]::WriteAllText($taskMarker,(@{version='V001';database='doormes_local';appliedAt=(Get-Date).ToString('o');sqlSha256=(Get-FileHash $taskSqlPath).Hash;tables=2;buttonPermissions=4}|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
'Migrated only doormes_local: 2 new tables, 4 action permissions. Reload dedicated backend permission caches.'
