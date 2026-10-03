[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$taskJava = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$taskAuthPath = Join-Path $taskRoot 'runtime-local/db-credentials.json'
if (!(Test-Path $taskAuthPath)) { throw 'Restore private runtime-local/db-credentials.json before startup. Do not reinitialize the existing database.' }
$taskAuth = Get-Content $taskAuthPath -Raw | ConvertFrom-Json
if ($taskAuth.database -ne 'doormes_local' -or $taskAuth.username -ne 'doormes_app') { throw 'Unexpected database identity; refusing startup.' }
$taskRedis = Get-Content (Join-Path $taskRoot 'runtime-local/redis/redis-credentials.json') -Raw | ConvertFrom-Json
if ($taskRedis.port -ne 6381 -or $taskRedis.host -ne '127.0.0.1') { throw 'Unexpected Redis identity; refusing startup.' }
$taskPriorUser = $env:DOORMES_DB_USERNAME
$taskPriorPassword = $env:DOORMES_DB_PASSWORD
$taskPriorRedisPassword = $env:DOORMES_REDIS_PASSWORD
$taskPriorSpringRedisPassword = $env:SPRING_DATA_REDIS_PASSWORD
try {
  $env:DOORMES_DB_USERNAME = $taskAuth.username
  $env:DOORMES_DB_PASSWORD = $taskAuth.password
  $env:DOORMES_REDIS_PASSWORD = $taskRedis.password
  $env:SPRING_DATA_REDIS_PASSWORD = $taskRedis.password
  Push-Location (Join-Path $taskRoot 'backend')
  # Local request tracing otherwise logs login request bodies, including passwords.
  $taskBanner = ([Uri](Join-Path $taskRoot 'backend/yudao-framework/yudao-spring-boot-starter-web/src/main/resources/banner.txt')).AbsoluteUri
  try { & $taskJava '-Dfile.encoding=UTF-8' -Xms256m -Xmx1536m -jar yudao-server/target/yudao-server.jar '--spring.profiles.active=local,doormes' '--logging.level.cn.iocoder.yudao.framework.apilog=WARN' "--spring.banner.location=$taskBanner" } finally { Pop-Location }
} finally {
  $env:DOORMES_DB_USERNAME = $taskPriorUser
  $env:DOORMES_DB_PASSWORD = $taskPriorPassword
  $env:DOORMES_REDIS_PASSWORD = $taskPriorRedisPassword
  $env:SPRING_DATA_REDIS_PASSWORD = $taskPriorSpringRedisPassword
}
