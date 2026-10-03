[CmdletBinding()]
param([switch]$Apply, [string]$FactoryName = '门窗工厂')
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot = Get-DoormesWorkspaceRoot
if ($taskRoot -ne 'E:\doorMES_v1\LUCK-MES-HC-0') { throw 'Unexpected workspace; refusing account cleanup.' }
if ($FactoryName.Length -lt 1 -or $FactoryName.Length -gt 20) { throw 'Factory name must be 1-20 characters.' }
$taskMarker = Join-Path $taskRoot 'runtime-local/factory-bootstrap/complete.json'
if (Test-Path -LiteralPath $taskMarker) { throw 'Factory accounts are already initialized. Use normal user/role management, not this one-time script.' }
if (!(Test-Path -LiteralPath (Join-Path $taskRoot 'runtime-local/db-clone/20261002-075035/clone-complete.json'))) { throw 'Verified clone is required before cleanup.' }
$taskCounts = Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_type='BASE TABLE'; SELECT COUNT(*) FROM doormes_local.system_menu WHERE id BETWEEN 96000000 AND 96000999; SELECT COUNT(*) FROM doormes_local.system_users WHERE id>=1001; SELECT COUNT(*) FROM doormes_local.system_dept WHERE id>=1001; SELECT COUNT(*) FROM doormes_local.system_role WHERE id>=1001; SELECT COUNT(*) FROM doormes_local.system_post WHERE id>=1001; SELECT COUNT(*) FROM doormes_local.system_tenant_package WHERE id=2000; SELECT COUNT(*) FROM doormes_local.system_role WHERE id=1 AND code='super_admin' AND tenant_id=1;"
$taskChecks = @($taskCounts -split '\r?\n' | ForEach-Object { [long]$_ })
if ($taskChecks.Count -ne 8 -or $taskChecks[0] -ne 694 -or $taskChecks[7] -ne 1 -or ($taskChecks[1..6] | Where-Object { $_ -ne 0 })) { throw 'Unexpected database baseline or reserved IDs; refusing destructive changes.' }
$taskProfiles = @(
  @{username='admin';nickname='系统管理员';roleCode='super_admin';roleName='门窗厂系统管理员';deptName='系统管理';menus=@(1,2,3,4,5,6,7,8)},
  @{username='sales';nickname='销售测试员';roleCode='factory_sales';roleName='门窗销售';deptName='销售部';menus=@(1,2,4,8)},
  @{username='design';nickname='设计研发测试员';roleCode='factory_design';roleName='门窗设计研发';deptName='设计研发部';menus=@(1,2,3,4,7,8)},
  @{username='process';nickname='工艺测试员';roleCode='factory_process';roleName='门窗工艺';deptName='工艺技术部';menus=@(1,3,5,6,7,8)},
  @{username='purchase';nickname='采购测试员';roleCode='factory_purchase';roleName='门窗采购';deptName='采购部';menus=@(1,4,5,8)},
  @{username='production';nickname='生产测试员';roleCode='factory_production';roleName='门窗生产';deptName='生产部';menus=@(1,6,7,8)},
  @{username='reviewer';nickname='审核测试员';roleCode='factory_reviewer';roleName='门窗审核';deptName='审核与质量部';menus=@(1,2,3,5,7,8)}
)
if (!$Apply) {
  [pscustomobject]@{Database='doormes_local';Factory=$FactoryName;Accounts=@($taskProfiles.username);Departments=8;BackupRequired=$true;Action='Preview only; -Apply performs the authorized cleanup and initialization'} | ConvertTo-Json -Depth 4
  return
}
function ConvertTo-TaskSqlText([string]$Value) {
  # Hex literals avoid SQL-mode-dependent quoting and preserve UTF-8 Chinese text.
  return "CONVERT(0x$([Convert]::ToHexString([Text.Encoding]::UTF8.GetBytes($Value))) USING utf8mb4)"
}
$taskStamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$taskBackup = Join-Path $taskRoot "runtime-local/factory-bootstrap/$taskStamp"
[IO.Directory]::CreateDirectory($taskBackup) | Out-Null
$taskBackupSql = Join-Path $taskBackup 'before-factory.sql'
$taskTables = @('system_menu','system_users','system_dept','system_post','system_role','system_user_role','system_user_post','system_role_menu','system_tenant','system_tenant_package','system_oauth2_access_token','system_oauth2_refresh_token','system_oauth2_code','system_oauth2_approve')
$taskEngineCheck = Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='doormes_local' AND table_name IN ('$($taskTables -join "','")') AND engine='InnoDB';"
if ([int]$taskEngineCheck -ne $taskTables.Count) { throw 'All modified tables must support transactional recovery.' }
$taskDbUser = $env:DOORMES_ADMIN_DB_USERNAME
$taskDbPassword = $env:DOORMES_ADMIN_DB_PASSWORD
if (!$taskDbUser -or !$taskDbPassword) { throw 'Local database administrative environment variables are required.' }
$taskDumpInfo = [Diagnostics.ProcessStartInfo]::new('C:\Program Files\MySQL\MySQL Server 5.7\bin\mysqldump.exe')
$taskDumpInfo.UseShellExecute = $false
$taskDumpInfo.CreateNoWindow = $true
$taskDumpInfo.RedirectStandardOutput = $true
$taskDumpInfo.RedirectStandardError = $true
$taskDumpInfo.Environment['MYSQL_PWD'] = $taskDbPassword
foreach ($taskArg in @('--host=127.0.0.1','--port=3306',"--user=$taskDbUser",'--default-character-set=utf8mb4','--single-transaction','--skip-lock-tables','--hex-blob','--set-gtid-purged=OFF','doormes_local') + $taskTables) { $taskDumpInfo.ArgumentList.Add($taskArg) }
$taskDumpProcess = [Diagnostics.Process]::Start($taskDumpInfo)
$taskDumpErrors = $taskDumpProcess.StandardError.ReadToEndAsync()
$taskOutputFile = [IO.File]::Create($taskBackupSql)
try { $taskDumpProcess.StandardOutput.BaseStream.CopyTo($taskOutputFile) } finally { $taskOutputFile.Dispose() }
$taskDumpProcess.WaitForExit()
if ($taskDumpProcess.ExitCode -ne 0 -or (Get-Item -LiteralPath $taskBackupSql).Length -lt 1000) { throw 'Pre-change backup failed; no account or menu changes made.' }
$taskOldCounts = Invoke-DoormesAdminQuery -Sql "SELECT 'users',COUNT(*),MAX(id) FROM doormes_local.system_users; SELECT 'departments',COUNT(*),MAX(id) FROM doormes_local.system_dept; SELECT 'menus',COUNT(*),MAX(id) FROM doormes_local.system_menu;"
[IO.File]::WriteAllText((Join-Path $taskBackup 'before-counts.tsv'), $taskOldCounts, [Text.UTF8Encoding]::new($false))

# Hash using the same BCrypt implementation as Spring Security, offline.
$taskPasswords = @($taskProfiles | ForEach-Object { 'Dm!' + [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(9)).Replace('+','K').Replace('/','Z') + '9' })
$taskHashInfo = [Diagnostics.ProcessStartInfo]::new('D:\Java\jdk-17.0.12\bin\java.exe')
$taskHashInfo.UseShellExecute = $false
$taskHashInfo.CreateNoWindow = $true
$taskHashInfo.RedirectStandardInput = $true
$taskHashInfo.RedirectStandardOutput = $true
$taskHashInfo.RedirectStandardError = $true
foreach ($taskArg in @('-cp','C:\Users\LENOVO\.m2\repository\org\springframework\security\spring-security-crypto\6.5.7\spring-security-crypto-6.5.7.jar',(Join-Path $taskRoot 'tools/FactoryAccountPasswordTool.java'))) { $taskHashInfo.ArgumentList.Add($taskArg) }
$taskHashProcess = [Diagnostics.Process]::Start($taskHashInfo)
$taskHashErrors = $taskHashProcess.StandardError.ReadToEndAsync()
$taskHashOutput = $taskHashProcess.StandardOutput.ReadToEndAsync()
$taskHashProcess.StandardInput.Write(($taskPasswords -join "`n") + "`n")
$taskHashProcess.StandardInput.Close()
$taskHashProcess.WaitForExit()
$taskHashes = @($taskHashOutput.GetAwaiter().GetResult().Trim() -split '\r?\n')
if ($taskHashProcess.ExitCode -ne 0 -or $taskHashes.Count -ne 7 -or ($taskHashes | Where-Object { $_ -notmatch '^\$2a\$10\$[./A-Za-z0-9]{53}$' })) { throw 'Password hashing failed; no database changes made.' }
$taskAccounts = @()
for ($taskIndex=0; $taskIndex -lt $taskProfiles.Count; $taskIndex++) {
  $taskAccounts += [pscustomobject]@{username=$taskProfiles[$taskIndex].username;nickname=$taskProfiles[$taskIndex].nickname;password=$taskPasswords[$taskIndex];role=$taskProfiles[$taskIndex].roleName;department=$taskProfiles[$taskIndex].deptName;userId=(1001+$taskIndex)}
}
$taskCredentialsPath = Join-Path $taskRoot 'runtime-local/factory-bootstrap/accounts.local.json'
[IO.File]::WriteAllText($taskCredentialsPath, (@{factory=$FactoryName;tenantId=1;accounts=$taskAccounts} | ConvertTo-Json -Depth 5), [Text.UTF8Encoding]::new($false))
# Local secrets and recovery exports are ignored by Git; grant only this Windows user and SYSTEM.
foreach ($taskSecretPath in @($taskCredentialsPath,$taskBackupSql)) {
  $taskSecretFile = [IO.FileInfo]::new($taskSecretPath)
  $taskAcl = [IO.FileSystemAclExtensions]::GetAccessControl($taskSecretFile,[Security.AccessControl.AccessControlSections]::Access)
  $taskAcl.SetAccessRuleProtection($true,$false)
  $taskAcl.SetAccessRule([Security.AccessControl.FileSystemAccessRule]::new([Security.Principal.WindowsIdentity]::GetCurrent().User,'FullControl','Allow'))
  # Sandboxed shells run as a separate account; the actual operator must retain access.
  $taskOperatorSid = [Security.Principal.NTAccount]::new('LENOVO').Translate([Security.Principal.SecurityIdentifier])
  $taskAcl.SetAccessRule([Security.AccessControl.FileSystemAccessRule]::new($taskOperatorSid,'FullControl','Allow'))
  $taskAcl.SetAccessRule([Security.AccessControl.FileSystemAccessRule]::new([Security.Principal.SecurityIdentifier]::new('S-1-5-18'),'FullControl','Allow'))
  [IO.FileSystemAclExtensions]::SetAccessControl($taskSecretFile,$taskAcl)
}

$taskSql = [Text.StringBuilder]::new()
[void]$taskSql.AppendLine('USE doormes_local; START TRANSACTION;')
# Keep foundation descendants and their action permissions. No old menu is deleted.
[void]$taskSql.AppendLine("INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,status,visible,keep_alive,always_show,creator,updater) VALUES (96000999,'备份菜单','',1,999,0,'/backup','lucide:archive','Layout',0,1,0,1,'doormes-bootstrap','doormes-bootstrap'),(96000000,'门窗业务','',1,1,0,'/factory','lucide:panels-top-left','Layout',0,1,0,1,'doormes-bootstrap','doormes-bootstrap');")
[void]$taskSql.AppendLine("UPDATE system_menu SET parent_id=96000999,path=CASE WHEN path LIKE '/%' THEN TRIM(LEADING '/' FROM path) ELSE path END,updater='doormes-bootstrap' WHERE parent_id=0 AND id NOT IN (1,96000999,96000000);")
[void]$taskSql.AppendLine("UPDATE system_menu SET parent_id=96000999,updater='doormes-bootstrap' WHERE parent_id=1 AND id NOT IN (100,101,102,103,104,105,108,1224,1243,1261,2083);")
[void]$taskSql.AppendLine("UPDATE system_menu SET name='基础管理',sort=90 WHERE id=1; UPDATE system_menu SET name='组织管理' WHERE id=103; UPDATE system_menu SET parent_id=1 WHERE id=106;")
$taskMenus = @(
  @{offset=1;name='工厂工作台';path='workbench';icon='lucide:layout-dashboard';component='doormes/workbench/index';componentName='DoorMesWorkbench';permission='doormes:workspace:query'},
  @{offset=2;name='订单与设计需求';path='orders';icon='lucide:clipboard-list';component='doormes/module/index';componentName='DoorMesOrders';permission='doormes:orders:query'},
  @{offset=3;name='设计研发';path='design';icon='lucide:pen-tool';component='doormes/module/index';componentName='DoorMesDesign';permission='doormes:design:query'},
  @{offset=4;name='产品与组件目录';path='catalog';icon='lucide:panels-top-left';component='doormes/module/index';componentName='DoorMesCatalog';permission='doormes:catalog:query'},
  @{offset=5;name='工艺与采购确认';path='feasibility';icon='lucide:git-pull-request';component='doormes/module/index';componentName='DoorMesFeasibility';permission='doormes:feasibility:query'},
  @{offset=6;name='生产协同';path='production';icon='lucide:factory';component='doormes/module/index';componentName='DoorMesProduction';permission='doormes:production:query'},
  @{offset=7;name='设计与生产变更';path='changes';icon='lucide:git-compare-arrows';component='doormes/module/index';componentName='DoorMesChanges';permission='doormes:changes:query'},
  @{offset=8;name='图纸中心';path='drawings';icon='lucide:files';component='doormes/module/index';componentName='DoorMesDrawings';permission='doormes:drawings:query'}
)
foreach ($taskMenu in $taskMenus) {
  [void]$taskSql.AppendLine("INSERT INTO system_menu (id,name,permission,type,sort,parent_id,path,icon,component,component_name,status,visible,keep_alive,always_show,creator,updater) VALUES ($((96000000+$taskMenu.offset)),'$($taskMenu.name)','$($taskMenu.permission)',2,$($taskMenu.offset),96000000,'$($taskMenu.path)','$($taskMenu.icon)','$($taskMenu.component)','$($taskMenu.componentName)',0,1,0,1,'doormes-bootstrap','doormes-bootstrap');")
}
# New IDs never reuse old user/department/post IDs referenced by historical orders.
[void]$taskSql.AppendLine("DELETE FROM system_oauth2_access_token WHERE user_type=2; DELETE FROM system_oauth2_refresh_token WHERE user_type=2; DELETE FROM system_oauth2_code WHERE user_type=2; DELETE FROM system_oauth2_approve WHERE user_type=2; DELETE FROM system_user_role; DELETE FROM system_user_post; DELETE FROM system_users; DELETE FROM system_dept; DELETE FROM system_post;")
[void]$taskSql.AppendLine("UPDATE system_role SET status=1,deleted=1,updater='doormes-bootstrap' WHERE id<>1; UPDATE system_role_menu SET deleted=1,updater='doormes-bootstrap'; UPDATE system_role SET name='门窗厂系统管理员',status=0,portal_menu_id=96000001,data_scope=1,data_scope_dept_ids='[]',remark='门窗工厂基础管理与全部门窗菜单',updater='doormes-bootstrap' WHERE id=1;")
[void]$taskSql.AppendLine("INSERT INTO system_dept (id,name,parent_id,sort,status,creator,updater,tenant_id) VALUES (1001,$(ConvertTo-TaskSqlText $FactoryName),0,0,0,'doormes-bootstrap','doormes-bootstrap',1);")
for ($taskIndex=0; $taskIndex -lt $taskProfiles.Count; $taskIndex++) {
  $taskProfile=$taskProfiles[$taskIndex]; $taskUserId=1001+$taskIndex; $taskDeptId=1002+$taskIndex; $taskPostId=1001+$taskIndex; $taskRoleId=if ($taskIndex -eq 0) {1} else {1001+$taskIndex}
  [void]$taskSql.AppendLine("INSERT INTO system_dept (id,name,parent_id,sort,leader_user_id,status,creator,updater,tenant_id) VALUES ($taskDeptId,'$($taskProfile.deptName)',1001,$taskIndex,$taskUserId,0,'doormes-bootstrap','doormes-bootstrap',1);")
  [void]$taskSql.AppendLine("INSERT INTO system_post (id,code,name,sort,status,remark,creator,updater,tenant_id) VALUES ($taskPostId,'$($taskProfile.roleCode)','$($taskProfile.roleName)',$taskIndex,0,'门窗业务测试岗位','doormes-bootstrap','doormes-bootstrap',1);")
  if ($taskIndex -gt 0) { [void]$taskSql.AppendLine("INSERT INTO system_role (id,name,code,sort,portal_menu_id,data_scope,data_scope_dept_ids,status,type,remark,creator,updater,tenant_id) VALUES ($taskRoleId,'$($taskProfile.roleName)','$($taskProfile.roleCode)',$taskIndex,96000001,1,'[]',0,2,'门窗业务菜单；数据范围后续按订单/项目细化','doormes-bootstrap','doormes-bootstrap',1);") }
  [void]$taskSql.AppendLine("INSERT INTO system_users (id,username,password,nickname,remark,dept_id,post_ids,status,creator,updater,tenant_id) VALUES ($taskUserId,'$($taskProfile.username)','$($taskHashes[$taskIndex])','$($taskProfile.nickname)','门窗业务人工联测账号；首次使用请修改密码',$taskDeptId,'[$taskPostId]',0,'doormes-bootstrap','doormes-bootstrap',1);")
  [void]$taskSql.AppendLine("INSERT INTO system_user_role (user_id,role_id,creator,updater,tenant_id) VALUES ($taskUserId,$taskRoleId,'doormes-bootstrap','doormes-bootstrap',1); INSERT INTO system_user_post (user_id,post_id,creator,updater,tenant_id) VALUES ($taskUserId,$taskPostId,'doormes-bootstrap','doormes-bootstrap',1);")
  if ($taskIndex -gt 0) {
    foreach ($taskMenuId in @(96000000)+@($taskProfile.menus | ForEach-Object { 96000000+$_ })) { [void]$taskSql.AppendLine("INSERT INTO system_role_menu (role_id,menu_id,creator,updater,tenant_id) VALUES ($taskRoleId,$taskMenuId,'doormes-bootstrap','doormes-bootstrap',1);") }
  }
}
[void]$taskSql.AppendLine("UPDATE system_dept SET leader_user_id=1001 WHERE id=1001; SET SESSION group_concat_max_len=1000000; INSERT INTO system_tenant_package (id,name,status,remark,menu_ids,creator,updater) SELECT 2000,'门窗工厂开发套餐',0,'基础管理、门窗业务及管理员备份菜单',CONCAT('[',GROUP_CONCAT(id ORDER BY id),']'),'doormes-bootstrap','doormes-bootstrap' FROM system_menu WHERE deleted=0;")
# TenantDO.websites uses the inherited comma-separated StringListTypeHandler, not JSON.
[void]$taskSql.AppendLine("UPDATE system_tenant SET contact_user_id=NULL,status=1,updater='doormes-bootstrap' WHERE id<>1; UPDATE system_tenant SET name=$(ConvertTo-TaskSqlText $FactoryName),contact_user_id=1001,contact_name='系统管理员',contact_mobile='',status=0,package_id=2000,websites='localhost,127.0.0.1,192.168.50.193',expire_time='2099-12-31 23:59:59',account_count=200,updater='doormes-bootstrap' WHERE id=1;")
[void]$taskSql.AppendLine('COMMIT;')
Invoke-DoormesAdminQuery -Sql $taskSql.ToString() | Out-Null
$taskResult = Invoke-DoormesAdminQuery -Sql "SELECT COUNT(*) FROM doormes_local.system_users; SELECT COUNT(*) FROM doormes_local.system_dept; SELECT COUNT(*) FROM doormes_local.system_user_role; SELECT COUNT(*) FROM doormes_local.system_users WHERE id<1001; SELECT COUNT(*) FROM doormes_local.system_menu WHERE parent_id=0 AND deleted=0; SELECT COUNT(*) FROM doormes_local.system_role WHERE deleted=0 AND status=0; SELECT COUNT(*) FROM doormes_local.system_menu WHERE deleted=0;"
$taskVerified = @($taskResult -split '\r?\n' | ForEach-Object { [int]$_ })
if ($taskVerified.Count -ne 7 -or $taskVerified[0] -ne 7 -or $taskVerified[1] -ne 8 -or $taskVerified[2] -ne 7 -or $taskVerified[3] -ne 0 -or $taskVerified[4] -ne 3 -or $taskVerified[5] -ne 7 -or $taskVerified[6] -ne 1360) { throw 'Initialization committed but verification failed; inspect the protected backup before further changes.' }
$taskManifest = @{schemaVersion='doormes-factory-bootstrap.v1';database='doormes_local';factory=$FactoryName;tenantId=1;createdAt=(Get-Date).ToString('o');backup=$taskBackupSql;backupSha256=(Get-FileHash -LiteralPath $taskBackupSql -Algorithm SHA256).Hash;credentialsFile=$taskCredentialsPath;accounts=7;departments=8;menuRoots=3;activeRoles=7;historicalUserIdsNotReused=$true;originalDatabaseUnchanged=$true}
[IO.File]::WriteAllText($taskMarker,($taskManifest | ConvertTo-Json -Depth 4),[Text.UTF8Encoding]::new($false))
[pscustomobject]@{Database='doormes_local';Accounts=7;Departments=8;ActiveRoles=7;MenuRoots=3;Backup=$taskBackupSql;CredentialsFile=$taskCredentialsPath;Status='Initialized; restart isolated backend/cache before login verification'} | ConvertTo-Json
