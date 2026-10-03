[CmdletBinding()]
param([string]$BaseUrl='http://localhost:5180')
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot
$taskUri=[Uri]$BaseUrl
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0' -or $taskUri.Host -notin @('localhost','127.0.0.1','192.168.50.193') -or $taskUri.Port -ne 5180 -or $taskUri.Scheme -ne 'http') { throw 'Unexpected isolated environment.' }
$taskMigration=Get-Content (Join-Path $taskRoot 'runtime-local/design-data/schema-v001.json') -Raw | ConvertFrom-Json
if($taskMigration.database -ne 'doormes_local') { throw 'Unexpected database.' }
$taskAccounts=Get-Content (Join-Path $taskRoot 'runtime-local/factory-bootstrap/accounts.local.json') -Raw | ConvertFrom-Json
$taskRun='M1-REQ-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6)
$taskArchive=Join-Path $taskRoot "runtime-local/scenarios/$taskRun"
[IO.Directory]::CreateDirectory($taskArchive) | Out-Null
Copy-Item -LiteralPath (Join-Path $taskRoot 'tests/scenarios/M1-requirements.v1.json') -Destination (Join-Path $taskArchive 'scenario-case.json')
$taskEvidence=[Collections.Generic.List[object]]::new()
$taskIds=[Collections.Generic.List[string]]::new()
$taskHeadersByRole=@{}
$taskPassed=$false
function Save-TaskJson([string]$Name,[object]$Value) {
  [IO.File]::WriteAllText((Join-Path $taskArchive $Name),($Value|ConvertTo-Json -Depth 25),[Text.UTF8Encoding]::new($false))
}
function Invoke-TaskApi([string]$Role,[string]$Path,[string]$Method='Get',[object]$Body=$null) {
  $taskParameters=@{Uri="$BaseUrl/admin-api$Path";Headers=$taskHeadersByRole[$Role];Method=$Method;TimeoutSec=30}
  if($null -ne $Body) { $taskParameters.ContentType='application/json; charset=utf-8';$taskParameters.Body=($Body|ConvertTo-Json -Depth 20 -Compress) }
  try {
    $taskResponse=Invoke-RestMethod @taskParameters
    if($Path -eq '/doormes/requirements/create' -and $taskResponse.code -eq 0 -and $taskResponse.data.id -and !$taskIds.Contains($taskResponse.data.id)) { $taskIds.Add($taskResponse.data.id) }
    return $taskResponse
  } catch { throw "HTTP transport failed for $Path; sensitive details suppressed." }
}
function Assert-Task([string]$Id,[bool]$Result,[string]$Description) {
  $taskEvidence.Add(@{id=$Id;result=$(if($Result){'PASS'}else{'FAIL'});description=$Description})
  if(!$Result) { throw "Scenario assertion failed: $Id ($Description)." }
}
function Expected-Task([string]$Id,[int]$Code,[object]$Response) {
  Assert-Task $Id ($Response.code -eq $Code) "API code=$($Response.code), expected=$Code"
}
function Copy-Task([object]$Value) { return ($Value|ConvertTo-Json -Depth 20|ConvertFrom-Json) }
try {
  foreach($taskRole in @('sales','design','reviewer','purchase')) {
    $taskAccount=@($taskAccounts.accounts|Where-Object username -eq $taskRole)[0]
    $taskHeadersByRole[$taskRole]=@{'tenant-id'='1'}
    $taskLogin=Invoke-TaskApi $taskRole '/system/auth/login' 'Post' @{username=$taskAccount.username;password=$taskAccount.password}
    if($taskLogin.code -ne 0 -or !$taskLogin.data.accessToken) { throw "Login failed for $taskRole; details suppressed." }
    $taskHeadersByRole[$taskRole].Authorization='Bearer '+$taskLogin.data.accessToken
  }
  $taskDemand=@{schemaVersion='doormes-design-demand.v1';number='TEST-'+$taskRun;customer='M1接口验证（测试后归档）';project='仅接口场景';note='不是人工订单';lines=@(@{mark='C1';kind='custom';quantity=2;requirement=@{widthMm=1200;heightMm=1500;material='AL70';glass='GL24';hardware='HW-TT';finish='RAL7016';dueDate='2026-10-15';note='左内开'}})}
  Save-TaskJson 'request-create.json' @{expectedRevision=0;changeNote='接口验证新建';demand=$taskDemand}
  $taskFirst=Invoke-TaskApi 'sales' '/doormes/requirements/create' 'Post' @{expectedRevision=0;changeNote='接口验证新建';demand=$taskDemand}
  Expected-Task 'ASSERT-01' 0 $taskFirst
  $taskId=$taskFirst.data.id
  Assert-Task 'ASSERT-02' ($taskFirst.data.revision -eq 1 -and $taskFirst.data.status -eq 'DRAFT' -and $taskFirst.data.createdBy -eq 1002 -and $taskFirst.data.demand.lines[0].id) 'Draft revision 1 with server-owned actor and line ID'
  $taskReload=Invoke-TaskApi 'sales' "/doormes/requirements/get?id=$taskId"
  Expected-Task 'ASSERT-03' 0 $taskReload
  Assert-Task 'ASSERT-04' ($taskReload.data.demand.customer -eq $taskDemand.customer -and $taskReload.data.demand.lines[0].requirement.widthMm -eq 1200) 'Actual GET roundtrip including Chinese text'
  $taskChanged=Copy-Task $taskReload.data.demand
  $taskChanged.lines[0].requirement.widthMm=1250
  $taskSecond=Invoke-TaskApi 'sales' "/doormes/requirements/update?id=$taskId" 'Put' @{expectedRevision=1;changeNote='宽度修改';demand=$taskChanged}
  Expected-Task 'ASSERT-05' 0 $taskSecond
  Assert-Task 'ASSERT-06' ($taskSecond.data.revision -eq 2 -and $taskSecond.data.demand.lines[0].requirement.widthMm -eq 1250 -and $taskSecond.data.demand.lines[0].id -eq $taskFirst.data.demand.lines[0].id) 'Revision 2 retains internal line identity'
  $taskOld=Invoke-TaskApi 'sales' "/doormes/requirements/get?id=$taskId&revision=1"
  Assert-Task 'ASSERT-07' ($taskOld.code -eq 0 -and $taskOld.data.demand.lines[0].requirement.widthMm -eq 1200) 'Historical revision is immutable'
  Expected-Task 'ASSERT-08' 409 (Invoke-TaskApi 'sales' "/doormes/requirements/update?id=$taskId" 'Put' @{expectedRevision=1;changeNote='过期修订';demand=$taskChanged})
  Expected-Task 'ASSERT-09' 409 (Invoke-TaskApi 'sales' '/doormes/requirements/create' 'Post' @{expectedRevision=0;changeNote='重复单号';demand=$taskDemand})
  $taskInvalid=Copy-Task $taskDemand; $taskInvalid.number='INVALID-'+$taskRun; $taskInvalid.lines[0].requirement.widthMm=0
  Expected-Task 'ASSERT-10' 400 (Invoke-TaskApi 'sales' '/doormes/requirements/create' 'Post' @{expectedRevision=0;changeNote='尺寸边界';demand=$taskInvalid})
  $taskInvalid=Copy-Task $taskDemand; $taskInvalid.number='INVALID-'+$taskRun; $taskInvalid.lines+=@(Copy-Task $taskInvalid.lines[0]);$taskInvalid.lines[1].mark='c1'
  Expected-Task 'ASSERT-11' 400 (Invoke-TaskApi 'sales' '/doormes/requirements/create' 'Post' @{expectedRevision=0;changeNote='编号重复';demand=$taskInvalid})
  $taskInvalid=Copy-Task $taskDemand; $taskInvalid.number='INVALID-'+$taskRun;$taskInvalid.lines[0].kind='standard'
  Expected-Task 'ASSERT-12' 501 (Invoke-TaskApi 'sales' '/doormes/requirements/create' 'Post' @{expectedRevision=0;changeNote='禁止虚构标准引用';demand=$taskInvalid})
  Expected-Task 'ASSERT-13' 403 (Invoke-TaskApi 'reviewer' '/doormes/requirements/create' 'Post' @{expectedRevision=0;changeNote='越权';demand=$taskDemand})
  Expected-Task 'ASSERT-14' 403 (Invoke-TaskApi 'purchase' '/doormes/requirements/page')
  Expected-Task 'ASSERT-15' 403 (Invoke-TaskApi 'sales' "/doormes/requirements/claim?id=$taskId" 'Post' @{expectedRevision=2;note='越权领用'})
  $taskSubmit=Invoke-TaskApi 'sales' "/doormes/requirements/submit?id=$taskId" 'Post' @{expectedRevision=2;note='提交研发'}
  Assert-Task 'ASSERT-16' ($taskSubmit.code -eq 0 -and $taskSubmit.data.revision -eq 3 -and $taskSubmit.data.status -eq 'SUBMITTED') 'Submitted revision 3'
  $taskQueue=Invoke-TaskApi 'design' "/doormes/requirements/page?status=SUBMITTED&keyword=$($taskDemand.number)"
  Assert-Task 'ASSERT-17' ($taskQueue.code -eq 0 -and $taskQueue.data.total -eq 1 -and $taskQueue.data.list[0].id -eq $taskId) 'R&D sees actual submitted queue'
  $taskClaim=Invoke-TaskApi 'design' "/doormes/requirements/claim?id=$taskId" 'Post' @{expectedRevision=3;note='研发领用'}
  Assert-Task 'ASSERT-18' ($taskClaim.code -eq 0 -and $taskClaim.data.revision -eq 4 -and $taskClaim.data.status -eq 'IN_DESIGN' -and $taskClaim.data.assignedTo -eq 1003) 'R&D revision 4 assigned to authenticated account'
  Expected-Task 'ASSERT-19' 409 (Invoke-TaskApi 'design' "/doormes/requirements/claim?id=$taskId" 'Post' @{expectedRevision=3;note='重复领用'})
  Expected-Task 'ASSERT-20' 409 (Invoke-TaskApi 'sales' "/doormes/requirements/update?id=$taskId" 'Put' @{expectedRevision=4;changeNote='禁止覆盖研发基线';demand=$taskChanged})
  Expected-Task 'ASSERT-21' 0 (Invoke-TaskApi 'reviewer' "/doormes/requirements/get?id=$taskId")
  $taskVersions=Invoke-TaskApi 'design' "/doormes/requirements/versions?id=$taskId"
  Assert-Task 'ASSERT-22' ($taskVersions.code -eq 0 -and @($taskVersions.data).Count -eq 4) 'Exactly four persisted revisions'
  Save-TaskJson 'final-document.json' $taskClaim.data
  Save-TaskJson 'versions.json' $taskVersions.data
  $taskDb=Invoke-DoormesAdminQuery -Sql "SELECT revision,status,assigned_to FROM doormes_local.dm_design_requirement WHERE tenant_id=1 AND id='$taskId'; SELECT revision,sha256 FROM doormes_local.dm_design_requirement_version WHERE tenant_id=1 AND requirement_id='$taskId' ORDER BY revision;"
  [IO.File]::WriteAllText((Join-Path $taskArchive 'database-evidence.txt'),$taskDb,[Text.UTF8Encoding]::new($false))
  Assert-Task 'ASSERT-23' ($taskDb.StartsWith("4`tIN_DESIGN`t1003")) 'MySQL current metadata matches API'
  $taskHashes=@($taskDb -split '\r?\n'|Select-Object -Skip 1)
  for($taskRev=1;$taskRev -le 4;$taskRev++) {
    $taskFile=Join-Path $taskRoot "runtime-local/design-data/requirements/tenant-1/$taskId/r$taskRev.json"
    $taskExpectedHash=($taskHashes[$taskRev-1] -split "`t")[1]
    Assert-Task "ASSERT-FILE-0$taskRev" ((Get-FileHash -LiteralPath $taskFile).Hash.ToLowerInvariant() -eq $taskExpectedHash) "Snapshot R$taskRev exists and SHA-256 matches database"
  }
  $taskPassed=$true
} finally {
  Save-TaskJson 'manifest.json' @{scenarioId=$taskRun;tenantId=1;database='doormes_local';ids=@($taskIds);baseUrl=$BaseUrl;passed=$taskPassed;browserUsed=$false;testedAt=(Get-Date).ToString('o');assertions=@($taskEvidence)}
  foreach($taskRole in $taskHeadersByRole.Keys) {
    if($taskHeadersByRole[$taskRole].Authorization) { try { $null=Invoke-TaskApi $taskRole '/system/auth/logout' 'Post' @{} } catch { Write-Warning "Test logout failed for $taskRole." } }
  }
}
"Scenario passed; evidence: $taskArchive. Controlled test record must be archived by the cleanup script."
