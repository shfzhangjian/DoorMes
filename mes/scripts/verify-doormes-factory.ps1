[CmdletBinding()]
param([string]$BaseUrl = 'http://localhost:5180')
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskUri = [Uri]$BaseUrl
if ($taskUri.Scheme -ne 'http' -or $taskUri.Host -notin @('localhost','127.0.0.1','192.168.50.193') -or $taskUri.Port -ne 5180) { throw 'Only the isolated DoorMes frontend proxy is allowed.' }
$taskManifest = Get-Content (Join-Path $taskRoot 'runtime-local/factory-bootstrap/complete.json') -Raw | ConvertFrom-Json
if ($taskManifest.database -ne 'doormes_local') { throw 'Unexpected bootstrap identity.' }
$taskAuth = Get-Content (Join-Path $taskRoot 'runtime-local/factory-bootstrap/accounts.local.json') -Raw | ConvertFrom-Json
$taskExpected = @{
  admin=@('workbench','orders','design','catalog','feasibility','production','changes','drawings')
  sales=@('workbench','orders','catalog','drawings')
  design=@('workbench','orders','design','catalog','changes','drawings')
  process=@('workbench','design','feasibility','production','changes','drawings')
  purchase=@('workbench','catalog','feasibility','drawings')
  production=@('workbench','production','changes','drawings')
  reviewer=@('workbench','orders','design','feasibility','changes','drawings')
}
function Invoke-TaskApi([string]$Path,[hashtable]$Headers,[string]$Method='Get',[object]$Body=$null) {
  $taskParams=@{Uri="$BaseUrl/admin-api$Path";Headers=$Headers;Method=$Method;TimeoutSec=20}
  if ($null -ne $Body) { $taskParams.ContentType='application/json; charset=utf-8'; $taskParams.Body=($Body | ConvertTo-Json -Compress) }
  # Never dump login bodies, returned tokens or exception responses into the report.
  try { Invoke-RestMethod @taskParams } catch { throw "HTTP request failed for $Path; sensitive details suppressed." }
}
$taskTenant=Invoke-TaskApi '/system/tenant/simple-list' @{}
if ($taskTenant.code -ne 0 -or @($taskTenant.data).Count -ne 1 -or $taskTenant.data[0].name -ne '门窗工厂') { throw 'Factory tenant list verification failed.' }
foreach($taskHost in @('localhost','127.0.0.1','192.168.50.193')) {
  $taskDomain=Invoke-TaskApi ("/system/tenant/get-by-website?website=$taskHost") @{}
  if ($taskDomain.code -ne 0 -or $taskDomain.data.id -ne 1 -or $taskDomain.data.name -ne '门窗工厂') { throw "Factory domain binding failed for $taskHost." }
}
$taskRows=@()
foreach($taskAccount in $taskAuth.accounts) {
  $taskHeaders=@{'tenant-id'='1'}
  $taskLogin=Invoke-TaskApi '/system/auth/login' $taskHeaders 'Post' @{username=$taskAccount.username;password=$taskAccount.password}
  if ($taskLogin.code -ne 0 -or !$taskLogin.data.accessToken -or $taskLogin.data.userId -ne $taskAccount.userId) { throw "Login failed for $($taskAccount.username), code=$($taskLogin.code)." }
  $taskHeaders.Authorization='Bearer '+$taskLogin.data.accessToken
  try {
    $taskInfo=Invoke-TaskApi '/system/auth/get-permission-info' $taskHeaders
    if ($taskInfo.code -ne 0 -or $taskInfo.data.user.username -ne $taskAccount.username) { throw 'Permission identity verification failed.' }
    $taskFactory=@($taskInfo.data.menus | Where-Object id -eq 96000000)
    if ($taskFactory.Count -ne 1) { throw 'Factory menu root missing.' }
    $taskPaths=@($taskFactory[0].children.path | Sort-Object)
    if (Compare-Object $taskPaths @($taskExpected[$taskAccount.username] | Sort-Object)) { throw "Menu grants mismatch for $($taskAccount.username)." }
    if ($taskAccount.username -eq 'admin') {
      if (@($taskInfo.data.menus).Count -ne 3 -or 'super_admin' -notin $taskInfo.data.roles) { throw 'Administrator foundation/backup permissions missing.' }
      $taskUsers=Invoke-TaskApi '/system/user/page?pageNo=1&pageSize=100' $taskHeaders
      $taskDepts=Invoke-TaskApi '/system/dept/list' $taskHeaders
      if ($taskUsers.code -ne 0 -or $taskUsers.data.total -ne 7 -or $taskDepts.code -ne 0 -or @($taskDepts.data).Count -ne 8) { throw 'User/organization verification failed.' }
    } else {
      if (@($taskInfo.data.menus).Count -ne 1 -or @($taskInfo.data.permissions | Where-Object { ![string]::IsNullOrWhiteSpace($_) -and $_ -notlike 'doormes:*' }).Count -ne 0 -or @($taskInfo.data.roles).Count -ne 1 -or $taskInfo.data.roles[0] -ne "factory_$($taskAccount.username)") { throw 'Business role unexpectedly received legacy or administrative permissions.' }
      # The inherited simple-user selector remains available for business assignments;
      # the protected full-user detail must still be denied.
      $taskDenied=Invoke-TaskApi '/system/user/get?id=1001' $taskHeaders
      if ($taskDenied.code -ne 403) { throw 'Business account unexpectedly accessed protected administration.' }
    }
    $taskRows += [pscustomobject]@{username=$taskAccount.username;login='PASS';roles=$taskInfo.data.roles;roots=@($taskInfo.data.menus.name);modules=$taskPaths;permissions='PASS'}
  } finally {
    $taskLogout=Invoke-TaskApi '/system/auth/logout' $taskHeaders 'Post' @{}
    if ($taskLogout.code -ne 0) { throw 'Test login cleanup failed.' }
  }
}
$taskReport=@{testedAt=(Get-Date).ToString('o');baseUrl=$BaseUrl;database='doormes_local';factory='门窗工厂';accounts=$taskRows;browserUsed=$false;businessWorkflowVerified=$false}
$taskReportFile=Join-Path $taskRoot ('runtime-local/factory-bootstrap/verification-'+$taskUri.Host+'.json')
[IO.File]::WriteAllText($taskReportFile,($taskReport | ConvertTo-Json -Depth 7),[Text.UTF8Encoding]::new($false))
$taskRows | Select-Object username,login,permissions,@{Name='MenuCount';Expression={$_.modules.Count}}
"Verification report: $taskReportFile"
