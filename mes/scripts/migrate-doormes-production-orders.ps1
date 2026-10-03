[CmdletBinding()]param([switch]$Apply)
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0'){throw 'Unexpected workspace.'}
$taskPrior=Get-Content (Join-Path $taskRoot 'runtime-local/design-data/schema-v005.json') -Raw|ConvertFrom-Json
if($taskPrior.database -ne 'doormes_local'){throw 'Unexpected database.'}
$taskMarker=Join-Path $taskRoot 'runtime-local/design-data/schema-v006.json'
if(Test-Path -LiteralPath $taskMarker){throw 'V006 already applied; inspect marker before any retry.'}
$taskCheck=Invoke-DoormesAdminQuery -Sql @'
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN('dm_production_order','dm_production_order_version');
SELECT COUNT(*) FROM doormes_local.system_menu WHERE id BETWEEN 96000114 AND 96000117;
SELECT COUNT(*) FROM doormes_local.system_menu WHERE (id=96000006 AND path='production') OR (id=96000007 AND path='changes');
SELECT COUNT(*) FROM doormes_local.system_role WHERE tenant_id=1 AND deleted=0 AND ((id=1002 AND code='factory_sales') OR (id=1003 AND code='factory_design') OR (id=1006 AND code='factory_production') OR (id=1007 AND code='factory_reviewer'));
'@
if(($taskCheck -split '\r?\n') -join ',' -ne '0,0,2,4'){throw 'Unexpected baseline. Inspect before applying.'}
if(!$Apply){'Preview: add two immutable production design order tables; activate production/change menus and four scoped actions. doormes_local only. No historical MES/account/organization deletion.';return}
$taskBackup=Join-Path $taskRoot ('runtime-local/design-data/migration-v006-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $taskBackup|Out-Null
$taskBefore=Invoke-DoormesAdminQuery -Sql @'
SELECT id,name,component,component_name,updater FROM doormes_local.system_menu WHERE id IN(96000006,96000007);
SELECT role_id,menu_id FROM doormes_local.system_role_menu WHERE role_id IN(1002,1003,1006,1007) AND tenant_id=1 AND deleted=0;
SELECT COUNT(*) FROM doormes_local.system_users;
SELECT COUNT(*) FROM doormes_local.system_dept;
SELECT COUNT(*) FROM doormes_local.dm_drawing;
SELECT COUNT(*) FROM doormes_local.dm_drawing_version;
'@
[IO.File]::WriteAllText((Join-Path $taskBackup 'baseline.txt'),$taskBefore,[Text.UTF8Encoding]::new($false))
$taskSql=Join-Path $taskRoot 'db/doormes/V006_production_design_orders.sql'
Invoke-DoormesAdminQuery -Sql (Get-Content -LiteralPath $taskSql -Raw)|Out-Null
$taskResult=Invoke-DoormesAdminQuery -Sql @'
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN('dm_production_order','dm_production_order_version');
SELECT COUNT(*) FROM doormes_local.system_menu WHERE id IN(96000006,96000007) AND component='doormes/production-orders/index';
SELECT COUNT(*) FROM doormes_local.system_role_menu WHERE menu_id BETWEEN 96000114 AND 96000117 AND tenant_id=1 AND deleted=0;
SELECT COUNT(*) FROM doormes_local.dm_production_order;
'@
if(($taskResult -split '\r?\n') -join ',' -ne '2,2,4,0'){throw 'Verification failed. DDL is committed; inspect state, do not repeat blindly.'}
[IO.File]::WriteAllText($taskMarker,(@{version='V006';database='doormes_local';appliedAt=(Get-Date).ToString('o');sqlSha256=(Get-FileHash $taskSql).Hash;backup=$taskBackup}|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
'V006 applied. Orders freeze saved drawing/BOM versions, not production release. Reload dedicated backend permission cache.'
