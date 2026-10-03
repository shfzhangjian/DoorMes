[CmdletBinding()]param([switch]$Apply,[string]$BaseUrl='http://localhost:5180')
$ErrorActionPreference='Stop'
$taskRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'));$taskUri=[Uri]$BaseUrl
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0' -or $taskUri.Host -notin @('localhost','127.0.0.1','192.168.50.193') -or $taskUri.Port -ne 5180 -or $taskUri.Scheme -ne 'http'){throw 'Unexpected isolated environment.'}
$taskMigration=Get-Content (Join-Path $taskRoot 'runtime-local/design-data/schema-v003.json') -Raw|ConvertFrom-Json
if($taskMigration.database -ne 'doormes_local'){throw 'Unexpected database.'}
$taskNote='仅人工测试用示例，不代表供应商已确认型号；发布仅供设计选型，不能作为正式制造目录。'
$taskItems=@(
  @{category='finish';code='DEMO-FIN-RAL7016';name='[示例] 深灰表面';specification='RAL7016 色彩参考';note=$taskNote;materialFamily='metal';baseColor='#383e42';metalness=0.5;roughness=0.4;opacity=1;thicknessMm=$null;compatibleProfileSystemIds=@()},
  @{category='finish';code='DEMO-FIN-RAL9016';name='[示例] 白色表面';specification='RAL9016 色彩参考';note=$taskNote;materialFamily='metal';baseColor='#f1f0ea';metalness=0.2;roughness=0.45;opacity=1;thicknessMm=$null;compatibleProfileSystemIds=@()},
  @{category='finish';code='DEMO-FIN-RAL9005';name='[示例] 黑色表面';specification='RAL9005 色彩参考';note=$taskNote;materialFamily='metal';baseColor='#0a0a0a';metalness=0.5;roughness=0.4;opacity=1;thicknessMm=$null;compatibleProfileSystemIds=@()},
  @{category='glass';code='DEMO-GL-CLEAR-24';name='[示例] 24mm 透明中空玻璃';specification='6+12+6（仅示例）';note=$taskNote;materialFamily='glass';baseColor='#c8e7f0';metalness=0;roughness=0.08;opacity=0.35;thicknessMm=24;compatibleProfileSystemIds=@('AL70')},
  @{category='glass';code='DEMO-GL-GREEN-27';name='[示例] 27mm 浅绿中空玻璃';specification='6+15+6（仅示例）';note=$taskNote;materialFamily='glass';baseColor='#b7d5c9';metalness=0;roughness=0.1;opacity=0.45;thicknessMm=27;compatibleProfileSystemIds=@('AL70')}
)
if(!$Apply){$taskItems|Select-Object code,name; 'Preview only. Create missing examples through real admin API; existing records will never be overwritten.';return}
$taskAccount=@((Get-Content (Join-Path $taskRoot 'runtime-local/factory-bootstrap/accounts.local.json') -Raw|ConvertFrom-Json).accounts|Where-Object username -eq 'admin')[0]
$taskHeaders=@{'tenant-id'='1'};$taskEvidence=[Collections.Generic.List[object]]::new()
function Invoke-TaskApi([string]$Path,[string]$Method='Get',[object]$Body=$null){
  $taskParameters=@{Uri="$BaseUrl/admin-api$Path";Headers=$taskHeaders;Method=$Method;TimeoutSec=45}
  if($null -ne $Body){$taskParameters.ContentType='application/json;charset=utf-8';$taskParameters.Body=$Body|ConvertTo-Json -Depth 35 -Compress}
  try{$taskResult=Invoke-RestMethod @taskParameters}catch{throw "HTTP failed for $Path; sensitive details suppressed."}
  if($taskResult.code -ne 0){throw "API failed for $Path, code=$($taskResult.code). Existing data is not overwritten."};return $taskResult.data
}
try{
  $taskLogin=Invoke-TaskApi '/system/auth/login' 'Post' @{username=$taskAccount.username;password=$taskAccount.password}
  if(!$taskLogin.accessToken){throw 'Administrator login failed.'};$taskHeaders.Authorization='Bearer '+$taskLogin.accessToken
  # Inspect the entire baseline before creating anything. A manual edit must not be silently reset.
  $taskMissing=[Collections.Generic.List[object]]::new()
  foreach($taskItem in $taskItems){
    $taskPage=Invoke-TaskApi ("/doormes/catalog/page?keyword=$($taskItem.code)&pageNo=1&pageSize=100")
    $taskExisting=@($taskPage.list|Where-Object {$_.item.code -eq $taskItem.code})
    if($taskExisting.Count -gt 1){throw 'Duplicate example baseline; inspect first.'}
    if($taskExisting.Count -eq 1){
      $taskDoc=$taskExisting[0]
      $taskMatches=$taskDoc.status -eq 'PUBLISHED'
      foreach($taskField in @('category','name','specification','note','materialFamily','baseColor','metalness','roughness','opacity','thicknessMm')){if($taskDoc.item.$taskField -ne $taskItem[$taskField]){$taskMatches=$false}}
      if(($taskDoc.item.compatibleProfileSystemIds -join ',') -ne ($taskItem.compatibleProfileSystemIds -join ',')){$taskMatches=$false}
      if(!$taskMatches){throw "Example $($taskItem.code) has a draft or manual changes; refusing automatic replacement."}
      $taskEvidence.Add(@{code=$taskItem.code;id=$taskDoc.id;revision=$taskDoc.revision;action='retained'})
    }else{$taskMissing.Add($taskItem)}
  }
  foreach($taskItem in $taskMissing){
    $taskCreated=Invoke-TaskApi '/doormes/catalog/create' 'Post' @{expectedRevision=0;changeNote='初始化人工测试示例';item=$taskItem}
    $taskEvidence.Add(@{code=$taskItem.code;id=$taskCreated.id;revision=$taskCreated.revision;action='created-draft'})
    $taskPublished=Invoke-TaskApi ("/doormes/catalog/publish?id=$($taskCreated.id)") 'Post' @{expectedRevision=$taskCreated.revision;note='发布示例设计选型，非正式制造目录'}
    if($taskPublished.data.productionReady -or $taskPublished.data.scope -ne 'design-only'){throw 'Unexpected example publication scope.'}
    $taskEvidence.Add(@{code=$taskItem.code;id=$taskPublished.id;revision=$taskPublished.revision;action='published-design-only'})
  }
  "Example initialization verified: $($taskItems.Count) published design-only models. No existing model overwritten."
}finally{
  $taskReport=Join-Path $taskRoot ('runtime-local/design-data/catalog-examples-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.json')
  [IO.File]::WriteAllText($taskReport,(@{database='doormes_local';tenantId=1;at=(Get-Date).ToString('o');scope='design-only';productionReady=$false;records=@($taskEvidence.ToArray())}|ConvertTo-Json -Depth 15),[Text.UTF8Encoding]::new($false))
  if($taskHeaders.Authorization){try{Invoke-TaskApi '/system/auth/logout' 'Post'|Out-Null}catch{Write-Warning 'Test administrator logout failed; token omitted.'}}
  "Non-secret initialization report: $taskReport"
}
