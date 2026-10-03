[CmdletBinding()]param([switch]$Apply)
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0'){throw 'Unexpected workspace.'}
$taskPrior=Get-Content (Join-Path $taskRoot 'runtime-local/design-data/schema-v004.json') -Raw|ConvertFrom-Json
if($taskPrior.database -ne 'doormes_local'){throw 'Unexpected database.'}
$taskMarker=Join-Path $taskRoot 'runtime-local/design-data/schema-v005.json'
if(Test-Path -LiteralPath $taskMarker){throw 'V005 already applied; inspect marker before any retry.'}
$taskCheck=Invoke-DoormesAdminQuery -Sql @'
SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='doormes_local' AND table_name='dm_drawing' AND column_name='drawing_number';
SELECT COUNT(*) FROM doormes_local.system_menu WHERE id=96000113;
SELECT COUNT(*) FROM doormes_local.system_menu WHERE id IN(96000003,96000008) AND deleted=0;
SELECT COUNT(*) FROM doormes_local.system_role WHERE id=1003 AND deleted=0;
'@
if(($taskCheck -split '\r?\n') -join ',' -ne '0,0,2,1'){throw 'Unexpected baseline. Inspect before applying.'}
if(!$Apply){'Preview: extend existing drawing heads; independent creation + unique editable number; design/drawing menu mappings. doormes_local only.';return}
$taskBackup=Join-Path $taskRoot ('runtime-local/design-data/migration-v005-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $taskBackup|Out-Null
$taskBefore=Invoke-DoormesAdminQuery -Sql @'
SHOW CREATE TABLE doormes_local.dm_drawing;
SELECT COUNT(*) FROM doormes_local.dm_drawing;
SELECT COUNT(*) FROM doormes_local.dm_drawing_version;
SELECT id,name,component,component_name,updater FROM doormes_local.system_menu WHERE id IN(96000003,96000008);
'@
[IO.File]::WriteAllText((Join-Path $taskBackup 'baseline.txt'),$taskBefore,[Text.UTF8Encoding]::new($false))
$taskSql=Join-Path $taskRoot 'db/doormes/V005_design_center.sql'
Invoke-DoormesAdminQuery -Sql (Get-Content -LiteralPath $taskSql -Raw)|Out-Null
$taskResult=Invoke-DoormesAdminQuery -Sql @'
SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='doormes_local' AND table_name='dm_drawing' AND column_name IN('drawing_number','name','note','source_type','created_at','create_request_id','create_payload_hash');
SELECT COUNT(*) FROM doormes_local.system_menu WHERE id IN(96000003,96000008) AND component='doormes/designs/index';
SELECT COUNT(*) FROM doormes_local.system_role_menu WHERE menu_id=96000113 AND role_id=1003 AND tenant_id=1 AND deleted=0;
'@
if(($taskResult -split '\r?\n') -join ',' -ne '7,2,1'){throw 'Verification failed. DDL is committed; inspect state, do not repeat blindly.'}
[IO.File]::WriteAllText($taskMarker,(@{version='V005';database='doormes_local';appliedAt=(Get-Date).ToString('o');sqlSha256=(Get-FileHash $taskSql).Hash;backup=$taskBackup}|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
'V005 applied. Existing snapshots were not rewritten. Never drop new independent records to revert schema; recover with a forward repair. Reload dedicated backend permission cache.'
