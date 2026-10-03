[CmdletBinding()]param([string]$BaseUrl='http://localhost:5180')
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot;$taskUri=[Uri]$BaseUrl
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0' -or $taskUri.Host -notin @('localhost','127.0.0.1','192.168.50.193') -or $taskUri.Port -ne 5180 -or $taskUri.Scheme -ne 'http'){throw 'Unexpected isolated environment.'}
$taskMigration=Get-Content (Join-Path $taskRoot 'runtime-local/design-data/schema-v003.json') -Raw|ConvertFrom-Json
if($taskMigration.database -ne 'doormes_local'){throw 'Unexpected database.'}
$taskAccounts=Get-Content (Join-Path $taskRoot 'runtime-local/factory-bootstrap/accounts.local.json') -Raw|ConvertFrom-Json
$taskRun='M1-CAT-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6)
$taskArchive=Join-Path $taskRoot "runtime-local/scenarios/$taskRun";[IO.Directory]::CreateDirectory($taskArchive)|Out-Null
Copy-Item -LiteralPath (Join-Path $taskRoot 'tests/scenarios/M1-catalog.v1.json') -Destination (Join-Path $taskArchive 'scenario-case.json')
$taskEvidence=[Collections.Generic.List[object]]::new();$taskIds=[Collections.Generic.List[string]]::new();$taskCatalogIds=[Collections.Generic.List[string]]::new();$taskHeaders=@{};$taskPassed=$false
function Save-TaskJson([string]$Name,[object]$Value){[IO.File]::WriteAllText((Join-Path $taskArchive $Name),($Value|ConvertTo-Json -Depth 75),[Text.UTF8Encoding]::new($false))}
function Invoke-TaskApi([string]$Role,[string]$Path,[string]$Method='Get',[object]$Body=$null){
  $taskParameters=@{Uri="$BaseUrl/admin-api$Path";Headers=$taskHeaders[$Role];Method=$Method;TimeoutSec=45}
  if($null -ne $Body){$taskParameters.ContentType='application/json;charset=utf-8';$taskParameters.Body=($Body|ConvertTo-Json -Depth 75 -Compress)}
  try{$taskResponse=Invoke-RestMethod @taskParameters}catch{throw "HTTP failed for $Path; sensitive details suppressed."}
  if($taskResponse.code -eq 0 -and $taskResponse.data.id){if($Path -eq '/doormes/catalog/create'){$taskCatalogIds.Add($taskResponse.data.id)}elseif($Path -eq '/doormes/requirements/create'){$taskIds.Add($taskResponse.data.id)}}
  return $taskResponse
}
function Assert-Task([string]$Id,[bool]$Result,[string]$Description){$taskEvidence.Add(@{id=$Id;result=$(if($Result){'PASS'}else{'FAIL'});description=$Description});if(!$Result){throw "Assertion failed: $Id ($Description)."}}
function Expected-Task([string]$Id,[int]$Code,[object]$Response){Assert-Task $Id ($Response.code -eq $Code) "API code=$($Response.code), expected=$Code"}
function Copy-Task([object]$Value){return ($Value|ConvertTo-Json -Depth 75|ConvertFrom-Json)}
function Apply-TaskCatalog([object]$Document,[string]$Target,[object]$Choice){
  $taskInput=Join-Path $taskArchive ([Guid]::NewGuid().ToString()+'-input.json');$taskOutput=$taskInput.Replace('-input.json','-output.json')
  [IO.File]::WriteAllText($taskInput,(@{mode='apply-catalog';document=$Document;windowId=$Document.windows[0].objectId;target=$Target;choice=$Choice}|ConvertTo-Json -Depth 75 -Compress),[Text.UTF8Encoding]::new($false))
  & 'C:\Program Files\nodejs\node.exe' (Join-Path $taskRoot 'frontend/vendor/doormes-engine/dist/validator.mjs') $taskInput $taskOutput
  if($LASTEXITCODE -ne 0){throw 'Shared catalog command validation failed.'}
  $taskResult=Get-Content -LiteralPath $taskOutput -Raw|ConvertFrom-Json;if(!$taskResult.ok){throw 'Shared catalog command validation failed.'};return $taskResult.document
}
try{
  foreach($taskRole in @('admin','sales','design')){
    $taskAccount=@($taskAccounts.accounts|Where-Object username -eq $taskRole)[0];$taskHeaders[$taskRole]=@{'tenant-id'='1'}
    $taskLogin=Invoke-TaskApi $taskRole '/system/auth/login' 'Post' @{username=$taskAccount.username;password=$taskAccount.password}
    if($taskLogin.code -ne 0 -or !$taskLogin.data.accessToken){throw "Login failed for $taskRole."};$taskHeaders[$taskRole].Authorization='Bearer '+$taskLogin.data.accessToken
  }
  $taskItem=@{category='finish';code="TEST-$taskRun-FIN";name='测试深灰';specification='RAL7016测试喷涂';note='仅接口场景';materialFamily='metal';baseColor='#374151';metalness=0.7;roughness=0.35;opacity=1;thicknessMm=$null;compatibleProfileSystemIds=@()}
  $taskCreateBody=@{expectedRevision=0;changeNote='场景新建';item=$taskItem}
  Expected-Task 'ASSERT-01' 403 (Invoke-TaskApi 'sales' '/doormes/catalog/create' 'Post' $taskCreateBody)
  $taskFinish=Invoke-TaskApi 'design' '/doormes/catalog/create' 'Post' $taskCreateBody;Expected-Task 'ASSERT-02' 0 $taskFinish;$taskFinishId=$taskFinish.data.id
  Expected-Task 'ASSERT-03' 409 (Invoke-TaskApi 'design' '/doormes/catalog/create' 'Post' $taskCreateBody)
  Expected-Task 'ASSERT-04' 403 (Invoke-TaskApi 'design' "/doormes/catalog/publish?id=$taskFinishId" 'Post' @{expectedRevision=1;note='越权发布'})
  $taskPublished=Invoke-TaskApi 'admin' "/doormes/catalog/publish?id=$taskFinishId" 'Post' @{expectedRevision=1;note='管理员发布设计选型'};Expected-Task 'ASSERT-05' 0 $taskPublished
  Assert-Task 'ASSERT-06' ($taskPublished.data.revision -eq 2 -and $taskPublished.data.data.scope -eq 'design-only' -and !$taskPublished.data.data.productionReady) 'Publication is an immutable design version, not manufacturing readiness'
  $taskPublishedPage=Invoke-TaskApi 'sales' "/doormes/catalog/page?published=true&keyword=TEST-$taskRun&pageNo=1&pageSize=20"
  Assert-Task 'ASSERT-07' ($taskPublishedPage.code -eq 0 -and $taskPublishedPage.data.total -eq 1 -and $taskPublishedPage.data.list[0].revision -eq 2) 'Published selector exposes precise version'
  $taskDemand=@{schemaVersion='doormes-design-demand.v1';number='TEST-'+$taskRun;customer='材质目录测试（后归档）';project='仅测试';note='不是人工订单';lines=@(@{mark='C1';kind='custom';quantity=1;requirement=@{widthMm=1200;heightMm=1500;material='测试需求';glass='测试玻璃';hardware='测试五金';finish='测试';dueDate='2026-10-15';note='仅接口测试'}})}
  $taskRequirement=Invoke-TaskApi 'sales' '/doormes/requirements/create' 'Post' @{expectedRevision=0;changeNote='测试';demand=$taskDemand};Expected-Task 'ASSERT-08' 0 $taskRequirement
  $taskRequirementId=$taskRequirement.data.id;$taskLineId=$taskRequirement.data.demand.lines[0].id
  Expected-Task 'ASSERT-09' 0 (Invoke-TaskApi 'sales' "/doormes/requirements/submit?id=$taskRequirementId" 'Post' @{expectedRevision=1;note='提交'})
  Expected-Task 'ASSERT-10' 0 (Invoke-TaskApi 'design' "/doormes/requirements/claim?id=$taskRequirementId" 'Post' @{expectedRevision=2;note='领用'})
  $taskFirst=Invoke-TaskApi 'design' '/doormes/drawings/open' 'Post' @{requirementId=$taskRequirementId;lineId=$taskLineId;expectedRequirementRevision=3};Expected-Task 'ASSERT-11' 0 $taskFirst;$taskDrawingId=$taskFirst.data.id
  $taskOutside=Apply-TaskCatalog $taskFirst.data.document 'profile-outside' $taskPublished.data
  $taskSecond=Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=1;changeNote='外表面R2选型';document=$taskOutside};Expected-Task 'ASSERT-12' 0 $taskSecond
  Assert-Task 'ASSERT-13' ($taskSecond.data.document.windows[0].visualConfiguration.appearance.frame.outside.appearanceId -eq "MES-CATALOG:$taskFinishId" -and $taskSecond.data.document.windows[0].visualConfiguration.appearance.frame.outside.appearanceVersion -eq '2') 'Drawing keeps catalog UUID + exact business version and renderer parameters'
  $taskModified=Copy-Task $taskItem;$taskModified.baseColor='#ffffff';$taskModified.name='测试白色'
  $taskDraft=Invoke-TaskApi 'design' "/doormes/catalog/revise?id=$taskFinishId" 'Put' @{expectedRevision=2;changeNote='新白色草稿';item=$taskModified};Expected-Task 'ASSERT-14' 0 $taskDraft
  $taskPublishedPage=Invoke-TaskApi 'design' "/doormes/catalog/page?published=true&keyword=TEST-$taskRun&pageNo=1&pageSize=20"
  Assert-Task 'ASSERT-15' ($taskPublishedPage.code -eq 0 -and $taskPublishedPage.data.list[0].revision -eq 2) 'New draft does not remove prior published version'
  $taskInvalid=Copy-Task $taskSecond.data.document;$taskInvalid.windows[0].visualConfiguration.appearance.frame.outside=Copy-Task $taskDraft.data.data.appearance
  Expected-Task 'ASSERT-16' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=2;changeNote='非法草稿引用';document=$taskInvalid})
  Expected-Task 'ASSERT-17' 409 (Invoke-TaskApi 'design' "/doormes/catalog/revise?id=$taskFinishId" 'Put' @{expectedRevision=2;changeNote='过期';item=$taskModified})
  $taskNewPublished=Invoke-TaskApi 'admin' "/doormes/catalog/publish?id=$taskFinishId" 'Post' @{expectedRevision=3;note='发布白色'};Expected-Task 'ASSERT-18' 0 $taskNewPublished
  $taskOld=Invoke-TaskApi 'design' "/doormes/drawings/get?id=$taskDrawingId&revision=2"
  Assert-Task 'ASSERT-19' ($taskOld.code -eq 0 -and $taskOld.data.document.windows[0].visualConfiguration.appearance.frame.outside.baseColor -eq '#374151') 'Publishing newer material never changes historical drawing'
  $taskInside=Apply-TaskCatalog $taskSecond.data.document 'profile-inside' $taskNewPublished.data
  $taskThird=Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=2;changeNote='室内白色R4，室外深灰R2';document=$taskInside};Expected-Task 'ASSERT-20' 0 $taskThird
  Assert-Task 'ASSERT-21' ($taskThird.data.document.windows[0].visualConfiguration.appearance.frame.inside.baseColor -eq '#ffffff' -and $taskThird.data.document.windows[0].visualConfiguration.appearance.frame.outside.baseColor -eq '#374151') 'Independent inside/outside finish mappings'
  $taskInvalid=Copy-Task $taskThird.data.document;$taskInvalid.windows[0].visualConfiguration.appearance.frame.outside.baseColor='#000000'
  Expected-Task 'ASSERT-22' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=3;changeNote='篡改冻结材质';document=$taskInvalid})
  $taskGlass=@{category='glass';code="TEST-$taskRun-GL";name='测试27mm玻璃';specification='6+15+6';note='仅测试';materialFamily='glass';baseColor='#c8e7f0';metalness=0;roughness=0.1;opacity=0.4;thicknessMm=27;compatibleProfileSystemIds=@('AL70')}
  $taskGlassDraft=Invoke-TaskApi 'design' '/doormes/catalog/create' 'Post' @{expectedRevision=0;changeNote='新玻璃';item=$taskGlass};Expected-Task 'ASSERT-23' 0 $taskGlassDraft;$taskGlassId=$taskGlassDraft.data.id
  $taskGlassPublished=Invoke-TaskApi 'admin' "/doormes/catalog/publish?id=$taskGlassId" 'Post' @{expectedRevision=1;note='发布测试玻璃'};Expected-Task 'ASSERT-24' 0 $taskGlassPublished
  $taskGlassDocument=Apply-TaskCatalog $taskThird.data.document 'glass' $taskGlassPublished.data
  $taskFourth=Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=3;changeNote='选用27mm玻璃';document=$taskGlassDocument};Expected-Task 'ASSERT-25' 0 $taskFourth
  Assert-Task 'ASSERT-26' ($taskFourth.data.document.windows[0].sectionDimensions.glassDepthMm -eq 27 -and $taskFourth.data.document.windows[0].defaultGlassSelection.materialCode -eq $taskGlass.code -and $taskFourth.data.document.windows[0].visualConfiguration.appearance.glass.opacity -eq 0.4) 'Glass business model, thickness and visual appearance travel together'
  $taskInvalid=Copy-Task $taskFourth.data.document;$taskInvalid.windows[0].sectionDimensions.glassDepthMm=24
  Expected-Task 'ASSERT-27' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=4;changeNote='厚度不一致';document=$taskInvalid})
  $taskInvalid=Copy-Task $taskFourth.data.document;$taskInvalid.windows[0].defaultGlassSelection.businessName='篡改名称'
  Expected-Task 'ASSERT-28' 400 (Invoke-TaskApi 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=4;changeNote='篡改业务选型';document=$taskInvalid})
  $taskVersions=Invoke-TaskApi 'design' "/doormes/drawings/versions?id=$taskDrawingId"
  Assert-Task 'ASSERT-29' ($taskVersions.code -eq 0 -and @($taskVersions.data).Count -eq 4) 'Rejected mappings do not create diagram revisions'
  Save-TaskJson 'drawing-final.json' $taskFourth.data;Save-TaskJson 'finish-r2.json' $taskPublished.data;Save-TaskJson 'finish-r4.json' $taskNewPublished.data;Save-TaskJson 'glass-r2.json' $taskGlassPublished.data
  $taskDb=Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM doormes_local.dm_material_catalog WHERE tenant_id=1 AND id IN('$taskFinishId','$taskGlassId'); SELECT catalog_id,revision,sha256 FROM doormes_local.dm_material_catalog_version WHERE tenant_id=1 AND catalog_id IN('$taskFinishId','$taskGlassId') ORDER BY catalog_id,revision; SELECT revision FROM doormes_local.dm_drawing WHERE tenant_id=1 AND id='$taskDrawingId';"
  [IO.File]::WriteAllText((Join-Path $taskArchive 'database-evidence.txt'),$taskDb,[Text.UTF8Encoding]::new($false))
  $taskLines=@($taskDb -split '\r?\n');Assert-Task 'ASSERT-30' ($taskLines[0] -eq '2' -and $taskLines[-1] -eq '4' -and $taskLines.Count -eq 8) 'SQL contains two controlled catalog items, six catalog versions and four drawing versions'
  $taskAllHashes=$true;foreach($taskRow in $taskLines[1..6]){$taskValues=$taskRow -split "`t";$taskFile=Join-Path $taskRoot "runtime-local/design-data/catalog/tenant-1/$($taskValues[0])/r$($taskValues[1]).json";if((Get-FileHash -LiteralPath $taskFile).Hash.ToLowerInvariant() -ne $taskValues[2]){$taskAllHashes=$false}}
  Assert-Task 'ASSERT-31' $taskAllHashes 'All six catalog JSON version hashes match SQL metadata'
  $taskPassed=$true
}finally{
  Save-TaskJson 'manifest.json' @{scenarioId=$taskRun;tenantId=1;database='doormes_local';ids=@($taskIds);catalogIds=@($taskCatalogIds);baseUrl=$BaseUrl;passed=$taskPassed;browserUsed=$false;testedAt=(Get-Date).ToString('o');assertions=@($taskEvidence)}
  foreach($taskRole in $taskHeaders.Keys){if($taskHeaders[$taskRole].Authorization){try{$null=Invoke-TaskApi $taskRole '/system/auth/logout' 'Post' @{}}catch{Write-Warning "Logout failed for $taskRole."}}}
}
"Catalog scenario passed; evidence: $taskArchive. Archive only this controlled test using the cleanup scripts."
