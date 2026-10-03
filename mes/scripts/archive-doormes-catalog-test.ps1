[CmdletBinding()]param([Parameter(Mandatory)][string]$ScenarioDirectory,[switch]$Apply)
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot;$taskScenarios=[IO.Path]::GetFullPath((Join-Path $taskRoot 'runtime-local/scenarios'));$taskScenario=(Resolve-Path -LiteralPath $ScenarioDirectory).Path
if(!(Split-Path $taskScenario -Parent).Equals($taskScenarios,[StringComparison]::OrdinalIgnoreCase)){throw 'Only one exact scenario directory is allowed.'}
$taskManifest=Get-Content (Join-Path $taskScenario 'manifest.json') -Raw|ConvertFrom-Json
if($taskManifest.database -ne 'doormes_local' -or $taskManifest.tenantId -ne 1 -or $taskManifest.scenarioId -notmatch '^M1-(CAT|AST)-[0-9]{8}-[0-9]{6}-[a-f0-9]{6}$'){throw 'Invalid catalog test manifest.'}
if(Test-Path -LiteralPath (Join-Path $taskScenario 'catalog-cleanup.json')){throw 'Catalog test already archived.'}
$taskCatalogPaths=@();$taskSql="USE doormes_local; START TRANSACTION;`n"
foreach($taskId in $taskManifest.catalogIds){
  if([Guid]::Parse($taskId).ToString() -ne $taskId){throw 'Invalid catalog identity.'}
  $taskCode=Invoke-DoormesAdminQuery -Sql "SELECT code FROM doormes_local.dm_material_catalog WHERE tenant_id=1 AND id='$taskId';"
  if($taskCode -notin @("TEST-$($taskManifest.scenarioId)-FIN","TEST-$($taskManifest.scenarioId)-GL")){throw 'Catalog not owned by this exact test.'}
  $taskDirectory=Join-Path $taskRoot "runtime-local/design-data/catalog/tenant-1/$taskId";$taskResolved=(Resolve-Path -LiteralPath $taskDirectory).Path
  if($taskResolved -ne [IO.Path]::GetFullPath($taskDirectory) -or (Get-Item -LiteralPath $taskResolved).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Unsafe snapshot directory.'}
  $taskCatalogPaths+=@{id=$taskId;path=$taskResolved;destination=(Join-Path $taskScenario "archived-catalog/$taskId")}
  $taskSql+="DELETE FROM dm_material_catalog_version WHERE tenant_id=1 AND catalog_id='$taskId';`nDELETE FROM dm_material_catalog WHERE tenant_id=1 AND id='$taskId' AND code='$taskCode';`n"
}
if(!$taskCatalogPaths.Count){throw 'No exact controlled catalog items.'}
# First archive the scenario drawing using the existing controlled requirement cleanup.
# Any retained historical/current drawing reference prevents catalog cleanup.
$taskDrawingRows=Invoke-DoormesAdminQuery -Sql "SELECT drawing_id,revision FROM doormes_local.dm_drawing_version WHERE tenant_id=1;"
foreach($taskRow in @($taskDrawingRows -split '\r?\n'|Where-Object {$_})){
  $taskValues=$taskRow -split "`t";if([Guid]::Parse($taskValues[0]).ToString() -ne $taskValues[0] -or $taskValues[1] -notmatch '^[1-9][0-9]*$'){throw 'Unexpected drawing identity.'}
  $taskFile=Join-Path $taskRoot "runtime-local/design-data/drawings/tenant-1/$($taskValues[0])/r$($taskValues[1]).json";$taskContent=Get-Content -LiteralPath $taskFile -Raw
  foreach($taskCatalog in $taskCatalogPaths){if($taskContent.Contains("MES-CATALOG:$($taskCatalog.id)")){throw 'Retained drawing still references test catalog; keep catalog, archive only controlled drawing first.'}}
}
$taskSql+='COMMIT;'
if(!$Apply){"Preview: recoverably archive $($taskCatalogPaths.Count) exact controlled catalog items. No users, menus or manual catalog records affected.";return}
$taskIdsSql=($taskCatalogPaths|ForEach-Object {"'$($_.id)'"}) -join ','
$taskDump=Invoke-DoormesAdminQuery -Sql "SELECT * FROM doormes_local.dm_material_catalog WHERE tenant_id=1 AND id IN($taskIdsSql); SELECT * FROM doormes_local.dm_material_catalog_version WHERE tenant_id=1 AND catalog_id IN($taskIdsSql);"
[IO.File]::WriteAllText((Join-Path $taskScenario 'catalog-before-cleanup.txt'),$taskDump,[Text.UTF8Encoding]::new($false))
$taskRestore=Invoke-DoormesAdminQuery -Sql @"
SELECT CONCAT('INSERT INTO dm_material_catalog(id,tenant_id,category,code,name,revision,status,published_revision,updated_at) VALUES(',QUOTE(id),',',tenant_id,',',QUOTE(category),',',QUOTE(code),',',QUOTE(name),',',revision,',',QUOTE(status),',',IFNULL(published_revision,'NULL'),',',QUOTE(updated_at),');') FROM doormes_local.dm_material_catalog WHERE tenant_id=1 AND id IN($taskIdsSql);
SELECT CONCAT('INSERT INTO dm_material_catalog_version(tenant_id,catalog_id,revision,status,json_path,sha256,change_note,changed_by,updated_at) VALUES(',tenant_id,',',QUOTE(catalog_id),',',revision,',',QUOTE(status),',',QUOTE(json_path),',',QUOTE(sha256),',',QUOTE(change_note),',',changed_by,',',QUOTE(updated_at),');') FROM doormes_local.dm_material_catalog_version WHERE tenant_id=1 AND catalog_id IN($taskIdsSql) ORDER BY catalog_id,revision;
"@
[IO.File]::WriteAllText((Join-Path $taskScenario 'catalog-restore.sql'),("USE doormes_local;`nSET NAMES utf8mb4;`nSTART TRANSACTION;`n"+$taskRestore+"`nCOMMIT;"),[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $taskScenario 'catalog-cleanup.sql'),$taskSql,[Text.UTF8Encoding]::new($false))
[IO.Directory]::CreateDirectory((Join-Path $taskScenario 'archived-catalog'))|Out-Null
foreach($taskPath in $taskCatalogPaths){Copy-Item -LiteralPath $taskPath.path -Destination $taskPath.destination -Recurse -ErrorAction Stop}
Invoke-DoormesAdminQuery -Sql $taskSql|Out-Null
$taskRemaining=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM doormes_local.dm_material_catalog WHERE tenant_id=1 AND id IN($taskIdsSql); SELECT COUNT(*) FROM doormes_local.dm_material_catalog_version WHERE tenant_id=1 AND catalog_id IN($taskIdsSql);"
if(($taskRemaining -split '\r?\n') -join ',' -ne '0,0'){throw 'Catalog rows remain; keep snapshots.'}
foreach($taskPath in $taskCatalogPaths){Remove-Item -LiteralPath $taskPath.path -Recurse -Force}
[IO.File]::WriteAllText((Join-Path $taskScenario 'catalog-cleanup.json'),(@{database='doormes_local';tenantId=1;catalogIds=@($taskCatalogPaths.id);archivedAt=(Get-Date).ToString('o');remaining=0;recoverable=$true}|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
"Archived exact controlled catalog items; full versions and restore SQL at $taskScenario."
