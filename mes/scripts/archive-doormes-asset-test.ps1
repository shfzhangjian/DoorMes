[CmdletBinding()]param([Parameter(Mandatory)][string]$ScenarioDirectory,[switch]$Apply)
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot
$taskScenario=(Resolve-Path -LiteralPath $ScenarioDirectory).Path
$taskScenarios=[IO.Path]::GetFullPath((Join-Path $taskRoot 'runtime-local/scenarios'))
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0' -or !(Split-Path $taskScenario -Parent).Equals($taskScenarios,[StringComparison]::OrdinalIgnoreCase)){throw 'Only one exact isolated scenario is allowed.'}
function Safe-TaskPath([string]$Path,[string]$Parent){
 $taskFull=[IO.Path]::GetFullPath($Path);$taskBase=[IO.Path]::GetFullPath($Parent)
 if(!$taskFull.StartsWith($taskBase+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Path outside its exact archive/storage root.'}
 $taskCurrent=$taskFull
 while($taskCurrent){if(Test-Path -LiteralPath $taskCurrent){if((Get-Item -LiteralPath $taskCurrent).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Linked paths are not allowed.'}};$taskCurrent=Split-Path $taskCurrent -Parent}
 return $taskFull
}
$null=Safe-TaskPath $taskScenario $taskScenarios
$taskManifest=Get-Content -LiteralPath (Join-Path $taskScenario 'manifest.json') -Raw|ConvertFrom-Json
if($taskManifest.database -ne 'doormes_local' -or $taskManifest.tenantId -ne 1 -or $taskManifest.scenarioId -notmatch '^M1-AST-[0-9]{8}-[0-9]{6}-[a-f0-9]{6}$'){throw 'Invalid controlled asset manifest.'}
if(Test-Path -LiteralPath (Join-Path $taskScenario 'asset-cleanup.json')){throw 'Asset test already archived.'}
$taskIds=@($taskManifest.assetIds|Select-Object -Unique)
if(!$taskIds.Count){throw 'No exact controlled asset identities.'}
foreach($taskId in $taskIds){if($taskId -notmatch '^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$' -or $taskId -notin @("TEST-$($taskManifest.scenarioId)-MODEL","TEST-$($taskManifest.scenarioId)-GLB","TEST-$($taskManifest.scenarioId)-EXTERNAL","MES-TEXTURE-TEST-$($taskManifest.scenarioId)-TEX","MES-TEXTURE-TEST-$($taskManifest.scenarioId)-BAD")){throw 'Asset identity is not owned by this exact scenario.'}}
$taskIdsSql=($taskIds|ForEach-Object {"'$_'"}) -join ','
# Keep every resource referenced by any retained current or historical drawing/catalog version.
$taskDrawingRows=Invoke-DoormesAdminQuery -Sql 'SELECT drawing_id,revision FROM doormes_local.dm_drawing_version WHERE tenant_id=1;'
$taskCatalogRows=Invoke-DoormesAdminQuery -Sql 'SELECT catalog_id,revision FROM doormes_local.dm_material_catalog_version WHERE tenant_id=1;'
foreach($taskKind in @('drawings','catalog')){
 $taskRows=if($taskKind -eq 'drawings'){$taskDrawingRows}else{$taskCatalogRows}
 foreach($taskRow in @($taskRows -split '\r?\n'|Where-Object {$_})){
  $taskValues=$taskRow -split "`t"
  if([Guid]::Parse($taskValues[0]).ToString() -ne $taskValues[0] -or $taskValues[1] -notmatch '^[1-9][0-9]*$'){throw 'Unexpected retained snapshot identity.'}
  $taskFile=Safe-TaskPath (Join-Path $taskRoot "runtime-local/design-data/$taskKind/tenant-1/$($taskValues[0])/r$($taskValues[1]).json") (Join-Path $taskRoot "runtime-local/design-data/$taskKind/tenant-1")
  $taskContent=Get-Content -LiteralPath $taskFile -Raw
  foreach($taskId in $taskIds){if($taskContent.Contains($taskId)){throw 'Retained snapshot references a controlled asset. Archive only its controlled drawing/catalog first; keep all assets.'}}
 }
}
$taskRows=Invoke-DoormesAdminQuery -Sql "SELECT asset_id,blob_id,content_hash,metadata_hash FROM doormes_local.dm_visual_asset WHERE tenant_id=1 AND asset_id IN($taskIdsSql);"
$taskPaths=@()
foreach($taskRow in @($taskRows -split '\r?\n'|Where-Object {$_})){
 $taskValues=$taskRow -split "`t";if($taskValues.Count -ne 4 -or [Guid]::Parse($taskValues[1]).ToString() -ne $taskValues[1]){throw 'Unexpected asset blob identity.'}
 $taskSource=Safe-TaskPath (Join-Path $taskRoot "runtime-local/design-data/assets/tenant-1/$($taskValues[1])") (Join-Path $taskRoot 'runtime-local/design-data/assets/tenant-1')
 $taskDestination=Safe-TaskPath (Join-Path $taskScenario "archived-assets/$($taskValues[1])") $taskScenario
 if(Test-Path -LiteralPath $taskDestination){throw 'Archive destination exists; inspect it instead of overwriting.'}
 if(('sha256:'+(Get-FileHash -LiteralPath (Join-Path $taskSource 'content.bin')).Hash.ToLowerInvariant()) -ne $taskValues[2] -or (Get-FileHash -LiteralPath (Join-Path $taskSource 'descriptor.json')).Hash.ToLowerInvariant() -ne $taskValues[3]){throw 'Source asset hash mismatch; preserve files for investigation.'}
 $taskPaths+=@{assetId=$taskValues[0];blobId=$taskValues[1];source=$taskSource;destination=$taskDestination;contentHash=$taskValues[2];metadataHash=$taskValues[3]}
}
if(!$Apply){"Preview: recoverably archive $($taskPaths.Count) exact resources and expire only this scenario's remaining grants. No manual/shared resource affected.";return}
$taskDump=Invoke-DoormesAdminQuery -Sql "SELECT * FROM doormes_local.dm_visual_asset WHERE tenant_id=1 AND asset_id IN($taskIdsSql); SELECT * FROM doormes_local.dm_visual_asset_upload WHERE tenant_id=1 AND asset_id IN($taskIdsSql);"
[IO.File]::WriteAllText((Join-Path $taskScenario 'assets-before-cleanup.txt'),$taskDump,[Text.UTF8Encoding]::new($false))
$taskRestore=Invoke-DoormesAdminQuery -Sql "SELECT CONCAT('INSERT INTO dm_visual_asset(tenant_id,asset_id,blob_id,kind,media_type,content_hash,byte_length,metadata_hash,uploaded_by,stored_at) VALUES(',tenant_id,',',QUOTE(asset_id),',',QUOTE(blob_id),',',QUOTE(kind),',',QUOTE(media_type),',',QUOTE(content_hash),',',byte_length,',',QUOTE(metadata_hash),',',uploaded_by,',',QUOTE(stored_at),');') FROM doormes_local.dm_visual_asset WHERE tenant_id=1 AND asset_id IN($taskIdsSql);"
[IO.File]::WriteAllText((Join-Path $taskScenario 'assets-restore.sql'),("USE doormes_local;`nSTART TRANSACTION;`n"+$taskRestore+"`nCOMMIT;"),[Text.UTF8Encoding]::new($false))
[IO.Directory]::CreateDirectory((Join-Path $taskScenario 'archived-assets'))|Out-Null
foreach($taskPath in $taskPaths){
 Copy-Item -LiteralPath $taskPath.source -Destination $taskPath.destination -Recurse -ErrorAction Stop
 if(('sha256:'+(Get-FileHash -LiteralPath (Join-Path $taskPath.destination 'content.bin')).Hash.ToLowerInvariant()) -ne $taskPath.contentHash -or (Get-FileHash -LiteralPath (Join-Path $taskPath.destination 'descriptor.json')).Hash.ToLowerInvariant() -ne $taskPath.metadataHash){throw 'Backup verification failed; keep original assets.'}
}
$taskSql="USE doormes_local; START TRANSACTION; DELETE FROM dm_visual_asset_upload WHERE tenant_id=1 AND asset_id IN($taskIdsSql); DELETE FROM dm_visual_asset WHERE tenant_id=1 AND asset_id IN($taskIdsSql); COMMIT;"
[IO.File]::WriteAllText((Join-Path $taskScenario 'assets-cleanup.sql'),$taskSql,[Text.UTF8Encoding]::new($false))
Invoke-DoormesAdminQuery -Sql $taskSql|Out-Null
$taskRemaining=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM doormes_local.dm_visual_asset WHERE tenant_id=1 AND asset_id IN($taskIdsSql); SELECT COUNT(*) FROM doormes_local.dm_visual_asset_upload WHERE tenant_id=1 AND asset_id IN($taskIdsSql);"
if(($taskRemaining -split '\r?\n') -join ',' -ne '0,0'){throw 'Rows remain; preserve original resources.'}
foreach($taskPath in $taskPaths){$taskValidated=Safe-TaskPath $taskPath.source (Join-Path $taskRoot 'runtime-local/design-data/assets/tenant-1');Remove-Item -LiteralPath $taskValidated -Recurse -Force}
[IO.File]::WriteAllText((Join-Path $taskScenario 'asset-cleanup.json'),(@{database='doormes_local';tenantId=1;assets=$taskPaths;archivedAt=(Get-Date).ToString('o');remaining=0;recoverable=$true}|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
"Archived exact controlled resources; verified binary backups and restore SQL at $taskScenario."
