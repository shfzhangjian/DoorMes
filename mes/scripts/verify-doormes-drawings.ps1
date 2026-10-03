[CmdletBinding()]
param([string]$BaseUrl='http://localhost:5180')
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot;$taskUri=[Uri]$BaseUrl
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0' -or $taskUri.Host -notin @('localhost','127.0.0.1','192.168.50.193') -or $taskUri.Port -ne 5180 -or $taskUri.Scheme -ne 'http'){throw 'Unexpected isolated environment.'}
$taskMigration=Get-Content (Join-Path $taskRoot 'runtime-local/design-data/schema-v002.json') -Raw | ConvertFrom-Json
if($taskMigration.database -ne 'doormes_local'){throw 'Unexpected database.'}
$taskAccounts=Get-Content (Join-Path $taskRoot 'runtime-local/factory-bootstrap/accounts.local.json') -Raw | ConvertFrom-Json
$taskRun='M1-DRAW-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6)
$taskArchive=Join-Path $taskRoot "runtime-local/scenarios/$taskRun";[IO.Directory]::CreateDirectory($taskArchive)|Out-Null
Copy-Item -LiteralPath (Join-Path $taskRoot 'tests/scenarios/M1-drawings.v1.json') -Destination (Join-Path $taskArchive 'scenario-case.json')
$taskEvidence=[Collections.Generic.List[object]]::new();$taskIds=[Collections.Generic.List[string]]::new();$taskHeaders=@{};$taskPassed=$false
function Save-TaskJson([string]$Name,[object]$Value){[IO.File]::WriteAllText((Join-Path $taskArchive $Name),($Value|ConvertTo-Json -Depth 75),[Text.UTF8Encoding]::new($false))}
function Invoke-TaskApi([string]$Role,[string]$Path,[string]$Method='Get',[object]$Body=$null) {
  $taskParameters=@{Uri="$BaseUrl/admin-api$Path";Headers=$taskHeaders[$Role];Method=$Method;TimeoutSec=45}
  if($null -ne $Body){$taskParameters.ContentType='application/json;charset=utf-8';$taskParameters.Body=($Body|ConvertTo-Json -Depth 75 -Compress)}
  try{$taskResponse=Invoke-RestMethod @taskParameters}catch{throw "HTTP failed for $Path; sensitive details suppressed."}
  if($Path -eq '/doormes/requirements/create' -and $taskResponse.code -eq 0 -and $taskResponse.data.id){$taskIds.Add($taskResponse.data.id)}
  return $taskResponse
}
function Assert-Task([string]$Id,[bool]$Result,[string]$Description){$taskEvidence.Add(@{id=$Id;result=$(if($Result){'PASS'}else{'FAIL'});description=$Description});if(!$Result){throw "Assertion failed: $Id ($Description)."}}
function Expected-Task([string]$Id,[int]$Code,[object]$Response){Assert-Task $Id ($Response.code -eq $Code) "API code=$($Response.code), expected=$Code"}
function Copy-Task([object]$Value){return ($Value|ConvertTo-Json -Depth 75|ConvertFrom-Json)}
try {
  foreach($taskRole in @('sales','design','reviewer')){
    $taskAccount=@($taskAccounts.accounts|Where-Object username -eq $taskRole)[0];$taskHeaders[$taskRole]=@{'tenant-id'='1'}
    $taskLogin=Invoke-TaskApi $taskRole '/system/auth/login' 'Post' @{username=$taskAccount.username;password=$taskAccount.password}
    if($taskLogin.code -ne 0 -or !$taskLogin.data.accessToken){throw "Login failed for $taskRole."};$taskHeaders[$taskRole].Authorization='Bearer '+$taskLogin.data.accessToken
  }
  $taskDemand=@{schemaVersion='doormes-design-demand.v1';number='TEST-'+$taskRun;customer='绘图接口验证（测试后归档）';project='仅测试';note='不是人工订单';lines=@(@{mark='C1';kind='custom';quantity=2;requirement=@{widthMm=1200;heightMm=1500;material='材料需求自由文本';glass='玻璃要求';hardware='五金要求';finish='RAL7016';dueDate='2026-10-15';note='左内开'}})}
  $taskCreated=Invoke-TaskApi 'sales' '/doormes/requirements/create' 'Post' @{expectedRevision=0;changeNote='测试需求';demand=$taskDemand};Expected-Task 'ASSERT-01' 0 $taskCreated
  $taskId=$taskCreated.data.id;$taskLineId=$taskCreated.data.demand.lines[0].id
  Expected-Task 'ASSERT-02' 0 (Invoke-TaskApi 'sales' "/doormes/requirements/submit?id=$taskId" 'Post' @{expectedRevision=1;note='提交研发'})
  $taskClaimed=Invoke-TaskApi 'design' "/doormes/requirements/claim?id=$taskId" 'Post' @{expectedRevision=2;note='研发领用'};Expected-Task 'ASSERT-03' 0 $taskClaimed
  Save-TaskJson 'source-requirement.json' $taskClaimed.data
  $taskOpenBody=@{requirementId=$taskId;lineId=$taskLineId;expectedRequirementRevision=3}
  $taskFirst=Invoke-TaskApi 'design' '/doormes/drawings/open' 'Post' $taskOpenBody;Expected-Task 'ASSERT-04' 0 $taskFirst
  $taskDrawingId=$taskFirst.data.id;Save-TaskJson 'drawing-r1.json' $taskFirst.data
  Assert-Task 'ASSERT-05' ($taskFirst.data.revision -eq 1 -and $taskFirst.data.requirementId -eq $taskId -and $taskFirst.data.lineId -eq $taskLineId -and $taskFirst.data.document.designId -eq $taskDrawingId -and $taskFirst.data.document.windows[0].quantity -eq 2 -and $taskFirst.data.document.windows[0].widthMm -eq 1200) 'Server identity, source link, seed geometry and quantity'
  $taskRepeat=Invoke-TaskApi 'design' '/doormes/drawings/open' 'Post' $taskOpenBody
  Assert-Task 'ASSERT-06' ($taskRepeat.code -eq 0 -and $taskRepeat.data.id -eq $taskDrawingId -and $taskRepeat.data.revision -eq 1) 'Repeat open is idempotent'
  $taskFound=Invoke-TaskApi 'design' "/doormes/drawings/find?requirementId=$taskId&lineId=$taskLineId"
  Assert-Task 'ASSERT-07' ($taskFound.code -eq 0 -and $taskFound.data.id -eq $taskDrawingId) 'Find by stable demand line'
  $taskChanged=Copy-Task $taskFirst.data.document;$taskChanged.windows[0].widthMm=1250
  $taskSecond=Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=1;changeNote='测试宽度调整1250';document=$taskChanged};Expected-Task 'ASSERT-08' 0 $taskSecond
  Assert-Task 'ASSERT-09' ($taskSecond.data.revision -eq 2 -and $taskSecond.data.document.windows[0].widthMm -eq 1250 -and $taskSecond.data.id -eq $taskDrawingId) 'Real save advances immutable business version'
  Save-TaskJson 'drawing-r2.json' $taskSecond.data
  $taskOld=Invoke-TaskApi 'design' "/doormes/drawings/get?id=$taskDrawingId&revision=1"
  Assert-Task 'ASSERT-10' ($taskOld.code -eq 0 -and $taskOld.data.document.windows[0].widthMm -eq 1200) 'Historical drawing unchanged'
  Expected-Task 'ASSERT-11' 409 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=1;changeNote='过期修订';document=$taskChanged})
  $taskInvalid=Copy-Task $taskChanged;$taskInvalid.windows[0].widthMm=0
  Expected-Task 'ASSERT-12' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=2;changeNote='无效尺寸';document=$taskInvalid})
  $taskInvalid=Copy-Task $taskChanged;$taskInvalid.windows+=@(Copy-Task $taskInvalid.windows[0])
  Expected-Task 'ASSERT-13' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=2;changeNote='重复ID';document=$taskInvalid})
  $taskInvalid=Copy-Task $taskChanged;$taskInvalid.designId=[Guid]::NewGuid().ToString()
  Expected-Task 'ASSERT-14' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=2;changeNote='篡改图纸身份';document=$taskInvalid})
  Expected-Task 'ASSERT-15' 404 (Invoke-TaskApi 'design' '/doormes/drawings/open' 'Post' @{requirementId=$taskId;lineId=[Guid]::NewGuid().ToString();expectedRequirementRevision=3})
  Expected-Task 'ASSERT-16' 403 (Invoke-TaskApi 'sales' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=2;changeNote='销售越权保存';document=$taskChanged})
  Expected-Task 'ASSERT-17' 403 (Invoke-TaskApi 'reviewer' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=2;changeNote='审核越权保存';document=$taskChanged})
  Expected-Task 'ASSERT-18' 0 (Invoke-TaskApi 'reviewer' "/doormes/drawings/get?id=$taskDrawingId")
  $taskSource=Invoke-TaskApi 'sales' "/doormes/requirements/get?id=$taskId"
  Assert-Task 'ASSERT-19' ($taskSource.code -eq 0 -and $taskSource.data.revision -eq 3 -and $taskSource.data.demand.lines[0].requirement.widthMm -eq 1200) 'Drawing edits never mutate sales requirement baseline'
  $taskVersions=Invoke-TaskApi 'design' "/doormes/drawings/versions?id=$taskDrawingId"
  Assert-Task 'ASSERT-20' ($taskVersions.code -eq 0 -and @($taskVersions.data).Count -eq 2) 'Rejected saves do not add versions'
  $taskDb=Invoke-DoormesAdminQuery -Sql "SELECT revision,line_id,requirement_revision FROM doormes_local.dm_drawing WHERE tenant_id=1 AND id='$taskDrawingId'; SELECT revision,sha256 FROM doormes_local.dm_drawing_version WHERE tenant_id=1 AND drawing_id='$taskDrawingId' ORDER BY revision;"
  [IO.File]::WriteAllText((Join-Path $taskArchive 'database-evidence.txt'),$taskDb,[Text.UTF8Encoding]::new($false))
  Assert-Task 'ASSERT-21' ($taskDb.StartsWith("2`t$taskLineId`t3")) 'SQL head and source baseline match API'
  $taskHashes=@($taskDb -split '\r?\n'|Select-Object -Skip 1)
  for($taskRev=1;$taskRev -le 2;$taskRev++){
    $taskFile=Join-Path $taskRoot "runtime-local/design-data/drawings/tenant-1/$taskDrawingId/r$taskRev.json"
    Assert-Task "ASSERT-FILE-0$taskRev" ((Get-FileHash -LiteralPath $taskFile).Hash.ToLowerInvariant() -eq ($taskHashes[$taskRev-1] -split "`t")[1]) "Drawing R$taskRev file SHA-256 matches SQL"
  }
  $taskPassed=$true
} finally {
  Save-TaskJson 'manifest.json' @{scenarioId=$taskRun;tenantId=1;database='doormes_local';ids=@($taskIds);baseUrl=$BaseUrl;passed=$taskPassed;browserUsed=$false;testedAt=(Get-Date).ToString('o');assertions=@($taskEvidence)}
  foreach($taskRole in $taskHeaders.Keys){if($taskHeaders[$taskRole].Authorization){try{$null=Invoke-TaskApi $taskRole '/system/auth/logout' 'Post' @{}}catch{Write-Warning "Logout failed for $taskRole."}}}
}
"Drawing scenario passed; evidence: $taskArchive. Archive only this controlled test using the cleanup script."
