[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskRedisRoot = Join-Path $taskRoot 'runtime-local/redis'
$taskSecretPath = Join-Path $taskRedisRoot 'redis-credentials.json'
if (Test-Path $taskSecretPath) { throw 'Dedicated Redis credentials already exist; refusing to replace them.' }
$taskRandom = [byte[]]::new(32)
[Security.Cryptography.RandomNumberGenerator]::Fill($taskRandom)
$taskPassword = [Convert]::ToBase64String($taskRandom)
$taskClient = [Net.Sockets.TcpClient]::new()
try {
  $taskClient.Connect('127.0.0.1',6381)
  $taskStream = $taskClient.GetStream()
  $taskStream.ReadTimeout = 5000
  function Invoke-TaskRedisCommand([string[]]$Arguments) {
    $taskResp = '*' + $Arguments.Count + "`r`n"
    foreach ($taskArg in $Arguments) { $taskResp += '$' + [Text.Encoding]::UTF8.GetByteCount($taskArg) + "`r`n" + $taskArg + "`r`n" }
    $taskBytes = [Text.Encoding]::UTF8.GetBytes($taskResp)
    $taskStream.Write($taskBytes,0,$taskBytes.Length)
    $taskReply = [byte[]]::new(4096)
    $taskLength = $taskStream.Read($taskReply,0,$taskReply.Length)
    return [Text.Encoding]::UTF8.GetString($taskReply,0,$taskLength).Trim()
  }
  if ((Invoke-TaskRedisCommand @('PING')) -ne '+PONG') { throw 'Dedicated Redis is not accessible; original Redis is not touched.' }
  if ((Invoke-TaskRedisCommand @('CONFIG','SET','requirepass',$taskPassword)) -ne '+OK') { throw 'Cannot configure dedicated Redis authentication.' }
  if ((Invoke-TaskRedisCommand @('AUTH',$taskPassword)) -ne '+OK' -or (Invoke-TaskRedisCommand @('PING')) -ne '+PONG') { throw 'Dedicated Redis authentication validation failed.' }
  [IO.File]::WriteAllText($taskSecretPath, (@{port=6381;host='127.0.0.1';password=$taskPassword} | ConvertTo-Json), [Text.UTF8Encoding]::new($false))
  $taskRedisConfig = "bind 127.0.0.1`nport 6381`nprotected-mode yes`nappendonly no`nsave `"`"`ndir `"$($taskRedisRoot.Replace('\','/'))`"`nrequirepass $taskPassword`n"
  [IO.File]::WriteAllText((Join-Path $taskRedisRoot 'redis.local.conf'), $taskRedisConfig, [Text.UTF8Encoding]::new($false))
  Write-Host 'Dedicated Redis 6381 authentication configured and verified. Original Redis 6379 unchanged.'
} finally { $taskClient.Dispose() }
