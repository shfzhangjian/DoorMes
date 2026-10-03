[CmdletBinding()]
param([switch]$ResumeData, [string]$BackupDirectory = '', [switch]$RefreshData)
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskSourceDb = $env:DOORMES_SOURCE_DATABASE
if (!$taskSourceDb -or $taskSourceDb -notmatch '^[a-zA-Z0-9_]+$' -or $taskSourceDb -eq 'doormes_local') { throw 'Set DOORMES_SOURCE_DATABASE to an explicitly approved local source database.' }
$taskTargetDb = 'doormes_local'
$taskMysql = 'C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe'
$taskDump = 'C:\Program Files\MySQL\MySQL Server 5.7\bin\mysqldump.exe'
$taskUser = $env:DOORMES_ADMIN_DB_USERNAME
$taskPassword = $env:DOORMES_ADMIN_DB_PASSWORD
if (!$taskUser -or !$taskPassword) { throw 'Set DOORMES_ADMIN_DB_USERNAME and DOORMES_ADMIN_DB_PASSWORD; no database was changed.' }

function Invoke-TaskMysql {
  param([string]$Sql, [string]$Database = '')
  $taskInfo = [Diagnostics.ProcessStartInfo]::new($taskMysql)
  $taskInfo.UseShellExecute = $false
  $taskInfo.CreateNoWindow = $true
  $taskInfo.RedirectStandardInput = $true
  $taskInfo.RedirectStandardOutput = $true
  $taskInfo.RedirectStandardError = $true
  $taskInfo.Environment['MYSQL_PWD'] = $taskPassword
  foreach ($taskArg in @('--host=127.0.0.1','--port=3306',"--user=$taskUser",'--default-character-set=utf8mb4','--batch','--skip-column-names')) { $taskInfo.ArgumentList.Add($taskArg) }
  if ($Database) { $taskInfo.ArgumentList.Add($Database) }
  $taskProcess = [Diagnostics.Process]::Start($taskInfo)
  $taskErrors = $taskProcess.StandardError.ReadToEndAsync()
  $taskOutput = $taskProcess.StandardOutput.ReadToEndAsync()
  $taskProcess.StandardInput.Write($Sql)
  $taskProcess.StandardInput.Close()
  $taskProcess.WaitForExit()
  if ($taskProcess.ExitCode -ne 0) { throw "MySQL operation failed ($($taskProcess.ExitCode)); details not printed to avoid exposing data." }
  return $taskOutput.GetAwaiter().GetResult().Trim()
}

function Export-TaskDump {
  param([string]$Path, [string[]]$ExtraArgs)
  $taskInfo = [Diagnostics.ProcessStartInfo]::new($taskDump)
  $taskInfo.UseShellExecute = $false
  $taskInfo.CreateNoWindow = $true
  $taskInfo.RedirectStandardOutput = $true
  $taskInfo.RedirectStandardError = $true
  $taskInfo.Environment['MYSQL_PWD'] = $taskPassword
  foreach ($taskArg in @('--host=127.0.0.1','--port=3306',"--user=$taskUser",'--default-character-set=utf8mb4','--single-transaction','--quick','--skip-lock-tables','--hex-blob','--skip-events') + $ExtraArgs + @($taskSourceDb)) { $taskInfo.ArgumentList.Add($taskArg) }
  $taskProcess = [Diagnostics.Process]::Start($taskInfo)
  $taskErrors = $taskProcess.StandardError.ReadToEndAsync()
  $taskFile = [IO.File]::Create($Path)
  try { $taskProcess.StandardOutput.BaseStream.CopyTo($taskFile) } finally { $taskFile.Dispose() }
  $taskProcess.WaitForExit()
  if ($taskProcess.ExitCode -ne 0) { throw "Dump failed ($($taskProcess.ExitCode)); target database has not been initialized." }
}

function Import-TaskDump {
  param([string]$Path)
  if ($taskTargetDb -ne 'doormes_local' -or $taskTargetDb -eq $taskSourceDb) { throw 'Unsafe import target.' }
  $taskInfo = [Diagnostics.ProcessStartInfo]::new($taskMysql)
  $taskInfo.UseShellExecute = $false
  $taskInfo.CreateNoWindow = $true
  $taskInfo.RedirectStandardInput = $true
  $taskInfo.RedirectStandardOutput = $true
  $taskInfo.RedirectStandardError = $true
  $taskInfo.Environment['MYSQL_PWD'] = $taskPassword
  foreach ($taskArg in @('--host=127.0.0.1','--port=3306',"--user=$taskUser",'--default-character-set=utf8mb4','--max-allowed-packet=256M','--binary-mode',$taskTargetDb)) { $taskInfo.ArgumentList.Add($taskArg) }
  $taskProcess = [Diagnostics.Process]::Start($taskInfo)
  $taskErrors = $taskProcess.StandardError.ReadToEndAsync()
  $taskOutput = $taskProcess.StandardOutput.ReadToEndAsync()
  $taskFile = [IO.File]::OpenRead($Path)
  $taskCopyError = $null
  try { $taskFile.CopyTo($taskProcess.StandardInput.BaseStream) } catch { $taskCopyError = $_ } finally { $taskFile.Dispose(); $taskProcess.StandardInput.Close() }
  $taskProcess.WaitForExit()
  $taskErrorText = $taskErrors.GetAwaiter().GetResult()
  if ($taskProcess.ExitCode -ne 0 -or $taskCopyError) {
    [IO.File]::WriteAllText(($Path + '.import-error.log'), $taskErrorText, [Text.UTF8Encoding]::new($false))
    $taskErrorCode = [regex]::Match($taskErrorText, 'ERROR\s+[0-9]+\s+\([a-zA-Z0-9]+\)(?:\s+at\s+line\s+[0-9]+)?').Value
    throw "Import failed ($taskErrorCode); partial NEW database retained for diagnosis; original unchanged. Protected details: $Path.import-error.log"
  }
}

$taskExists = Invoke-TaskMysql "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$taskTargetDb';"
if ($taskExists -ne '0' -and !$ResumeData) { throw 'doormes_local already exists; refusing to overwrite it.' }
if ($ResumeData -and $taskExists -ne '1') { throw 'Cannot resume an absent clone.' }
$taskSchema = Invoke-TaskMysql "SELECT DEFAULT_CHARACTER_SET_NAME,DEFAULT_COLLATION_NAME FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$taskSourceDb';"
$taskSettings = $taskSchema -split "`t"
if ($taskSettings.Count -ne 2 -or $taskSettings[0] -notmatch '^[a-zA-Z0-9_]+$' -or $taskSettings[1] -notmatch '^[a-zA-Z0-9_]+$') { throw 'Unexpected source schema.' }
if ((Invoke-TaskMysql "SELECT COUNT(*) FROM information_schema.EVENTS WHERE EVENT_SCHEMA='$taskSourceDb';") -ne '0') { throw 'Source has scheduled SQL events. Explicit disabled-event migration is required before clone.' }
$taskRun = if ($ResumeData) { [IO.Path]::GetFullPath($BackupDirectory) } else { Join-Path $taskRoot ('runtime-local/db-clone/' + (Get-Date -Format 'yyyyMMdd-HHmmss')) }
if (!$taskRun.StartsWith((Join-Path $taskRoot 'runtime-local/db-clone') + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Backup must be inside this copy runtime-local/db-clone.' }
if ($ResumeData) {
  foreach ($taskName in @('source-data.sql','target-schema.sql','source-tables.txt','source-row-counts.tsv')) { if (!(Test-Path (Join-Path $taskRun $taskName))) { throw "Missing recoverable export: $taskName" } }
  $taskSourceTables = [IO.File]::ReadAllText((Join-Path $taskRun 'source-tables.txt'))
  $taskTargetTables = Invoke-TaskMysql "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='$taskTargetDb' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME;"
  if ($taskSourceTables.Trim() -ne $taskTargetTables.Trim()) { throw 'Target structure changed after cloning; refusing to reset data.' }
  $taskBefore = [IO.File]::ReadAllText((Join-Path $taskRun 'source-row-counts.tsv'))
  $taskCountsSql = (($taskSourceTables -split '\r?\n') | ForEach-Object {
    if ($_ -notmatch '^[a-zA-Z0-9_]+$') { throw 'Unexpected identifier.' }
    "SELECT '$_',COUNT(*) FROM ``$taskTargetDb``.``$_``"
  }) -join ' UNION ALL '
  $taskRecoveryData = Join-Path $taskRun 'source-data.sql'
  if ($RefreshData) {
    $taskRecoveryData = Join-Path $taskRun 'source-data-mysql57.sql'
    if (Test-Path $taskRecoveryData) { throw 'Refusing to replace an existing refreshed export.' }
    Write-Host 'Re-exporting with matching MySQL 5.7 client (generated columns are excluded from INSERT values).'
    $taskBefore = Invoke-TaskMysql ($taskCountsSql.Replace(('`' + $taskTargetDb + '`.'), ('`' + $taskSourceDb + '`.')) + ';')
    [IO.File]::WriteAllText((Join-Path $taskRun 'source-row-counts-mysql57.tsv'), $taskBefore, [Text.UTF8Encoding]::new($false))
    Export-TaskDump $taskRecoveryData @('--no-create-info','--skip-triggers','--skip-add-locks')
  } elseif (Test-Path (Join-Path $taskRun 'source-data-mysql57.sql')) {
    $taskRecoveryData = Join-Path $taskRun 'source-data-mysql57.sql'
    $taskBefore = [IO.File]::ReadAllText((Join-Path $taskRun 'source-row-counts-mysql57.tsv'))
  }
  [IO.File]::WriteAllText((Join-Path $taskRun 'partial-target-row-counts.tsv'), (Invoke-TaskMysql ($taskCountsSql + ';')), [Text.UTF8Encoding]::new($false))
  # Explicit recovery of this newly created, not-yet-served clone only. No schema drop and no source writes.
  $taskResetSql = 'SET FOREIGN_KEY_CHECKS=0; ' + ((($taskSourceTables -split '\r?\n') | ForEach-Object { "TRUNCATE TABLE ``$taskTargetDb``.``$_``;" }) -join ' ') + ' SET FOREIGN_KEY_CHECKS=1;'
  Write-Host 'Recovering partial clone data ONLY in doormes_local from the completed export.'
  $null = Invoke-TaskMysql $taskResetSql
  Import-TaskDump $taskRecoveryData
  $taskAfter = Invoke-TaskMysql ($taskCountsSql + ';')
  [IO.File]::WriteAllText((Join-Path $taskRun 'target-row-counts.tsv'), $taskAfter, [Text.UTF8Encoding]::new($false))
  $taskMismatch = @(Compare-Object ($taskBefore -split '\r?\n') ($taskAfter -split '\r?\n'))
  if ($taskMismatch.Count) { throw "Row counts differ for $($taskMismatch.Count) entries; investigate before startup." }
  [IO.File]::WriteAllText((Join-Path $taskRun 'clone-complete.json'), (@{ sourceDatabase=$taskSourceDb; targetDatabase=$taskTargetDb; baseTables=@($taskSourceTables -split '\r?\n').Count; exactRowCounts=$true; completedAt=(Get-Date).ToString('o') } | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
  Write-Host "Recovered clone complete: exact row counts match for $(@($taskSourceTables -split '\r?\n').Count) base tables. Exports: $taskRun"
  return
}
New-Item -ItemType Directory -Path $taskRun -Force | Out-Null
$taskSourceTables = Invoke-TaskMysql "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='$taskSourceDb' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME;"
[IO.File]::WriteAllText((Join-Path $taskRun 'source-tables.txt'), $taskSourceTables, [Text.UTF8Encoding]::new($false))
$taskCountsSql = (($taskSourceTables -split '\r?\n') | ForEach-Object {
  if ($_ -notmatch '^[a-zA-Z0-9_]+$') { throw 'Unexpected table identifier.' }
  "SELECT '$_',COUNT(*) FROM ``$taskSourceDb``.``$_``"
}) -join ' UNION ALL '
Write-Host 'Checking all source table row counts (read-only).'
$taskBefore = Invoke-TaskMysql ($taskCountsSql + ';')
[IO.File]::WriteAllText((Join-Path $taskRun 'source-row-counts.tsv'), $taskBefore, [Text.UTF8Encoding]::new($false))
Write-Host 'Exporting schema, views and routines (source remains read-only).'
$taskSchemaPath = Join-Path $taskRun 'source-schema.sql'
Export-TaskDump $taskSchemaPath @('--no-data','--routines','--triggers')
Write-Host 'Exporting all data in an InnoDB consistent snapshot.'
$taskDataPath = Join-Path $taskRun 'source-data.sql'
Export-TaskDump $taskDataPath @('--no-create-info','--skip-triggers','--skip-add-locks')
$taskSchemaText = [IO.File]::ReadAllText($taskSchemaPath)
# Only DDL is rebound; row data is never text-rewritten.
$taskTargetSchema = $taskSchemaText.Replace(('`' + $taskSourceDb + '`.'), ('`' + $taskTargetDb + '`.')).Replace(('USE `' + $taskSourceDb + '`;'), ('USE `' + $taskTargetDb + '`;'))
if ($taskTargetSchema -match ('(?i)\bUSE\s+`?' + $taskSourceDb + '`?\s*;') -or $taskTargetSchema.Contains(('`' + $taskSourceDb + '`.'))) { throw 'Source schema reference remained in DDL.' }
$taskReboundPath = Join-Path $taskRun 'target-schema.sql'
[IO.File]::WriteAllText($taskReboundPath, $taskTargetSchema, [Text.UTF8Encoding]::new($false))
Write-Host 'Full recoverable exports ready. Creating ONLY doormes_local.'
$null = Invoke-TaskMysql "CREATE DATABASE ``$taskTargetDb`` CHARACTER SET $($taskSettings[0]) COLLATE $($taskSettings[1]);"
Import-TaskDump $taskReboundPath
Write-Host 'Schema restored; importing data into new database.'
Import-TaskDump $taskDataPath
$taskAfter = Invoke-TaskMysql ($taskCountsSql.Replace(('`' + $taskSourceDb + '`.'), ('`' + $taskTargetDb + '`.')) + ';')
[IO.File]::WriteAllText((Join-Path $taskRun 'target-row-counts.tsv'), $taskAfter, [Text.UTF8Encoding]::new($false))
$taskMismatch = @(Compare-Object ($taskBefore -split '\r?\n') ($taskAfter -split '\r?\n'))
if ($taskMismatch.Count -gt 0) { throw "Clone imported, but row counts differ for $($taskMismatch.Count) entries; investigate before service startup (source may have changed during export)." }
[IO.File]::WriteAllText((Join-Path $taskRun 'clone-complete.json'), (@{ sourceDatabase=$taskSourceDb; targetDatabase=$taskTargetDb; baseTables=@($taskSourceTables -split '\r?\n').Count; exactRowCounts=$true; completedAt=(Get-Date).ToString('o') } | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
$taskObjects = Invoke-TaskMysql "SELECT TABLE_TYPE,COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$taskTargetDb' GROUP BY TABLE_TYPE; SELECT 'ROUTINES',COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='$taskTargetDb';"
Write-Host $taskObjects
Write-Host "Clone complete: exact row counts match for $(@($taskSourceTables -split '\r?\n').Count) base tables. Recoverable exports: $taskRun"
