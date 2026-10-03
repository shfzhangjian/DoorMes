param([Parameter(Mandatory = $true)][string]$OutputPath)
$ErrorActionPreference = 'Stop'
$browser = Join-Path $PSScriptRoot 'run-doormes-browser.ps1'
$root = [IO.Path]::GetFullPath((Join-Path (Split-Path $PSScriptRoot -Parent) 'runtime-local\scenarios'))
$target = [IO.Path]::GetFullPath($OutputPath)
if (!$target.StartsWith($root + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
  throw 'Recordings must stay inside this project scenario evidence directory.'
}
if (Test-Path -LiteralPath $target) { throw 'Use a new filename; existing evidence must not be overwritten.' }
# Preserve only this isolated test session, in process memory. Never print or save tokens.
$stateReply = & $browser eval --json 'JSON.stringify({local:Object.fromEntries(Object.entries(localStorage)),session:Object.fromEntries(Object.entries(sessionStorage))})'
if ($LASTEXITCODE -ne 0) { throw 'Could not read the isolated test session.' }
$state = ($stateReply | ConvertFrom-Json).data.result
if (!$state) { throw 'Test session state is unavailable.' }
& $browser record start $target.Replace('\','/')
if ($LASTEXITCODE -ne 0) { throw 'Recording did not start.' }
$restore = 'const state=' + $state + ';for(const [k,v] of Object.entries(state.local))localStorage.setItem(k,v);for(const [k,v] of Object.entries(state.session))sessionStorage.setItem(k,v);"isolated session restored"'
$restore | & $browser eval --stdin
$state = $null; $stateReply = $null; $restore = $null
if ($LASTEXITCODE -ne 0) { throw 'Session restore failed; stop the recording before retrying.' }
& $browser open 'http://localhost:5180/factory/workbench'
exit $LASTEXITCODE
