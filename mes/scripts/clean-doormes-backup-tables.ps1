[CmdletBinding()]
param([switch]$Apply)
$ErrorActionPreference = 'Stop'
Import-Module (Join-Path $PSScriptRoot 'doormes-db-tools.psm1') -Force
$taskRoot = Get-DoormesWorkspaceRoot
$taskRun = Join-Path $taskRoot 'runtime-local/backup-table-cleanup'
New-Item -ItemType Directory -Path $taskRun -Force | Out-Null
# Only unequivocal bak_ prefix tables; tmp/old/history and all normal business tables are retained.
$taskNamesText = Invoke-DoormesAdminQuery "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='doormes_local' AND TABLE_TYPE='BASE TABLE' AND LEFT(TABLE_NAME,4)='bak_' ORDER BY TABLE_NAME;"
$taskNames = @($taskNamesText -split '\r?\n' | Where-Object { $_ })
if (!$taskNames.Count) { Write-Host 'No bak_ tables in new database.'; return }
foreach ($taskName in $taskNames) { if ($taskName -notmatch '^bak_[a-zA-Z0-9_]+$') { throw 'Unsafe backup table identifier.' } }
$taskPattern = Join-Path $taskRun 'backup-candidates.txt'
[IO.File]::WriteAllLines($taskPattern, $taskNames, [Text.UTF8Encoding]::new($false))
# Retain any backup table that active Java/MyBatis code, a view/routine, or a foreign key still references.
$taskCodeMatches = & rg --no-heading --no-line-number -F -f $taskPattern (Join-Path $taskRoot 'backend') -g '*.java' -g '*.xml' -g '!**/target/**'
$taskDefinitions = Invoke-DoormesAdminQuery "SELECT VIEW_DEFINITION FROM information_schema.VIEWS WHERE TABLE_SCHEMA='doormes_local'; SELECT ROUTINE_DEFINITION FROM information_schema.ROUTINES WHERE ROUTINE_SCHEMA='doormes_local';"
$taskReferences = Invoke-DoormesAdminQuery "SELECT TABLE_NAME,REFERENCED_TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='doormes_local' AND REFERENCED_TABLE_NAME IS NOT NULL;"
$taskKeep = @()
$taskDrop = @()
foreach ($taskName in $taskNames) {
  $taskExact = '(?<![a-zA-Z0-9_])' + [regex]::Escape($taskName) + '(?![a-zA-Z0-9_])'
  if (($taskCodeMatches -join "`n") -match $taskExact -or $taskDefinitions -match $taskExact -or $taskReferences -match $taskExact) { $taskKeep += $taskName } else { $taskDrop += $taskName }
}
$taskManifest = [ordered]@{ targetDatabase='doormes_local'; createdAt=(Get-Date).ToString('o'); remove=$taskDrop; retainReferenced=$taskKeep; originalDatabaseChanged=$false; applied=$false }
[IO.File]::WriteAllText((Join-Path $taskRun 'cleanup-manifest.json'), ($taskManifest | ConvertTo-Json -Depth 4), [Text.UTF8Encoding]::new($false))
Write-Host "New database backup table candidates: $($taskNames.Count); safe to remove: $($taskDrop.Count); retained references: $($taskKeep.Count). Manifest: $taskRun"
if (!$Apply -or !$taskDrop.Count) { return }
$taskCloneRuns = @(Get-ChildItem (Join-Path $taskRoot 'runtime-local/db-clone') -Directory)
$taskVerified = @($taskCloneRuns | Where-Object { Test-Path (Join-Path $_.FullName 'clone-complete.json') })
if (!$taskVerified.Count) { throw 'No verified complete clone export; refusing backup table deletion.' }
$taskSql = 'USE `doormes_local`; ' + (($taskDrop | ForEach-Object { "DROP TABLE ``doormes_local``.``$_``;" }) -join ' ')
$null = Invoke-DoormesAdminQuery $taskSql
$taskManifest.applied = $true
$taskManifest.recoverableExport = $taskVerified[-1].FullName
[IO.File]::WriteAllText((Join-Path $taskRun 'cleanup-manifest.json'), ($taskManifest | ConvertTo-Json -Depth 4), [Text.UTF8Encoding]::new($false))
Write-Host "Removed $($taskDrop.Count) backup tables ONLY from doormes_local. Full pre-cleanup exports retained; original database unchanged."
