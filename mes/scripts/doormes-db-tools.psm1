$ErrorActionPreference = 'Stop'
$script:TaskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$script:TaskUser = $env:DOORMES_ADMIN_DB_USERNAME
$script:TaskPassword = $env:DOORMES_ADMIN_DB_PASSWORD
function Invoke-DoormesAdminQuery {
  param([Parameter(Mandatory)][string]$Sql)
  if (!$script:TaskUser -or !$script:TaskPassword) { throw 'Set DOORMES_ADMIN_DB_USERNAME and DOORMES_ADMIN_DB_PASSWORD for the local database before running administrative scripts.' }
  $taskInfo = [Diagnostics.ProcessStartInfo]::new('C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe')
  $taskInfo.UseShellExecute = $false
  $taskInfo.CreateNoWindow = $true
  $taskInfo.RedirectStandardInput = $true
  $taskInfo.RedirectStandardOutput = $true
  $taskInfo.RedirectStandardError = $true
  # Windows defaults stdin to GB2312; mysql explicitly expects UTF-8.
  $taskInfo.StandardInputEncoding = [Text.UTF8Encoding]::new($false)
  $taskInfo.StandardOutputEncoding = [Text.UTF8Encoding]::new($false)
  $taskInfo.StandardErrorEncoding = [Text.UTF8Encoding]::new($false)
  $taskInfo.Environment['MYSQL_PWD'] = $script:TaskPassword
  foreach ($taskArg in @('--host=127.0.0.1','--port=3306',"--user=$($script:TaskUser)",'--default-character-set=utf8mb4','--batch','--skip-column-names')) { $taskInfo.ArgumentList.Add($taskArg) }
  $taskProcess = [Diagnostics.Process]::Start($taskInfo)
  $taskErrors = $taskProcess.StandardError.ReadToEndAsync()
  $taskOutput = $taskProcess.StandardOutput.ReadToEndAsync()
  $taskInputFailed = $false
  try { $taskProcess.StandardInput.Write($Sql) } catch { $taskInputFailed = $true }
  try { $taskProcess.StandardInput.Close() } catch { $taskInputFailed = $true }
  $taskProcess.WaitForExit()
  if ($taskProcess.ExitCode -ne 0 -or $taskInputFailed) {
    $taskCode = [regex]::Match($taskErrors.GetAwaiter().GetResult(), 'ERROR\s+[0-9]+\s+\([a-zA-Z0-9]+\)(?: at line [0-9]+)?').Value
    throw "MySQL failed: $taskCode (sensitive details are not printed)."
  }
  return $taskOutput.GetAwaiter().GetResult().Trim()
}
function Get-DoormesWorkspaceRoot { return $script:TaskRoot }
Export-ModuleMember -Function Invoke-DoormesAdminQuery,Get-DoormesWorkspaceRoot
