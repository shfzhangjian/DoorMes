[CmdletBinding()]
param([Parameter(Mandatory)][string]$BackupDirectory)
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot = Get-DoormesWorkspaceRoot
$taskRun = [IO.Path]::GetFullPath($BackupDirectory)
if (!$taskRun.StartsWith((Join-Path $taskRoot 'runtime-local/db-clone') + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Unexpected backup directory.' }
$taskTables = [IO.File]::ReadAllText((Join-Path $taskRun 'source-tables.txt')).Trim() -split '\r?\n'
$taskCountsSql = ($taskTables | ForEach-Object {
  if ($_ -notmatch '^[a-zA-Z0-9_]+$') { throw 'Unexpected identifier.' }
  "SELECT '$_',COUNT(*) FROM ``doormes_local``.``$_``"
}) -join ' UNION ALL '
$taskExpectedPath = if (Test-Path (Join-Path $taskRun 'source-row-counts-mysql57.tsv')) { Join-Path $taskRun 'source-row-counts-mysql57.tsv' } else { Join-Path $taskRun 'source-row-counts.tsv' }
$taskExpected = [IO.File]::ReadAllText($taskExpectedPath).Trim()
$taskActual = Invoke-DoormesAdminQuery ($taskCountsSql + ';')
if (@(Compare-Object ($taskExpected -split '\r?\n') ($taskActual -split '\r?\n')).Count) { throw 'Cloned row counts differ; do not initialize or clean.' }
$taskMetadata = Invoke-DoormesAdminQuery "SELECT TABLE_TYPE,COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='doormes_local' GROUP BY TABLE_TYPE; SELECT 'ROUTINES',COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='doormes_local'; SELECT 'ORIGINAL_VIEW_REFS',COUNT(*) FROM information_schema.VIEWS WHERE TABLE_SCHEMA='doormes_local' AND VIEW_DEFINITION LIKE '%qdaq0923%'; SELECT 'ORIGINAL_ROUTINE_REFS',COUNT(*) FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='doormes_local' AND ROUTINE_DEFINITION LIKE '%qdaq0923%';"
if ($taskMetadata -notmatch 'ORIGINAL_VIEW_REFS\s+0' -or $taskMetadata -notmatch 'ORIGINAL_ROUTINE_REFS\s+0') { throw 'Cross-schema original references remain.' }
$taskManifest = @{ sourceDatabase='qdaq0923'; targetDatabase='doormes_local'; baseTables=$taskTables.Count; exactRowCounts=$true; metadata=$taskMetadata; completedAt=(Get-Date).ToString('o') }
[IO.File]::WriteAllText((Join-Path $taskRun 'clone-complete.json'), ($taskManifest | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
Write-Host "Verified exact row counts for $($taskTables.Count) base tables."
Write-Host $taskMetadata
