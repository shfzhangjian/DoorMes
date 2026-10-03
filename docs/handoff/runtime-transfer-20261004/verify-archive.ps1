[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$taskArchive=Join-Path $PSScriptRoot '01-design-data-and-bootstrap.zip'
$taskExpected='576e766d8935536e2f050bfad854f049c225ba207d9ed4c9125b121fee402fcd'
if (!(Test-Path -LiteralPath $taskArchive -PathType Leaf)) { throw 'Runtime archive missing.' }
if ((Get-FileHash -LiteralPath $taskArchive -Algorithm SHA256).Hash -ne $taskExpected) { throw 'Runtime archive SHA-256 mismatch. Do not restore.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$taskZip=[IO.Compression.ZipFile]::OpenRead($taskArchive)
try {
    $taskFiles=@($taskZip.Entries | Where-Object { !$_.FullName.EndsWith('/') })
    if ($taskFiles.Count -ne 30) { throw 'Unexpected archive file count.' }
    foreach ($taskEntry in $taskFiles) {
        if ($taskEntry.FullName.Contains('..') -or $taskEntry.FullName -notmatch '^runtime-local/(design-data/|factory-bootstrap/complete\.json$)') { throw 'Unexpected archive path.' }
    }
} finally { $taskZip.Dispose() }
'PASS: archive SHA-256 and 30 file paths verified. Database correspondence is not verified.'
