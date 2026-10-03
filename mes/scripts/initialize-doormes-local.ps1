[CmdletBinding()]
param([string]$PublicFileOrigin = 'http://192.168.50.193:5180')
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot = Get-DoormesWorkspaceRoot
$taskAuthPath = Join-Path $taskRoot 'runtime-local/db-credentials.json'
if (Test-Path $taskAuthPath) { throw 'Local runtime credentials already exist; do not overwrite the account or configuration.' }
if ((Invoke-DoormesAdminQuery "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='doormes_local';") -ne '1') { throw 'New database has not been created.' }
$taskVerified = @(Get-ChildItem (Join-Path $taskRoot 'runtime-local/db-clone') -Directory | Where-Object { Test-Path (Join-Path $_.FullName 'clone-complete.json') })
if (!$taskVerified.Count) { throw 'A verified clone is required before initialization.' }
if ((Invoke-DoormesAdminQuery "SELECT COUNT(*) FROM mysql.user WHERE User='doormes_app';") -ne '0') { throw 'The dedicated account already exists; refusing to alter an existing account.' }
$taskBytes = [byte[]]::new(32)
[Security.Cryptography.RandomNumberGenerator]::Fill($taskBytes)
$taskNewPassword = [Convert]::ToBase64String($taskBytes)
$taskUploadRoot = Join-Path $taskRoot 'runtime-local/uploads'
New-Item -ItemType Directory -Path $taskUploadRoot -Force | Out-Null
$taskUri = [Uri]$PublicFileOrigin
if (!$taskUri.IsAbsoluteUri -or $taskUri.Scheme -ne 'http' -or $taskUri.AbsolutePath -ne '/' -or $taskUri.Query -or $taskUri.Fragment -or $taskUri.UserInfo) { throw 'Public file origin must be a local development HTTP origin without credentials or path.' }
$taskFileConfig = @{ basePath=$taskUploadRoot.Replace('\','/'); domain=$PublicFileOrigin.TrimEnd('/') } | ConvertTo-Json -Compress
$taskFileConfigSql = $taskFileConfig.Replace("'", "''")
# New scoped account cannot query/update the original schema. Secrets stay in ignored runtime storage.
$taskSql = @"
CREATE USER 'doormes_app'@'localhost' IDENTIFIED BY '$taskNewPassword';
CREATE USER 'doormes_app'@'127.0.0.1' IDENTIFIED BY '$taskNewPassword';
GRANT ALL PRIVILEGES ON ``doormes_local``.* TO 'doormes_app'@'localhost';
GRANT ALL PRIVILEGES ON ``doormes_local``.* TO 'doormes_app'@'127.0.0.1';
USE ``doormes_local``;
START TRANSACTION;
UPDATE system_sms_channel SET status=1 WHERE status=0;
UPDATE system_sms_template SET status=1 WHERE status=0;
UPDATE system_mail_template SET status=1 WHERE status=0;
UPDATE infra_file_config SET master=b'0' WHERE master=b'1';
INSERT INTO infra_file_config (name,storage,remark,master,config,creator,updater,deleted)
VALUES ('DoorMes isolated local files',10,'Local DoorMes workspace only; inherited stores blocked by doormes profile',b'1','$taskFileConfigSql','doormes-local','doormes-local',b'0');
COMMIT;
"@
$null = Invoke-DoormesAdminQuery $taskSql
$taskAuth = @{ username='doormes_app'; password=$taskNewPassword; database='doormes_local'; host='127.0.0.1'; port=3306 }
[IO.File]::WriteAllText($taskAuthPath, ($taskAuth | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
Write-Host 'Dedicated account created with permissions ONLY on doormes_local. Original account unchanged.'
Write-Host "New uploads directory: $taskUploadRoot; original SMS/mail templates disabled only in new database."
