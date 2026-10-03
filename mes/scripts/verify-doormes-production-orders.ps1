[CmdletBinding()]param([string]$BaseUrl='http://localhost:5180')
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0'){throw 'Unexpected workspace.'}
$taskUri=[Uri]$BaseUrl
if($taskUri.Host -notin @('localhost','127.0.0.1','192.168.50.193') -or $taskUri.Port -ne 5180 -or $taskUri.Scheme -ne 'http' -or $taskUri.AbsolutePath -ne '/' -or $taskUri.Query){throw 'Only the isolated frontend origin is allowed.'}
$taskSchema=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN('dm_production_order','dm_production_order_version');"
if($taskSchema -ne '2'){throw 'V006 is not deployed. No records created.'}
$taskRun='ORDER-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,4)
$taskArchive=Join-Path $taskRoot "runtime-local/scenarios/$taskRun"
New-Item -ItemType Directory -Path $taskArchive|Out-Null
$taskEvidence=[Collections.Generic.List[object]]::new();$taskIds=[Collections.Generic.List[string]]::new();$taskDrawingIds=[Collections.Generic.List[string]]::new();$taskHeaders=@{};$taskPassed=$false
$taskAccounts=Get-Content -LiteralPath (Join-Path $taskRoot 'runtime-local/factory-bootstrap/accounts.local.json') -Raw|ConvertFrom-Json
function Save-Proof([string]$Name,[object]$Value){[IO.File]::WriteAllText((Join-Path $taskArchive $Name),($Value|ConvertTo-Json -Depth 100),[Text.UTF8Encoding]::new($false))}
function Api([string]$Role,[string]$Path,[string]$Method='Get',[object]$Body=$null){
  $taskOptions=@{Uri=($BaseUrl.TrimEnd('/')+'/admin-api'+$Path);Headers=$taskHeaders[$Role];Method=$Method;TimeoutSec=45}
  if($null-ne $Body){$taskOptions.ContentType='application/json;charset=utf-8';$taskOptions.Body=($Body|ConvertTo-Json -Depth 100 -Compress)}
  try{return Invoke-RestMethod @taskOptions}catch{throw "HTTP request failed at $Path. Credentials suppressed."}
}
function Check([string]$Id,[bool]$Pass,[string]$Description){$taskEvidence.Add(@{id=$Id;result=$(if($Pass){'PASS'}else{'FAIL'});description=$Description});if(!$Pass){throw "Assertion $Id failed: $Description"}}
function Code([string]$Id,[int]$Expected,[object]$Response){Check $Id ($Response.code -eq $Expected) "API expected $Expected, got $($Response.code): $($Response.msg)"}
function Clone([object]$Value){return ($Value|ConvertTo-Json -Depth 100|ConvertFrom-Json)}
try {
  foreach($taskRole in @('admin','sales','design','reviewer','production')){
    $taskAccount=@($taskAccounts.accounts|Where-Object username -eq $taskRole)[0];$taskHeaders[$taskRole]=@{'tenant-id'='1'}
    $taskLogin=Api $taskRole '/system/auth/login' 'Post' @{username=$taskAccount.username;password=$taskAccount.password}
    if($taskLogin.code-ne 0-or !$taskLogin.data.accessToken){throw "Login failed for $taskRole"}
    $taskHeaders[$taskRole].Authorization='Bearer '+$taskLogin.data.accessToken
  }
  $taskSharedBefore=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM doormes_local.system_users; SELECT COUNT(*) FROM doormes_local.system_dept; SELECT COUNT(*) FROM doormes_local.system_role;"
  $taskD=Api design '/doormes/drawings/create' 'Post' @{clientRequestId=[Guid]::NewGuid().ToString();number="TEST-$taskRun";name='订单基线验收样窗';note='自动场景受控图纸';mark='C1';quantity=2;widthMm=1200;heightMm=1500}
  Code 'ASSERT-01' 0 $taskD;$taskDrawing=$taskD.data;$taskDrawingIds.Add($taskDrawing.id)
  $taskCreate=@{number="TEST-$taskRun-S";customer='受控订单测试客户';project='固定图纸版本与设计BOM验收';type='STANDARD';quantity=3;note='TEST 自动场景，可保留供人工复验';drawingId=$taskDrawing.id;drawingRevision=1}
  Code 'ASSERT-02' 403 (Api design '/doormes/production-orders/create' Post $taskCreate)
  $taskResponse=Api sales '/doormes/production-orders/create' Post $taskCreate;Code 'ASSERT-03' 0 $taskResponse;$taskOrder=$taskResponse.data;$taskIds.Add($taskOrder.id)
  Save-Proof 'standard-r1.json' $taskOrder
  Check 'ASSERT-04' ($taskOrder.status-eq 'BOM_READY'-and $taskOrder.drawing.revision-eq 1-and !$taskOrder.productionReady) 'Saved design fixed at R1, production remains blocked'
  $taskFrame=@($taskOrder.bom.lines|Where-Object category -eq profile)
  Check 'ASSERT-05' ($taskFrame.Count-ge 4-and @($taskFrame|Where-Object {$_.quantity-ne 2-or $_.totalQuantity-ne 6}).Count-eq 0) 'Four frame members, document quantity 2 times order sets 3 exactly once'
  Check 'ASSERT-06' (@($taskOrder.bom.lines.objectId|Select-Object -Unique).Count-eq @($taskOrder.bom.lines).Count) 'BOM internal part identities are unique'
  Code 'ASSERT-07' 409 (Api sales '/doormes/production-orders/create' Post $taskCreate)
  $taskChanged=Clone $taskDrawing.document;$taskChanged.windows[0].widthMm=1250
  $taskD2=Api design "/doormes/drawings/save?id=$($taskDrawing.id)" Put @{expectedRevision=1;changeNote='宽改1250用于变更';document=$taskChanged};Code 'ASSERT-08' 0 $taskD2
  $taskStill=Api production "/doormes/production-orders/get?id=$($taskOrder.id)"
  Check 'ASSERT-09' ($taskStill.data.drawing.revision-eq 1) 'Saving a newer drawing does not silently update the order'
  $taskChange=Api design "/doormes/production-orders/changes/request?id=$($taskOrder.id)" Post @{expectedRevision=1;drawingId=$taskDrawing.id;drawingRevision=2;reason='客户尺寸变更至1250'};Code 'ASSERT-10' 0 $taskChange;$taskOrder=$taskChange.data;$taskC=$taskOrder.changes[-1]
  Check 'ASSERT-11' ($taskC.status-eq 'PENDING'-and $taskOrder.drawing.revision-eq 1-and @($taskC.diff.modified).Count-gt 0) 'Request retains R1 and computes changed component parameters'
  Code 'ASSERT-12' 403 (Api design "/doormes/production-orders/changes/review?id=$($taskOrder.id)" Post @{expectedRevision=$taskOrder.revision;changeId=$taskC.id;approved=$true;note='越权审核应拒绝'})
  Code 'ASSERT-13' 409 (Api production "/doormes/production-orders/changes/apply?id=$($taskOrder.id)" Post @{expectedRevision=$taskOrder.revision;changeId=$taskC.id;note='未经审核不执行'})
  $taskReview=Api reviewer "/doormes/production-orders/changes/review?id=$($taskOrder.id)" Post @{expectedRevision=$taskOrder.revision;changeId=$taskC.id;approved=$true;note='审核固定R2，仅设计基线'};Code 'ASSERT-14' 0 $taskReview;$taskOrder=$taskReview.data
  Check 'ASSERT-15' ($taskOrder.drawing.revision-eq 1-and $taskOrder.changes[-1].status-eq 'APPROVED') 'Review is separate from production application'
  $taskChanged=Clone $taskD2.data.document;$taskChanged.windows[0].widthMm=1300
  Code 'ASSERT-16' 0 (Api design "/doormes/drawings/save?id=$($taskDrawing.id)" Put @{expectedRevision=2;changeNote='另存R3不得混入已审核R2';document=$taskChanged})
  $taskApply=Api production "/doormes/production-orders/changes/apply?id=$($taskOrder.id)" Post @{expectedRevision=$taskOrder.revision;changeId=$taskC.id;note='明确采用已审核R2'};Code 'ASSERT-17' 0 $taskApply;$taskOrder=$taskApply.data
  Check 'ASSERT-18' ($taskOrder.drawing.revision-eq 2-and $taskOrder.changes[-1].status-eq 'APPLIED') 'Production adopts exact approved R2, never latest R3'
  Save-Proof 'standard-applied.json' $taskOrder
  $taskOld=Api production "/doormes/production-orders/get?id=$($taskOrder.id)&revision=1"
  Check 'ASSERT-19' ($taskOld.data.drawing.revision-eq 1-and $taskOld.data.bom.lines[0].objectId-eq $taskOrder.bom.lines[0].objectId) 'Order history preserves original BOM with stable part identity'
  Code 'ASSERT-20' 409 (Api production "/doormes/production-orders/changes/apply?id=$($taskOrder.id)" Post @{expectedRevision=$taskOrder.revision;changeId=$taskC.id;note='重复执行应拒绝'})
  $taskCustom=@{number="TEST-$taskRun-C";customer='受控定制测试客户';project='定制订单绘图验收';type='CUSTOM';quantity=2;note='TEST 自动场景，供设计账号复验';custom=@{mark='C1';widthMm=1100;heightMm=1400;material='AL70';glass='24mm中空玻璃';hardware='平开五金';finish='灰色';dueDate='2026-11-01';note='定制生产需求'}}
  $taskNew=Api sales '/doormes/production-orders/create' Post $taskCustom;Code 'ASSERT-21' 0 $taskNew;$taskCustomOrder=$taskNew.data;$taskIds.Add($taskCustomOrder.id)
  Check 'ASSERT-22' ($taskCustomOrder.status-eq 'DRAFT'-and $taskCustomOrder.requirement.id-and !$taskCustomOrder.drawing) 'Custom order creates a real demand, not a placeholder drawing'
  $taskCustomUpdate=Clone $taskCustom
  $taskCustomUpdate|Add-Member -NotePropertyName expectedRevision -NotePropertyValue $taskCustomOrder.revision
  $taskCustomUpdate.custom.widthMm=1120;$taskCustomUpdate.quantity=4
  Code 'ASSERT-22A' 403 (Api design "/doormes/production-orders/update?id=$($taskCustomOrder.id)" Put $taskCustomUpdate)
  $taskUpdated=Api sales "/doormes/production-orders/update?id=$($taskCustomOrder.id)" Put $taskCustomUpdate;Code 'ASSERT-22B' 0 $taskUpdated;$taskCustomOrder=$taskUpdated.data
  $taskDemand=Api design "/doormes/requirements/get?id=$($taskCustomOrder.requirement.id)&revision=$($taskCustomOrder.requirement.revision)"
  Check 'ASSERT-22C' ($taskCustomOrder.revision-eq 2-and $taskCustomOrder.quantity-eq 4-and $taskDemand.data.demand.lines[0].requirement.widthMm-eq 1120) 'Draft edit versions both order and requirement together'
  Code 'ASSERT-22D' 409 (Api sales "/doormes/production-orders/update?id=$($taskCustomOrder.id)" Put $taskCustomUpdate)
  Code 'ASSERT-22E' 409 (Api sales "/doormes/requirements/update?id=$($taskCustomOrder.requirement.id)" Put @{expectedRevision=$taskCustomOrder.requirement.revision;changeNote='独立入口不得绕过关联订单';demand=$taskDemand.data.demand})
  Code 'ASSERT-22F' 409 (Api sales "/doormes/requirements/submit?id=$($taskCustomOrder.requirement.id)" Post @{expectedRevision=$taskCustomOrder.requirement.revision;note='独立入口不得绕过订单提交'})
  $taskOriginalDemand=Api design "/doormes/requirements/get?id=$($taskCustomOrder.requirement.id)&revision=1"
  Check 'ASSERT-22G' ($taskOriginalDemand.data.demand.lines[0].requirement.widthMm-eq 1100) 'Draft update keeps original requirement history'
  $taskSubmit=Api sales "/doormes/production-orders/submit?id=$($taskCustomOrder.id)" Post @{expectedRevision=$taskCustomOrder.revision;note='提交定制研发'};Code 'ASSERT-23' 0 $taskSubmit;$taskCustomOrder=$taskSubmit.data
  $taskCustomUpdate.expectedRevision=$taskCustomOrder.revision
  Code 'ASSERT-23A' 409 (Api sales "/doormes/production-orders/update?id=$($taskCustomOrder.id)" Put $taskCustomUpdate)
  $taskClaim=Api design "/doormes/production-orders/claim?id=$($taskCustomOrder.id)" Post @{expectedRevision=$taskCustomOrder.revision;note='研发领用'};Code 'ASSERT-24' 0 $taskClaim;$taskCustomOrder=$taskClaim.data
  $taskOpen=Api design "/doormes/production-orders/open-drawing?id=$($taskCustomOrder.id)" Post @{expectedRevision=$taskCustomOrder.revision;note='开始绘图'};Code 'ASSERT-25' 0 $taskOpen;$taskCustomDrawing=$taskOpen.data.drawing;$taskDrawingIds.Add($taskCustomDrawing.id)
  Check 'ASSERT-26' ($taskCustomDrawing.document.windows[0].widthMm-eq 1120-and $taskCustomDrawing.document.windows[0].quantity-eq 1) 'Drawing seeded from updated demand as one set, order sets remain independent'
  $taskCustomGeometry=Clone $taskCustomDrawing.document;$taskCustomGeometry.windows[0].widthMm=1150
  $taskCustomSave=Api design "/doormes/drawings/save?id=$($taskCustomDrawing.id)" Put @{expectedRevision=1;changeNote='定制尺寸调整1150';document=$taskCustomGeometry};Code 'ASSERT-27' 0 $taskCustomSave
  $taskBind=Api design "/doormes/production-orders/bind-drawing?id=$($taskCustomOrder.id)" Post @{expectedRevision=$taskCustomOrder.revision;drawingId=$taskCustomDrawing.id;drawingRevision=2;note='采用定制设计R2带入组成件'};Code 'ASSERT-28' 0 $taskBind;$taskCustomOrder=$taskBind.data
  Check 'ASSERT-29' ($taskCustomOrder.drawing.revision-eq 2-and $taskCustomOrder.bom.lines.Count-gt 4-and !$taskCustomOrder.productionReady) 'Custom drawing and component BOM actually frozen onto the order'
  Save-Proof 'custom-bound.json' $taskCustomOrder
  Code 'ASSERT-30' 409 (Api design "/doormes/production-orders/bind-drawing?id=$($taskCustomOrder.id)" Post @{expectedRevision=$taskCustomOrder.revision;drawingId=$taskCustomDrawing.id;drawingRevision=2;note='重复绑定禁止绕过变更'})
  $taskSql=Invoke-DoormesAdminQuery -Sql "SELECT number,revision,status FROM doormes_local.dm_production_order WHERE tenant_id=1 AND id IN('$($taskIds[0])','$($taskIds[1])') ORDER BY number; SELECT COUNT(*) FROM doormes_local.dm_production_order_version WHERE tenant_id=1 AND order_id IN('$($taskIds[0])','$($taskIds[1])');"
  Save-Proof 'database-proof.json' @{rows=$taskSql}
  $taskSharedAfter=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM doormes_local.system_users; SELECT COUNT(*) FROM doormes_local.system_dept; SELECT COUNT(*) FROM doormes_local.system_role;"
  Check 'ASSERT-31' ($taskSharedBefore-eq $taskSharedAfter) 'Shared users, departments and roles unchanged'
  $taskPassed=$true
} finally {
  Save-Proof 'manifest.json' @{scenarioId=$taskRun;database='doormes_local';tenantId=1;passed=$taskPassed;assertions=$taskEvidence;orderIds=$taskIds;drawingIds=$taskDrawingIds;executedAt=(Get-Date).ToString('o');retention='Controlled TEST records retained for UI verification; no data or identities deleted.'}
}
"Production-order scenario passed: $($taskEvidence.Count) assertions; evidence $taskArchive"
