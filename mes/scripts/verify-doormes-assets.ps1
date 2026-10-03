[CmdletBinding()]param([string]$BaseUrl='http://192.168.50.193:5180')
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot=Get-DoormesWorkspaceRoot;$taskUri=[Uri]$BaseUrl
if($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0' -or $taskUri.Scheme -ne 'http' -or $taskUri.Host -notin @('localhost','127.0.0.1','192.168.50.193') -or $taskUri.Port -ne 5180){throw 'Only the isolated MES proxy is allowed.'}
$taskSchema=Get-Content (Join-Path $taskRoot 'runtime-local/design-data/schema-v004.json') -Raw|ConvertFrom-Json
if($taskSchema.database -ne 'doormes_local'){throw 'Unexpected schema.'}
$taskRun='M1-AST-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N').Substring(0,6)
$taskArchive=Join-Path $taskRoot "runtime-local/scenarios/$taskRun";[IO.Directory]::CreateDirectory($taskArchive)|Out-Null
Copy-Item -LiteralPath (Join-Path $taskRoot 'tests/scenarios/M1-assets.v1.json') -Destination (Join-Path $taskArchive 'scenario-case.json')
& 'C:\Program Files\nodejs\node.exe' (Join-Path $taskRoot 'tools/create-asset-fixtures.mjs') (Join-Path $taskArchive 'fixtures');if($LASTEXITCODE -ne 0){throw 'Fixture generation failed.'}
$taskAccounts=Get-Content (Join-Path $taskRoot 'runtime-local/factory-bootstrap/accounts.local.json') -Raw|ConvertFrom-Json
$taskHeaders=@{anonymous=@{'tenant-id'='1'}};$taskEvidence=[Collections.Generic.List[object]]::new();$taskIds=[Collections.Generic.List[string]]::new();$taskCatalogIds=[Collections.Generic.List[string]]::new();$taskAssetIds=[Collections.Generic.List[string]]::new();$taskPassed=$false
function Save-TaskJson([string]$Name,[object]$Value){[IO.File]::WriteAllText((Join-Path $taskArchive $Name),($Value|ConvertTo-Json -Depth 75),[Text.UTF8Encoding]::new($false))}
function Api([string]$Role,[string]$Path,[string]$Method='Get',[object]$Body=$null,[string]$Media='application/json;charset=utf-8'){
 $taskParams=@{Uri="$BaseUrl/admin-api$Path";Headers=$taskHeaders[$Role];Method=$Method;TimeoutSec=45}
 if($null -ne $Body){$taskParams.ContentType=$Media;if($Body -is [byte[]]){$taskParams.Body=$Body}else{$taskParams.Body=$Body|ConvertTo-Json -Depth 75 -Compress}}
 try{$taskResponse=Invoke-RestMethod @taskParams}catch{throw "HTTP failed for $Path; sensitive details suppressed."}
 if($taskResponse.code -eq 0 -and $taskResponse.data.id){if($Path -eq '/doormes/catalog/create'){$taskCatalogIds.Add($taskResponse.data.id)}elseif($Path -eq '/doormes/requirements/create'){$taskIds.Add($taskResponse.data.id)}}
 return $taskResponse
}
function Assert([string]$Id,[bool]$Result,[string]$Description){$taskEvidence.Add(@{id=$Id;result=$(if($Result){'PASS'}else{'FAIL'});description=$Description});if(!$Result){throw "Assertion failed: $Id ($Description)."}}
function Expect([string]$Id,[int]$Code,[object]$Result){Assert $Id ($Result.code -eq $Code) "code=$($Result.code), expected=$Code; $($Result.msg)"}
function Declare([string]$Id,[string]$Media,[byte[]]$Bytes){return @{assetId=$Id;mediaType=$Media;byteLength=$Bytes.Length;expectedContentHash='sha256:'+([Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($Bytes))).ToLowerInvariant()}}
function Upload([string]$Role,[hashtable]$Declaration,[byte[]]$Bytes){$taskAssetIds.Add($Declaration.assetId);$taskGrant=Api $Role '/doormes/visual-assets/authorize' 'Post' $Declaration;if($taskGrant.code -ne 0){throw 'Grant rejected.'};return Api $Role ('/doormes/visual-assets/uploads/'+$taskGrant.data.grantId) 'Put' $Bytes $Declaration.mediaType}
try{
 foreach($taskRole in @('admin','sales','design','purchase')){$taskAccount=@($taskAccounts.accounts|Where-Object username -eq $taskRole)[0];$taskHeaders[$taskRole]=@{'tenant-id'='1'};$taskLogin=Api $taskRole '/system/auth/login' 'Post' @{username=$taskAccount.username;password=$taskAccount.password};if($taskLogin.code -ne 0 -or !$taskLogin.data.accessToken){throw "Login failed for $taskRole."};$taskHeaders[$taskRole].Authorization='Bearer '+$taskLogin.data.accessToken}
 $taskModel=[IO.File]::ReadAllBytes((Join-Path $taskArchive 'fixtures/triangle.gltf'));$taskGlb=[IO.File]::ReadAllBytes((Join-Path $taskArchive 'fixtures/triangle.glb'));$taskPng=[IO.File]::ReadAllBytes((Join-Path $taskArchive 'fixtures/checker.png'))
 $taskDeclaration=Declare "TEST-$taskRun-MODEL" 'model/gltf+json' $taskModel
 Expect 'ASSERT-01' 401 (Api 'anonymous' '/doormes/visual-assets/authorize' 'Post' $taskDeclaration)
 Expect 'ASSERT-02' 403 (Api 'sales' '/doormes/visual-assets/authorize' 'Post' $taskDeclaration)
 $taskAssetIds.Add($taskDeclaration.assetId);$taskGrant=Api 'design' '/doormes/visual-assets/authorize' 'Post' $taskDeclaration;Expect 'ASSERT-03' 0 $taskGrant;$taskGrantId=$taskGrant.data.grantId
 Expect 'ASSERT-04' 403 (Api 'purchase' "/doormes/visual-assets/uploads/$taskGrantId" 'Put' $taskModel 'model/gltf+json')
 Expect 'ASSERT-05' 400 (Api 'design' "/doormes/visual-assets/uploads/$taskGrantId" 'Put' $taskModel 'image/png')
 Expect 'ASSERT-06' 400 (Api 'design' "/doormes/visual-assets/uploads/$taskGrantId" 'Put' ([Text.Encoding]::UTF8.GetBytes('{}')) 'model/gltf+json')
 $taskUploaded=Api 'design' "/doormes/visual-assets/uploads/$taskGrantId" 'Put' $taskModel 'model/gltf+json';Expect 'ASSERT-07' 0 $taskUploaded
 Expect 'ASSERT-08' 404 (Api 'design' "/doormes/visual-assets/uploads/$taskGrantId" 'Put' $taskModel 'model/gltf+json')
 $taskBinary=Upload 'design' (Declare "TEST-$taskRun-GLB" 'model/gltf-binary' $taskGlb) $taskGlb;Expect 'ASSERT-09' 0 $taskBinary
 $taskTexture=Upload 'purchase' (Declare "MES-TEXTURE-TEST-$taskRun-TEX" 'image/png' $taskPng) $taskPng;Expect 'ASSERT-10' 0 $taskTexture
 Assert 'ASSERT-11' ($taskTexture.data.inspection.width -eq 4 -and $taskTexture.data.inspection.height -eq 4 -and $taskUploaded.data.inspection.estimatedTriangleCount -eq 1 -and $taskBinary.data.inspection.estimatedTriangleCount -eq 1) 'Server inspected model triangles and actually decoded PNG'
 $taskRepeated=Upload 'design' $taskDeclaration $taskModel;Assert 'ASSERT-12' ($taskRepeated.code -eq 0 -and $taskRepeated.data.asset.storedAtIso -eq $taskUploaded.data.asset.storedAtIso) 'Identical repeat upload is idempotent'
 Expect 'ASSERT-13' 409 (Api 'design' '/doormes/visual-assets/authorize' 'Post' (Declare $taskDeclaration.assetId 'model/gltf+json' ([Text.Encoding]::UTF8.GetBytes('{}'))))
 foreach($taskAsset in @($taskUploaded.data.asset,$taskBinary.data.asset,$taskTexture.data.asset)){$taskDownload=Join-Path $taskArchive ($taskAsset.assetId+'.bin');Invoke-WebRequest -Uri "$BaseUrl/admin-api/doormes/visual-assets/content?assetId=$($taskAsset.assetId)&contentHash=$($taskAsset.contentHash)" -Headers $taskHeaders.sales -OutFile $taskDownload -TimeoutSec 30|Out-Null;Assert ('ASSERT-14-'+$taskAsset.kind+'-'+$taskAsset.mediaType) ('sha256:'+(Get-FileHash -LiteralPath $taskDownload).Hash.ToLowerInvariant() -eq $taskAsset.contentHash) 'Sales can read exact immutable bytes';Save-TaskJson ($taskAsset.assetId+'.json') $taskAsset}
 Expect 'ASSERT-15' 409 (Api 'design' ("/doormes/visual-assets/content?assetId=$($taskDeclaration.assetId)&contentHash=sha256:"+'f'*64))
 $taskExternal=[IO.File]::ReadAllBytes((Join-Path $taskArchive 'fixtures/external.gltf'));Expect 'ASSERT-16' 400 (Upload 'design' (Declare "TEST-$taskRun-EXTERNAL" 'model/gltf+json' $taskExternal) $taskExternal)
 Expect 'ASSERT-17' 400 (Upload 'design' (Declare "MES-TEXTURE-TEST-$taskRun-BAD" 'image/png' $taskModel) $taskModel)
 $taskItem=@{category='finish';code="TEST-$taskRun-FIN";name='测试棋盘表面';specification='本地颜色纹理';note='仅受控场景';materialFamily='metal';baseColor='#ffffff';metalness=.2;roughness=.5;opacity=1;thicknessMm=$null;compatibleProfileSystemIds=@();textureSetId=$taskTexture.data.asset.assetId;textureContentHash=$taskTexture.data.asset.contentHash;textureRepeatX=.5;textureRepeatY=.75}
 $taskCatalog=Api 'design' '/doormes/catalog/create' 'Post' @{expectedRevision=0;changeNote='本地纹理目录';item=$taskItem};Expect 'ASSERT-18' 0 $taskCatalog;$taskCatalogId=$taskCatalog.data.id
 $taskPublished=Api 'admin' "/doormes/catalog/publish?id=$taskCatalogId" 'Post' @{expectedRevision=1;note='发布测试选型'};Expect 'ASSERT-19' 0 $taskPublished
 $taskDemand=@{schemaVersion='doormes-design-demand.v1';number="TEST-$taskRun";customer='资产往返测试';project='仅场景';note='';lines=@(@{mark='C1';kind='custom';quantity=1;requirement=@{widthMm=1200;heightMm=1500;material='AL70';glass='24mm';hardware='设计待选';finish='纹理测试';dueDate='2026-10-15';note=''}})}
 $taskReq=Api 'sales' '/doormes/requirements/create' 'Post' @{expectedRevision=0;changeNote='创建测试需求';demand=$taskDemand};Expect 'ASSERT-20' 0 $taskReq;$taskReqId=$taskReq.data.id
 $taskSubmit=Api 'sales' "/doormes/requirements/submit?id=$taskReqId" 'Post' @{expectedRevision=1;note='提交'};Expect 'ASSERT-21' 0 $taskSubmit
 $taskClaim=Api 'design' "/doormes/requirements/claim?id=$taskReqId" 'Post' @{expectedRevision=2;note='领用'};Expect 'ASSERT-22' 0 $taskClaim
 $taskDrawing=Api 'design' '/doormes/drawings/open' 'Post' @{requirementId=$taskReqId;lineId=$taskClaim.data.demand.lines[0].id;expectedRequirementRevision=3};Expect 'ASSERT-23' 0 $taskDrawing;$taskDrawingId=$taskDrawing.data.id
 $taskInput=Join-Path $taskArchive 'apply-texture-input.json';$taskOutput=Join-Path $taskArchive 'apply-texture-output.json'
 Save-TaskJson 'apply-texture-input.json' @{mode='apply-catalog';document=$taskDrawing.data.document;windowId=$taskDrawing.data.document.windows[0].objectId;target='profile-outside';choice=$taskPublished.data}
 & 'C:\Program Files\nodejs\node.exe' (Join-Path $taskRoot 'frontend/vendor/doormes-engine/dist/validator.mjs') $taskInput $taskOutput;if($LASTEXITCODE -ne 0){throw 'Shared texture mapping failed.'}
 $taskChanged=(Get-Content $taskOutput -Raw|ConvertFrom-Json).document
 $taskSaved=Api 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=1;changeNote='应用真实纹理及小数重复';document=$taskChanged};Expect 'ASSERT-24' 0 $taskSaved
 $taskReload=Api 'sales' "/doormes/drawings/get?id=$taskDrawingId";Assert 'ASSERT-25' ($taskReload.code -eq 0 -and $taskReload.data.document.windows[0].visualConfiguration.appearance.frame.outside.textureSetId -eq $taskTexture.data.asset.assetId -and $taskReload.data.document.windows[0].visualConfiguration.appearance.frame.outside.uvScale.x -eq .5 -and $taskReload.data.document.windows[0].visualConfiguration.appearance.frame.outside.uvScale.y -eq .75) 'Reload retains texture identity and decimal repeat'
 $taskOld=Api 'design' "/doormes/drawings/get?id=$taskDrawingId&revision=1";Assert 'ASSERT-26' ($taskOld.code -eq 0 -and $taskOld.data.document.windows[0].visualConfiguration.appearance.frame.outside.textureSetId -ne $taskTexture.data.asset.assetId) 'Original drawing version unchanged'
 $taskInvalid=$taskChanged|ConvertTo-Json -Depth 75|ConvertFrom-Json;$taskInvalid.windows[0].visualConfiguration.appearance.frame.outside.appearanceId='custom-texture';$taskInvalid.windows[0].visualConfiguration.appearance.frame.outside.textureContentHash='sha256:'+'f'*64
 Expect 'ASSERT-27' 400 (Api 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=2;changeNote='非法纹理哈希';document=$taskInvalid})
 $taskItem.textureSetId='MES-TEXTURE-MISSING';$taskItem.code="TEST-$taskRun-MISSING";Expect 'ASSERT-28' 404 (Api 'design' '/doormes/catalog/create' 'Post' @{expectedRevision=0;changeNote='缺失资源';item=$taskItem})
 $taskModelDocument=$taskChanged|ConvertTo-Json -Depth 75|ConvertFrom-Json
 $taskHandle=@($taskModelDocument.windows[0].visualConfiguration.hardwareModels|Where-Object role -eq 'handle')[0]
 $taskHandle.model.geometry=@{kind='gltf';assetId=$taskUploaded.data.asset.assetId;contentHash=$taskUploaded.data.asset.contentHash;lodAssetIds=@($taskBinary.data.asset.assetId);lodAssets=@(@{quality='medium';assetId=$taskBinary.data.asset.assetId;contentHash=$taskBinary.data.asset.contentHash});importConfiguration=@{schemaVersion='doormes-component-import.v1';sourceUnit='meter';upAxis='+y';forwardAxis='+z'}}
 $taskModelDocument.revision=[int]$taskModelDocument.revision+1
 $taskModelSaved=Api 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=2;changeNote='固定模型与GLB中清版本';document=$taskModelDocument};Expect 'ASSERT-31' 0 $taskModelSaved
 $taskReload=Api 'sales' "/doormes/drawings/get?id=$taskDrawingId"
 $taskReloadHandle=@($taskReload.data.document.windows[0].visualConfiguration.hardwareModels|Where-Object role -eq 'handle')[0]
 Assert 'ASSERT-32' ($taskReload.code -eq 0 -and $taskReloadHandle.model.geometry.assetId -eq $taskUploaded.data.asset.assetId -and $taskReloadHandle.model.geometry.contentHash -eq $taskUploaded.data.asset.contentHash -and $taskReloadHandle.model.geometry.lodAssets[0].contentHash -eq $taskBinary.data.asset.contentHash) 'Model and LOD exact identities survive shared parser and backend reload'
 $taskBeforeModel=Api 'design' "/doormes/drawings/get?id=$taskDrawingId&revision=2"
 Assert 'ASSERT-33' (@($taskBeforeModel.data.document.windows[0].visualConfiguration.hardwareModels|Where-Object role -eq 'handle')[0].model.geometry.kind -eq 'parametric') 'Model change preserves previous drawing version'
 $taskInvalidLod=$taskModelDocument|ConvertTo-Json -Depth 75|ConvertFrom-Json;$taskInvalidLod.windows[0].visualConfiguration.hardwareModels|Where-Object role -eq 'handle'|ForEach-Object {$_.model.geometry.lodAssets[0].contentHash='sha256:'+'f'*64}
 Expect 'ASSERT-34' 400 (Api 'design' "/doormes/drawings/save?id=$taskDrawingId" 'Put' @{expectedRevision=3;changeNote='伪造LOD资源';document=$taskInvalidLod})
 $taskDb=Invoke-DoormesAdminQuery -Sql "SELECT asset_id,blob_id,content_hash,metadata_hash FROM doormes_local.dm_visual_asset WHERE tenant_id=1 AND asset_id LIKE '%$taskRun%' ORDER BY asset_id; SELECT COUNT(*) FROM doormes_local.dm_drawing_version WHERE tenant_id=1 AND drawing_id='$taskDrawingId';"
 [IO.File]::WriteAllText((Join-Path $taskArchive 'database-evidence.txt'),$taskDb,[Text.UTF8Encoding]::new($false));$taskDbLines=@($taskDb -split '\r?\n');Assert 'ASSERT-29' ($taskDbLines.Count -eq 4 -and $taskDbLines[-1] -eq '3') 'Three valid resources, rejected saves do not create versions'
 $taskHashMatch=$true;foreach($taskRow in $taskDbLines[0..2]){$taskValues=$taskRow -split "`t";$taskFolder=Join-Path $taskRoot "runtime-local/design-data/assets/tenant-1/$($taskValues[1])";if('sha256:'+(Get-FileHash (Join-Path $taskFolder 'content.bin')).Hash.ToLowerInvariant() -ne $taskValues[2] -or (Get-FileHash (Join-Path $taskFolder 'descriptor.json')).Hash.ToLowerInvariant() -ne $taskValues[3]){$taskHashMatch=$false}}
 Assert 'ASSERT-30' $taskHashMatch 'Every binary and descriptor JSON hash matches SQL'
 Save-TaskJson 'drawing-final.json' $taskReload.data;Save-TaskJson 'catalog-r2.json' $taskPublished.data;$taskPassed=$true
}finally{
 Save-TaskJson 'manifest.json' @{scenarioId=$taskRun;tenantId=1;database='doormes_local';ids=@($taskIds);catalogIds=@($taskCatalogIds);assetIds=@($taskAssetIds|Select-Object -Unique);baseUrl=$BaseUrl;passed=$taskPassed;browserUsed=$false;testedAt=(Get-Date).ToString('o');assertions=@($taskEvidence)}
 foreach($taskRole in $taskHeaders.Keys){if($taskHeaders[$taskRole].Authorization){try{$null=Api $taskRole '/system/auth/logout' 'Post' @{}}catch{Write-Warning "Test logout failed for $taskRole."}}}
}
"Asset scenario passed; evidence: $taskArchive"
