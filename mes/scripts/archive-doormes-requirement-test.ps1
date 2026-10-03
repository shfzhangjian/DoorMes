[CmdletBinding()]
param([Parameter(Mandatory)][string]$ScenarioDirectory,[switch]$Apply)
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot
$taskScenarios=[IO.Path]::GetFullPath((Join-Path $taskRoot 'runtime-local/scenarios'))
$taskScenario=(Resolve-Path -LiteralPath $ScenarioDirectory).Path
if(!(Split-Path $taskScenario -Parent).Equals($taskScenarios,[StringComparison]::OrdinalIgnoreCase)) { throw 'Only one exact test scenario directory is allowed.' }
$taskManifest=Get-Content (Join-Path $taskScenario 'manifest.json') -Raw|ConvertFrom-Json
if($taskManifest.database -ne 'doormes_local' -or $taskManifest.tenantId -ne 1 -or $taskManifest.scenarioId -notmatch '^M1-(REQ|DRAW|CAT|AST)-[0-9]{8}-[0-9]{6}-[a-f0-9]{6}$') {throw 'Invalid test manifest.'}
if(Test-Path (Join-Path $taskScenario 'cleanup.json')) {throw 'Scenario already archived.'}
$taskPaths=@()
$taskDrawingPaths=@()
$taskHasDrawings=(Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN ('dm_drawing','dm_drawing_version');") -eq '2'
$taskSql="USE doormes_local; START TRANSACTION;`n"
foreach($taskId in $taskManifest.ids) {
  if([Guid]::Parse($taskId).ToString() -ne $taskId) {throw 'Invalid test record identity.'}
  $taskNumber=Invoke-DoormesAdminQuery -Sql "SELECT number FROM doormes_local.dm_design_requirement WHERE tenant_id=1 AND id='$taskId';"
  if($taskNumber -notin @("TEST-$($taskManifest.scenarioId)","INVALID-$($taskManifest.scenarioId)")) {throw 'Record is not owned by this controlled scenario.'}
  $taskDirectory=Join-Path $taskRoot "runtime-local/design-data/requirements/tenant-1/$taskId"
  $taskResolved=(Resolve-Path -LiteralPath $taskDirectory).Path
  $taskExpected=[IO.Path]::GetFullPath($taskDirectory)
  if($taskResolved -ne $taskExpected -or (Get-Item -LiteralPath $taskResolved).Attributes -band [IO.FileAttributes]::ReparsePoint) {throw 'Unsafe test snapshot directory.'}
  $taskPaths+=@{id=$taskId;path=$taskResolved;destination=(Join-Path $taskScenario "archived-data/$taskId")}
  if($taskHasDrawings) {
    $taskDrawingIds=Invoke-DoormesAdminQuery -Sql "SELECT id FROM doormes_local.dm_drawing WHERE tenant_id=1 AND requirement_id='$taskId';"
    foreach($taskDrawingId in @($taskDrawingIds -split '\r?\n'|Where-Object {$_})) {
      if([Guid]::Parse($taskDrawingId).ToString() -ne $taskDrawingId){throw 'Invalid drawing identity.'}
      $taskDrawingDirectory=Join-Path $taskRoot "runtime-local/design-data/drawings/tenant-1/$taskDrawingId"
      $taskDrawingResolved=(Resolve-Path -LiteralPath $taskDrawingDirectory).Path
      if($taskDrawingResolved -ne [IO.Path]::GetFullPath($taskDrawingDirectory) -or (Get-Item -LiteralPath $taskDrawingResolved).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Unsafe drawing snapshot directory.'}
      $taskDrawingPaths+=@{id=$taskDrawingId;path=$taskDrawingResolved;destination=(Join-Path $taskScenario "archived-drawings/$taskDrawingId")}
      $taskSql+="DELETE FROM dm_drawing_version WHERE tenant_id=1 AND drawing_id='$taskDrawingId';`nDELETE FROM dm_drawing WHERE tenant_id=1 AND id='$taskDrawingId' AND requirement_id='$taskId';`n"
    }
  }
  $taskSql+="DELETE FROM dm_design_requirement_version WHERE tenant_id=1 AND requirement_id='$taskId';`nDELETE FROM dm_design_requirement WHERE tenant_id=1 AND id='$taskId' AND number IN ('TEST-$($taskManifest.scenarioId)','INVALID-$($taskManifest.scenarioId)');`n"
}
$taskSql+='COMMIT;'
if(!$Apply) { "Preview: archive $($taskPaths.Count) controlled test record(s), preserving snapshots and restoring SQL; no users, organizations or manual demands are affected."; return }
if($taskPaths.Count -eq 0) { throw 'No exact controlled records to archive.' }
$taskIdsSql=($taskPaths | ForEach-Object {"'$($_.id)'"}) -join ','
$taskDumpSql="SELECT * FROM doormes_local.dm_design_requirement WHERE tenant_id=1 AND id IN ($taskIdsSql); SELECT * FROM doormes_local.dm_design_requirement_version WHERE tenant_id=1 AND requirement_id IN ($taskIdsSql);"
if($taskDrawingPaths.Count -gt 0){$taskDrawingIdsSql=($taskDrawingPaths|ForEach-Object {"'$($_.id)'"}) -join ',';$taskDumpSql+=" SELECT * FROM doormes_local.dm_drawing WHERE tenant_id=1 AND id IN ($taskDrawingIdsSql); SELECT * FROM doormes_local.dm_drawing_version WHERE tenant_id=1 AND drawing_id IN ($taskDrawingIdsSql);"}
$taskDump=Invoke-DoormesAdminQuery -Sql $taskDumpSql
[IO.File]::WriteAllText((Join-Path $taskScenario 'before-cleanup.txt'),$taskDump,[Text.UTF8Encoding]::new($false))
[string]$taskRestoreRows=Invoke-DoormesAdminQuery -Sql @"
SELECT CONCAT('INSERT INTO dm_design_requirement (id,tenant_id,number,customer,project,revision,status,created_by,assigned_to,line_count,quantity,created_at,updated_at) VALUES (',QUOTE(id),',',tenant_id,',',QUOTE(number),',',QUOTE(customer),',',QUOTE(project),',',revision,',',QUOTE(status),',',created_by,',',IFNULL(assigned_to,'NULL'),',',line_count,',',quantity,',',QUOTE(created_at),',',QUOTE(updated_at),');') FROM doormes_local.dm_design_requirement WHERE tenant_id=1 AND id IN ($taskIdsSql);
SELECT CONCAT('INSERT INTO dm_design_requirement_version (tenant_id,requirement_id,revision,json_path,sha256,action,change_note,changed_by,updated_at) VALUES (',tenant_id,',',QUOTE(requirement_id),',',revision,',',QUOTE(json_path),',',QUOTE(sha256),',',QUOTE(action),',',QUOTE(change_note),',',changed_by,',',QUOTE(updated_at),');') FROM doormes_local.dm_design_requirement_version WHERE tenant_id=1 AND requirement_id IN ($taskIdsSql) ORDER BY requirement_id,revision;
"@
if($taskDrawingPaths.Count -gt 0) {
  $taskRestoreRows+="`n"+(Invoke-DoormesAdminQuery -Sql @"
SELECT CONCAT('INSERT INTO dm_drawing (id,tenant_id,requirement_id,requirement_revision,line_id,revision,status,created_by,updated_at) VALUES (',QUOTE(id),',',tenant_id,',',QUOTE(requirement_id),',',requirement_revision,',',QUOTE(line_id),',',revision,',',QUOTE(status),',',created_by,',',QUOTE(updated_at),');') FROM doormes_local.dm_drawing WHERE tenant_id=1 AND id IN ($taskDrawingIdsSql);
SELECT CONCAT('INSERT INTO dm_drawing_version (tenant_id,drawing_id,revision,json_path,sha256,change_note,changed_by,updated_at) VALUES (',tenant_id,',',QUOTE(drawing_id),',',revision,',',QUOTE(json_path),',',QUOTE(sha256),',',QUOTE(change_note),',',changed_by,',',QUOTE(updated_at),');') FROM doormes_local.dm_drawing_version WHERE tenant_id=1 AND drawing_id IN ($taskDrawingIdsSql) ORDER BY drawing_id,revision;
"@)
}
[IO.File]::WriteAllText((Join-Path $taskScenario 'restore.sql'),("USE doormes_local;`nSET NAMES utf8mb4;`nSTART TRANSACTION;`n"+$taskRestoreRows+"`nCOMMIT;"),[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText((Join-Path $taskScenario 'cleanup.sql'),$taskSql,[Text.UTF8Encoding]::new($false))
# Complete document files plus metadata are preserved before deleting only test rows.
[IO.Directory]::CreateDirectory((Join-Path $taskScenario 'archived-data'))|Out-Null
foreach($taskPath in $taskPaths) { Copy-Item -LiteralPath $taskPath.path -Destination $taskPath.destination -Recurse -ErrorAction Stop }
[IO.Directory]::CreateDirectory((Join-Path $taskScenario 'archived-drawings'))|Out-Null
foreach($taskPath in $taskDrawingPaths) { Copy-Item -LiteralPath $taskPath.path -Destination $taskPath.destination -Recurse -ErrorAction Stop }
Invoke-DoormesAdminQuery -Sql $taskSql|Out-Null
$taskRemaining=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM doormes_local.dm_design_requirement WHERE tenant_id=1 AND id IN ($taskIdsSql); SELECT COUNT(*) FROM doormes_local.dm_design_requirement_version WHERE tenant_id=1 AND requirement_id IN ($taskIdsSql);"
if($taskRemaining -ne "0`r`n0" -and $taskRemaining -ne "0`n0") {throw 'Test rows still exist; keep all archived snapshots.'}
if($taskDrawingPaths.Count -gt 0){$taskDrawingRemaining=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM doormes_local.dm_drawing WHERE tenant_id=1 AND id IN ($taskDrawingIdsSql); SELECT COUNT(*) FROM doormes_local.dm_drawing_version WHERE tenant_id=1 AND drawing_id IN ($taskDrawingIdsSql);";if(($taskDrawingRemaining -split '\r?\n') -join ',' -ne '0,0'){throw 'Drawing rows remain; keep snapshots.'}}
foreach($taskPath in @($taskPaths)+@($taskDrawingPaths)) {
  # Exact paths were resolved and validated above; only this run's UUID snapshots.
  Remove-Item -LiteralPath $taskPath.path -Recurse -Force
}
[IO.File]::WriteAllText((Join-Path $taskScenario 'cleanup.json'),(@{database='doormes_local';tenantId=1;recordIds=@($taskPaths.id);drawingIds=@($taskDrawingPaths.id);archivedAt=(Get-Date).ToString('o');remaining=0;recoverable=$true}|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
"Archived $($taskPaths.Count) controlled test record(s); full JSON and metadata preserved at $taskScenario."
