[CmdletBinding(DefaultParameterSetName='Verify')]
param(
  [Parameter(ParameterSetName='Verify')][string]$BaseUrl='http://localhost:5180',
  [Parameter(Mandatory,ParameterSetName='Archive')][switch]$ArchiveOnly,
  [Parameter(Mandatory,ParameterSetName='Archive')][string]$ScenarioDirectory,
  [Parameter(ParameterSetName='Archive')][switch]$ApplyCleanup
)
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0'){throw 'Unexpected isolated workspace.'}
function Write-TaskJson([string]$Path,[object]$Value){[IO.File]::WriteAllText($Path,($Value|ConvertTo-Json -Depth 90),[Text.UTF8Encoding]::new($false))}
function Guard-TaskPath([string]$Path,[string]$Root){
  $taskAbsolute=[IO.Path]::GetFullPath($Path);$taskBoundary=[IO.Path]::GetFullPath($Root).TrimEnd('\')+'\'
  if(!$taskAbsolute.StartsWith($taskBoundary,[StringComparison]::OrdinalIgnoreCase)){throw 'Target escaped the exact scenario storage root.'}
  for($taskPart=$taskAbsolute;$taskPart;$taskPart=Split-Path -Parent $taskPart){
    if(Test-Path -LiteralPath $taskPart){if((Get-Item -LiteralPath $taskPart -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'Scenario paths must not traverse links.'}}
  }
  return $taskAbsolute
}
function Shared-TaskCounts {
  return Invoke-DoormesAdminQuery -Sql "SELECT 'users',COUNT(*) FROM doormes_local.system_users; SELECT 'departments',COUNT(*) FROM doormes_local.system_dept; SELECT 'requirements',COUNT(*) FROM doormes_local.dm_design_requirement; SELECT 'catalog',COUNT(*) FROM doormes_local.dm_material_catalog; SELECT 'assets',COUNT(*) FROM doormes_local.dm_visual_asset;"
}

if($ArchiveOnly){
  $taskScenarios=[IO.Path]::GetFullPath((Join-Path $taskRoot 'runtime-local/scenarios'))
  $taskScenario=(Resolve-Path -LiteralPath $ScenarioDirectory).Path
  if(!(Split-Path -Parent $taskScenario).Equals($taskScenarios,[StringComparison]::OrdinalIgnoreCase)){throw 'Only one direct scenario archive directory is permitted.'}
  $taskScenario=Guard-TaskPath $taskScenario $taskScenarios
  $taskManifest=Get-Content -LiteralPath (Join-Path $taskScenario 'manifest.json') -Raw|ConvertFrom-Json
  if($taskManifest.scenarioId -notmatch '^M1-DSN-[0-9]{8}-[0-9]{6}-[a-f0-9]{6}$' -or $taskManifest.database -ne 'doormes_local' -or $taskManifest.tenantId -ne 1){throw 'Invalid independent-design test manifest.'}
  if(Test-Path -LiteralPath (Join-Path $taskScenario 'cleanup.json')){throw 'This exact scenario is already archived.'}
  $taskPaths=@();$taskGuards=@();$taskDrawingRoot=Join-Path $taskRoot 'runtime-local/design-data/drawings/tenant-1'
  foreach($taskId in @($taskManifest.drawingIds)){
    if([Guid]::Parse($taskId).ToString() -ne $taskId){throw 'Invalid drawing UUID.'}
    $taskHead=Invoke-DoormesAdminQuery -Sql "SELECT drawing_number,revision,created_by,source_type,IFNULL(requirement_id,'NULL'),IFNULL(line_id,'NULL') FROM doormes_local.dm_drawing WHERE tenant_id=1 AND id='$taskId';"
    $taskParts=$taskHead -split "`t"
    if($taskParts.Count -ne 6 -or $taskParts[0] -notmatch ('^TEST-'+[regex]::Escape($taskManifest.scenarioId)+'(?:-RENAMED|-B)?$') -or $taskParts[1] -notmatch '^[1-9][0-9]*$' -or $taskParts[2] -notin @('1001','1003') -or $taskParts[3] -ne 'INDEPENDENT' -or $taskParts[4] -ne 'NULL' -or $taskParts[5] -ne 'NULL'){throw 'Drawing no longer belongs exclusively to this controlled independent-design scenario.'}
    $taskDirectory=Guard-TaskPath (Join-Path $taskDrawingRoot $taskId) $taskDrawingRoot
    if(!(Test-Path -LiteralPath $taskDirectory -PathType Container)){throw 'Snapshot directory is missing; cleanup is forbidden.'}
    $taskVersionRows=Invoke-DoormesAdminQuery -Sql "SELECT revision,sha256 FROM doormes_local.dm_drawing_version WHERE tenant_id=1 AND drawing_id='$taskId' ORDER BY revision;"
    $taskVersions=@($taskVersionRows -split '\r?\n'|Where-Object {$_})
    if($taskVersions.Count -ne [int]$taskParts[1]){throw 'Version inventory is incomplete; preserve all rows.'}
    foreach($taskRow in $taskVersions){
      $taskVersion=$taskRow -split "`t";$taskFile=Guard-TaskPath (Join-Path $taskDirectory "r$($taskVersion[0]).json") $taskDrawingRoot
      if($taskVersion.Count -ne 2 -or $taskVersion[0] -notmatch '^[1-9][0-9]*$' -or (Get-FileHash -LiteralPath $taskFile).Hash.ToLowerInvariant() -ne $taskVersion[1]){throw 'Snapshot hashes are not verified; cleanup is forbidden.'}
    }
    $taskPaths+=@{id=$taskId;path=$taskDirectory;destination=(Join-Path $taskScenario "archived-drawings/$taskId");versions=$taskVersions}
    $taskGuards+="(id='$taskId' AND drawing_number='$($taskParts[0])' AND revision=$($taskParts[1]) AND created_by=$($taskParts[2]) AND source_type='INDEPENDENT' AND requirement_id IS NULL AND line_id IS NULL)"
  }
  if(!$taskPaths.Count -or @($taskPaths.id|Select-Object -Unique).Count -ne $taskPaths.Count){throw 'No unique exact scenario drawings to archive.'}
  if(!$ApplyCleanup){"Preview only: recoverably archive $($taskPaths.Count) exact TEST independent drawings. No users, organizations, catalog items, assets or demand records will be changed.";return}
  $taskIdsSql=($taskPaths|ForEach-Object {"'$($_.id)'"}) -join ','
  $taskSharedBefore=Shared-TaskCounts
  $taskDump=Invoke-DoormesAdminQuery -Sql "SELECT * FROM doormes_local.dm_drawing WHERE tenant_id=1 AND id IN($taskIdsSql); SELECT * FROM doormes_local.dm_drawing_version WHERE tenant_id=1 AND drawing_id IN($taskIdsSql);"
  [IO.File]::WriteAllText((Join-Path $taskScenario 'before-cleanup.txt'),$taskDump,[Text.UTF8Encoding]::new($false))
  $taskRestore=Invoke-DoormesAdminQuery -Sql @"
SELECT CONCAT('INSERT INTO dm_drawing(id,tenant_id,requirement_id,requirement_revision,line_id,revision,status,created_by,updated_at,drawing_number,name,note,source_type,created_at,create_request_id,create_payload_hash) VALUES(',QUOTE(id),',',tenant_id,',',IFNULL(QUOTE(requirement_id),'NULL'),',',requirement_revision,',',IFNULL(QUOTE(line_id),'NULL'),',',revision,',',QUOTE(status),',',created_by,',',QUOTE(updated_at),',',QUOTE(drawing_number),',',QUOTE(name),',',QUOTE(note),',',QUOTE(source_type),',',QUOTE(created_at),',',IFNULL(QUOTE(create_request_id),'NULL'),',',IFNULL(QUOTE(create_payload_hash),'NULL'),');') FROM doormes_local.dm_drawing WHERE tenant_id=1 AND id IN($taskIdsSql);
SELECT CONCAT('INSERT INTO dm_drawing_version(tenant_id,drawing_id,revision,json_path,sha256,change_note,changed_by,updated_at) VALUES(',tenant_id,',',QUOTE(drawing_id),',',revision,',',QUOTE(json_path),',',QUOTE(sha256),',',QUOTE(change_note),',',changed_by,',',QUOTE(updated_at),');') FROM doormes_local.dm_drawing_version WHERE tenant_id=1 AND drawing_id IN($taskIdsSql) ORDER BY drawing_id,revision;
"@
  [IO.File]::WriteAllText((Join-Path $taskScenario 'restore.sql'),("USE doormes_local;`nSET NAMES utf8mb4;`nSTART TRANSACTION;`n"+$taskRestore+"`nCOMMIT;"),[Text.UTF8Encoding]::new($false))
  [IO.Directory]::CreateDirectory((Join-Path $taskScenario 'archived-drawings'))|Out-Null
  foreach($taskPath in $taskPaths){
    if(Test-Path -LiteralPath $taskPath.destination){throw 'Archive target already exists; no files will be overwritten.'}
    Copy-Item -LiteralPath $taskPath.path -Destination $taskPath.destination -Recurse -ErrorAction Stop
    foreach($taskRow in $taskPath.versions){$taskFields=$taskRow -split "`t";if((Get-FileHash -LiteralPath (Join-Path $taskPath.destination "r$($taskFields[0]).json")).Hash.ToLowerInvariant() -ne $taskFields[1]){throw 'Archive copy hash mismatch; database preserved.'}}
  }
  # Lock and recheck every exact head in the same transaction. Concurrent edits abort all deletion.
  $taskPredicate=$taskGuards -join ' OR '
  $taskCleanup=@"
USE doormes_local;
START TRANSACTION;
SELECT id FROM dm_drawing WHERE tenant_id=1 AND id IN($taskIdsSql) FOR UPDATE;
SET @doormes_dsn_exact=(SELECT COUNT(*) FROM dm_drawing WHERE tenant_id=1 AND ($taskPredicate));
DELETE FROM dm_drawing_version WHERE tenant_id=1 AND drawing_id IN($taskIdsSql) AND @doormes_dsn_exact=$($taskPaths.Count);
DELETE FROM dm_drawing WHERE tenant_id=1 AND id IN($taskIdsSql) AND @doormes_dsn_exact=$($taskPaths.Count);
COMMIT;
"@
  [IO.File]::WriteAllText((Join-Path $taskScenario 'cleanup.sql'),$taskCleanup,[Text.UTF8Encoding]::new($false))
  Invoke-DoormesAdminQuery -Sql $taskCleanup|Out-Null
  $taskRemaining=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM doormes_local.dm_drawing WHERE tenant_id=1 AND id IN($taskIdsSql); SELECT COUNT(*) FROM doormes_local.dm_drawing_version WHERE tenant_id=1 AND drawing_id IN($taskIdsSql);"
  if(($taskRemaining -split '\r?\n') -join ',' -ne '0,0'){throw 'Exact head guard did not match or rows remain. Keep all archived and original snapshots.'}
  foreach($taskPath in $taskPaths){$taskVerified=Guard-TaskPath $taskPath.path $taskDrawingRoot;Remove-Item -LiteralPath $taskVerified -Recurse -Force}
  $taskSharedAfter=Shared-TaskCounts
  Write-TaskJson (Join-Path $taskScenario 'cleanup.json') @{scenarioId=$taskManifest.scenarioId;database='doormes_local';tenantId=1;drawingIds=@($taskPaths.id);remaining=0;recoverable=$true;archivedAt=(Get-Date).ToString('o');sharedBefore=$taskSharedBefore;sharedAfter=$taskSharedAfter;sharedCountsUnchanged=($taskSharedBefore -eq $taskSharedAfter)}
  "Archived $($taskPaths.Count) exact controlled independent drawings, with full JSON versions and restoring SQL at $taskScenario."
  return
}

$taskUri=[Uri]$BaseUrl
if($taskUri.Host -notin @('localhost','127.0.0.1','192.168.50.193') -or $taskUri.Port -ne 5180 -or $taskUri.Scheme -ne 'http' -or $taskUri.AbsolutePath -ne '/' -or $taskUri.Query){throw 'Only the isolated local/LAN frontend origin is allowed.'}
$BaseUrl=$BaseUrl.TrimEnd('/')
$taskSchema=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='doormes_local' AND table_name='dm_drawing' AND column_name IN('drawing_number','name','note','source_type','created_at','create_request_id','create_payload_hash');"
if($taskSchema -ne '7'){throw 'V005 independent drawing schema is not deployed; nothing will be created.'}
$taskAccounts=Get-Content -LiteralPath (Join-Path $taskRoot 'runtime-local/factory-bootstrap/accounts.local.json') -Raw|ConvertFrom-Json
$taskRun='M1-DSN-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6)
$taskArchive=Join-Path $taskRoot "runtime-local/scenarios/$taskRun";[IO.Directory]::CreateDirectory($taskArchive)|Out-Null
Copy-Item -LiteralPath (Join-Path $taskRoot 'docs/scenarios/M1-independent-design.v1.md') -Destination (Join-Path $taskArchive 'scenario-case.md')
$taskEvidence=[Collections.Generic.List[object]]::new();$taskDrawingIds=[Collections.Generic.List[string]]::new();$taskHeaders=@{};$taskPassed=$false;$taskSharedBefore=Shared-TaskCounts
function Save-TaskJson([string]$Name,[object]$Value){Write-TaskJson (Join-Path $taskArchive $Name) $Value}
function Invoke-TaskApi([string]$Role,[string]$Path,[string]$Method='Get',[object]$Body=$null){
  $taskParameters=@{Uri="$BaseUrl/admin-api$Path";Headers=$taskHeaders[$Role];Method=$Method;TimeoutSec=45}
  if($null -ne $Body){$taskParameters.ContentType='application/json;charset=utf-8';$taskParameters.Body=($Body|ConvertTo-Json -Depth 90 -Compress)}
  try{$taskResponse=Invoke-RestMethod @taskParameters}catch{throw "HTTP failed for $Path; sensitive details suppressed."}
  if($Path -eq '/doormes/drawings/create' -and $taskResponse.code -eq 0 -and $taskResponse.data.id -and !$taskDrawingIds.Contains([string]$taskResponse.data.id)){$taskDrawingIds.Add($taskResponse.data.id)}
  return $taskResponse
}
function Assert-Task([string]$Id,[bool]$Result,[string]$Description){$taskEvidence.Add(@{id=$Id;result=$(if($Result){'PASS'}else{'FAIL'});description=$Description});if(!$Result){throw "Assertion failed: $Id ($Description)."}}
function Expected-Task([string]$Id,[int]$Code,[object]$Response){Assert-Task $Id ($Response.code -eq $Code) "API code=$($Response.code), expected=$Code"}
function Copy-Task([object]$Value){return ($Value|ConvertTo-Json -Depth 90|ConvertFrom-Json)}
function Encode-Task([string]$Value){return [Uri]::EscapeDataString($Value)}
try{
  foreach($taskRole in @('admin','design','sales','reviewer')){
    $taskAccount=@($taskAccounts.accounts|Where-Object username -eq $taskRole)[0];$taskHeaders[$taskRole]=@{'tenant-id'='1'}
    $taskLogin=Invoke-TaskApi $taskRole '/system/auth/login' 'Post' @{username=$taskAccount.username;password=$taskAccount.password}
    if($taskLogin.code -ne 0 -or !$taskLogin.data.accessToken){throw "Login failed for $taskRole."}
    $taskHeaders[$taskRole].Authorization='Bearer '+$taskLogin.data.accessToken
  }
  $taskNumber="TEST-$taskRun";$taskRequest=[Guid]::NewGuid().ToString()
  $taskCreate=@{number=$taskNumber;name='独立研发测试图';note='受控场景，不是人工订单';mark='C1';quantity=2;widthMm=1200;heightMm=1500;clientRequestId=$taskRequest}
  Save-TaskJson 'create-input.json' $taskCreate
  Expected-Task 'ASSERT-01' 403 (Invoke-TaskApi 'sales' '/doormes/drawings/create' 'Post' $taskCreate)
  Expected-Task 'ASSERT-02' 403 (Invoke-TaskApi 'reviewer' '/doormes/drawings/create' 'Post' $taskCreate)
  $taskFirst=Invoke-TaskApi 'design' '/doormes/drawings/create' 'Post' $taskCreate;Expected-Task 'ASSERT-03' 0 $taskFirst
  $taskId=$taskFirst.data.id;Save-TaskJson 'drawing-r1.json' $taskFirst.data
  Assert-Task 'ASSERT-04' ($taskFirst.data.revision -eq 1 -and $null -eq $taskFirst.data.requirementId -and $null -eq $taskFirst.data.lineId -and $taskFirst.data.requirementRevision -eq 0 -and $taskFirst.data.metadata.source -eq 'INDEPENDENT' -and $taskFirst.data.metadata.number -eq $taskNumber -and $taskFirst.data.editable -and $taskFirst.data.document.designId -eq $taskId -and $taskFirst.data.document.windows[0].widthMm -eq 1200 -and $taskFirst.data.document.windows[0].quantity -eq 2) 'Independent creation, business metadata, permission flag and engine geometry persist without a demand'
  $taskRepeat=Invoke-TaskApi 'design' '/doormes/drawings/create' 'Post' $taskCreate
  Assert-Task 'ASSERT-05' ($taskRepeat.code -eq 0 -and $taskRepeat.data.id -eq $taskId -and $taskRepeat.data.revision -eq 1) 'Same client request is idempotent'
  $taskAltered=Copy-Task $taskCreate;$taskAltered.heightMm=1600
  Expected-Task 'ASSERT-06' 409 (Invoke-TaskApi 'design' '/doormes/drawings/create' 'Post' $taskAltered)
  $taskDuplicate=Copy-Task $taskCreate;$taskDuplicate.number=$taskNumber.ToLowerInvariant();$taskDuplicate.clientRequestId=[Guid]::NewGuid().ToString()
  Expected-Task 'ASSERT-07' 409 (Invoke-TaskApi 'design' '/doormes/drawings/create' 'Post' $taskDuplicate)
  $taskRead=Invoke-TaskApi 'reviewer' "/doormes/drawings/get?id=$taskId"
  Assert-Task 'ASSERT-08' ($taskRead.code -eq 0 -and !$taskRead.data.editable) 'Read-only reviewer can load but cannot edit'
  $taskChanged=Copy-Task $taskFirst.data.document;$taskChanged.windows[0].widthMm=1250
  $taskMetadata=@{number="$taskNumber-RENAMED";name='独立研发修订图';note='宽度调整至1250'}
  $taskSecond=Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=1;changeNote='尺寸和编号共同修订';document=$taskChanged;metadata=$taskMetadata};Expected-Task 'ASSERT-09' 0 $taskSecond
  Save-TaskJson 'drawing-r2.json' $taskSecond.data
  Assert-Task 'ASSERT-10' ($taskSecond.data.revision -eq 2 -and $taskSecond.data.metadata.number -eq "$taskNumber-RENAMED" -and $taskSecond.data.metadata.note -eq '宽度调整至1250' -and $taskSecond.data.document.windows[0].widthMm -eq 1250) 'Drawing number, note and geometry are versioned atomically'
  $taskOld=Invoke-TaskApi 'design' "/doormes/drawings/get?id=$taskId&revision=1"
  Assert-Task 'ASSERT-11' ($taskOld.code -eq 0 -and $taskOld.data.metadata.number -eq $taskNumber -and $taskOld.data.document.windows[0].widthMm -eq 1200 -and !$taskOld.data.editable) 'Historical version is immutable and read-only'
  $taskRepeat=Invoke-TaskApi 'design' '/doormes/drawings/create' 'Post' $taskCreate
  Assert-Task 'ASSERT-12' ($taskRepeat.code -eq 0 -and $taskRepeat.data.id -eq $taskId -and $taskRepeat.data.revision -eq 2) 'Original create retry returns current head after later edits'
  Expected-Task 'ASSERT-13' 409 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=1;changeNote='过期版本';document=$taskChanged})
  $taskInvalid=Copy-Task $taskChanged;$taskInvalid.windows[0].widthMm=0
  Expected-Task 'ASSERT-14' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=2;changeNote='无效尺寸';document=$taskInvalid})
  $taskInvalid=Copy-Task $taskChanged;$taskInvalid.windows+=@(Copy-Task $taskInvalid.windows[0])
  Expected-Task 'ASSERT-15' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=2;changeNote='重复内部图元ID';document=$taskInvalid})
  $taskInvalid=Copy-Task $taskChanged;$taskInvalid.designId=[Guid]::NewGuid().ToString()
  Expected-Task 'ASSERT-16' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=2;changeNote='篡改图纸身份';document=$taskInvalid})
  $taskInvalid=Copy-Task $taskChanged;$taskInvalid.windows[0].visualConfiguration.appearance.frame.outside.appearanceId='MES-CATALOG:'+([Guid]::NewGuid().ToString());$taskInvalid.windows[0].visualConfiguration.appearance.frame.outside.appearanceVersion='invalid'
  Expected-Task 'ASSERT-17' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=2;changeNote='非法目录版本';document=$taskInvalid})
  $taskInvalid=Copy-Task $taskChanged;$taskInvalid|Add-Member -NotePropertyName 'validationProbe' -NotePropertyValue @{kind='gltf';assetId='../invalid';contentHash='sha256:'+('a'*64)}
  Expected-Task 'ASSERT-18' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=2;changeNote='原始数据非法资产引用';document=$taskInvalid})
  $taskIllegalSource=Copy-Task $taskMetadata;$taskIllegalSource|Add-Member -NotePropertyName source -NotePropertyValue 'REQUIREMENT'
  Expected-Task 'ASSERT-19' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=2;changeNote='篡改来源';document=$taskChanged;metadata=$taskIllegalSource})
  Expected-Task 'ASSERT-20' 403 (Invoke-TaskApi 'sales' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=2;changeNote='销售越权';document=$taskChanged})
  Expected-Task 'ASSERT-21' 403 (Invoke-TaskApi 'reviewer' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=2;changeNote='审核越权';document=$taskChanged})
  $taskThird=Invoke-TaskApi 'admin' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=2;changeNote='管理员代修备注';document=$taskChanged;metadata=@{number="$taskNumber-RENAMED";name='独立研发修订图';note='管理员复核'}};Expected-Task 'ASSERT-22' 0 $taskThird
  Save-TaskJson 'drawing-r3.json' $taskThird.data
  Assert-Task 'ASSERT-23' ($taskThird.data.revision -eq 3 -and $taskThird.data.createdBy -eq 1003 -and $taskThird.data.changedBy -eq 1001) 'Admin save preserves creator and records changing actor'
  $taskOtherCreate=Copy-Task $taskCreate;$taskOtherCreate.number="$taskNumber-B";$taskOtherCreate.name="$taskRun%_图"
  $taskOther=Invoke-TaskApi 'admin' '/doormes/drawings/create' 'Post' $taskOtherCreate;Expected-Task 'ASSERT-24' 0 $taskOther
  $taskOtherId=$taskOther.data.id;Save-TaskJson 'drawing-other-r1.json' $taskOther.data
  Assert-Task 'ASSERT-25' ($taskOtherId -ne $taskId -and $taskOther.data.createdBy -eq 1001) 'Same client-request UUID is scoped to its creator'
  $taskOtherRead=Invoke-TaskApi 'design' "/doormes/drawings/get?id=$taskOtherId"
  Assert-Task 'ASSERT-26' ($taskOtherRead.code -eq 0 -and !$taskOtherRead.data.editable) 'Design role does not own another creator independent drawing'
  Expected-Task 'ASSERT-27' 403 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskOtherId" 'Put' @{expectedRevision=1;changeNote='非所有者越权';document=$taskOther.data.document})
  Expected-Task 'ASSERT-28' 409 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskId" 'Put' @{expectedRevision=3;changeNote='改名冲突';document=$taskChanged;metadata=@{number="$taskNumber-B";name='冲突';note=''}})
  $taskPage=Invoke-TaskApi 'design' ("/doormes/drawings/page?keyword="+(Encode-Task $taskRun)+'&source=INDEPENDENT&mine=false&pageNo=1&pageSize=1')
  Assert-Task 'ASSERT-29' ($taskPage.code -eq 0 -and $taskPage.data.total -eq 2 -and @($taskPage.data.list).Count -eq 1 -and $taskPage.data.list[0].id -eq $taskOtherId -and !$taskPage.data.list[0].editable) 'List orders newest updates first and paginates, with actor-specific editability'
  $taskPage=Invoke-TaskApi 'design' ("/doormes/drawings/page?keyword="+(Encode-Task $taskRun)+'&source=INDEPENDENT&mine=false&pageNo=2&pageSize=1')
  Assert-Task 'ASSERT-30' ($taskPage.code -eq 0 -and $taskPage.data.list[0].id -eq $taskId -and $taskPage.data.list[0].revision -eq 3 -and $taskPage.data.list[0].editable) 'Second page metadata matches the current saved head'
  $taskPage=Invoke-TaskApi 'design' ("/doormes/drawings/page?keyword="+(Encode-Task $taskRun)+'&mine=true&pageNo=1&pageSize=20')
  Assert-Task 'ASSERT-31' ($taskPage.code -eq 0 -and $taskPage.data.total -eq 1 -and $taskPage.data.list[0].id -eq $taskId) 'Only mine filters by creator for independent designs'
  $taskPage=Invoke-TaskApi 'design' ("/doormes/drawings/page?keyword="+(Encode-Task $taskRun)+'&source=REQUIREMENT&pageNo=1&pageSize=20')
  Assert-Task 'ASSERT-32' ($taskPage.code -eq 0 -and $taskPage.data.total -eq 0) 'Requirement-source filter excludes independent drawing records'
  $taskPage=Invoke-TaskApi 'design' ("/doormes/drawings/page?keyword="+(Encode-Task ($taskRun+'%'))+'&pageNo=1&pageSize=20')
  Assert-Task 'ASSERT-33' ($taskPage.code -eq 0 -and $taskPage.data.total -eq 1 -and $taskPage.data.list[0].id -eq $taskOtherId) 'Percent in a keyword is a literal, not a SQL wildcard'
  $taskPage=Invoke-TaskApi 'design' ("/doormes/drawings/page?keyword="+(Encode-Task ($taskRun+'_'))+'&pageNo=1&pageSize=20')
  Assert-Task 'ASSERT-34' ($taskPage.code -eq 0 -and $taskPage.data.total -eq 0) 'Underscore in a keyword is also a literal'
  Expected-Task 'ASSERT-35' 400 (Invoke-TaskApi 'design' '/doormes/drawings/page?source=UNKNOWN&pageNo=1&pageSize=20')
  Expected-Task 'ASSERT-36' 400 (Invoke-TaskApi 'design' '/doormes/drawings/page?pageNo=0&pageSize=20')
  $taskFinal=Invoke-TaskApi 'design' "/doormes/drawings/get?id=$taskId"
  Assert-Task 'ASSERT-37' ($taskFinal.code -eq 0 -and $taskFinal.data.revision -eq 3 -and $taskFinal.data.metadata.note -eq '管理员复核' -and $taskFinal.data.document.windows[0].widthMm -eq 1250) 'Reload recovers current business metadata and complete geometry'
  $taskVersions=Invoke-TaskApi 'design' "/doormes/drawings/versions?id=$taskId"
  Assert-Task 'ASSERT-38' ($taskVersions.code -eq 0 -and @($taskVersions.data).Count -eq 3 -and $taskVersions.data[0].revision -eq 3) 'Rejected saves create no extra immutable revisions'
  Save-TaskJson 'drawing-reloaded.json' $taskFinal.data;Save-TaskJson 'versions.json' $taskVersions.data
  $taskIdsSql=(@($taskDrawingIds)|ForEach-Object {"'$_'"}) -join ','
  $taskHeads=Invoke-DoormesAdminQuery -Sql "SELECT id,revision,drawing_number,source_type,IFNULL(requirement_id,'NULL'),IFNULL(line_id,'NULL'),requirement_revision FROM doormes_local.dm_drawing WHERE tenant_id=1 AND id IN($taskIdsSql) ORDER BY id;"
  $taskHashRows=Invoke-DoormesAdminQuery -Sql "SELECT drawing_id,revision,sha256,json_path FROM doormes_local.dm_drawing_version WHERE tenant_id=1 AND drawing_id IN($taskIdsSql) ORDER BY drawing_id,revision;"
  [IO.File]::WriteAllText((Join-Path $taskArchive 'database-evidence.txt'),($taskHeads+"`n"+$taskHashRows),[Text.UTF8Encoding]::new($false))
  $taskHeadRows=@($taskHeads -split '\r?\n'|Where-Object {$_});$taskHeadCorrect=$taskHeadRows.Count -eq 2
  foreach($taskRow in $taskHeadRows){$taskFields=$taskRow -split "`t";if($taskFields[3] -ne 'INDEPENDENT' -or $taskFields[4] -ne 'NULL' -or $taskFields[5] -ne 'NULL' -or $taskFields[6] -ne '0'){$taskHeadCorrect=$false}}
  Assert-Task 'ASSERT-39' $taskHeadCorrect 'SQL stores two independent heads and no demand/line association'
  $taskHashes=@($taskHashRows -split '\r?\n'|Where-Object {$_});Assert-Task 'ASSERT-40' ($taskHashes.Count -eq 4) 'SQL stores exactly R1/R2/R3 and the other drawing R1'
  foreach($taskRow in $taskHashes){
    $taskFields=$taskRow -split "`t";$taskFile=Guard-TaskPath (Join-Path $taskRoot "runtime-local/design-data/drawings/tenant-1/$($taskFields[0])/r$($taskFields[1]).json") (Join-Path $taskRoot 'runtime-local/design-data/drawings/tenant-1')
    Assert-Task ("ASSERT-FILE-"+$taskFields[0]+'-R'+$taskFields[1]) ((Get-FileHash -LiteralPath $taskFile).Hash.ToLowerInvariant() -eq $taskFields[2]) 'Persisted immutable JSON SHA-256 matches SQL'
  }
  $taskSharedAfter=Shared-TaskCounts
  Assert-Task 'ASSERT-41' ($taskSharedBefore -eq $taskSharedAfter) 'Users, organizations, demands, material catalog and assets are unchanged by this scenario'
  $taskPassed=$true
}finally{
  Save-TaskJson 'manifest.json' @{scenarioId=$taskRun;tenantId=1;database='doormes_local';drawingIds=@($taskDrawingIds);baseUrl=$BaseUrl;passed=$taskPassed;browserUsed=$false;testedAt=(Get-Date).ToString('o');sharedBefore=$taskSharedBefore;assertions=@($taskEvidence)}
  foreach($taskRole in $taskHeaders.Keys){if($taskHeaders[$taskRole].Authorization){try{$null=Invoke-TaskApi $taskRole '/system/auth/logout' 'Post' @{}}catch{Write-Warning "Logout failed for $taskRole."}}}
}
"Independent design scenario passed; evidence: $taskArchive. Use -ArchiveOnly -ScenarioDirectory with preview, then -ApplyCleanup to recoverably clean only these TEST drawings."
