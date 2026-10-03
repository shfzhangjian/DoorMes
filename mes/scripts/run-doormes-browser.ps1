$ToolArguments=$args
$ErrorActionPreference='Stop'
$OutputEncoding=[System.Text.UTF8Encoding]::new($false)
$taskRoot=Split-Path $PSScriptRoot -Parent
$toolRoot=Join-Path $taskRoot 'runtime-local\browser-tools-compatible\node_modules'
$browserCli=Join-Path $toolRoot '.bin\agent-browser.cmd'
$encoderDirectory=Join-Path $taskRoot 'runtime-local\browser-tools\node_modules\ffmpeg-static'
if (!(Test-Path -LiteralPath $browserCli)) { throw 'Install the approved local agent-browser tool first.' }
if (Test-Path -LiteralPath (Join-Path $encoderDirectory 'ffmpeg.exe')) { $env:PATH="$encoderDirectory;$env:PATH" }
# Named, non-restoring session; no credentials or user browser profile in arguments.
$downloadDirectory=(Join-Path $taskRoot 'runtime-local\scenarios\S1-FACTORY-DRAWING-20261003').Replace('\','/')
$env:AGENT_BROWSER_STREAM_PORT='9225'
if ($MyInvocation.ExpectingInput) { $input | & $browserCli --session doormes-s1-record-1 --cdp http://127.0.0.1:9223 --download-path $downloadDirectory @ToolArguments }
else { & $browserCli --session doormes-s1-record-1 --cdp http://127.0.0.1:9223 --download-path $downloadDirectory @ToolArguments }
exit $LASTEXITCODE
