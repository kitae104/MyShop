<#
.SYNOPSIS
  로컬 개발 환경(DB 컨테이너 + 백엔드 + 프론트엔드)을 한 번에 실행·중지합니다.

.DESCRIPTION
  start  : Docker 로 DB 를 올리고(healthy 까지 대기), 백엔드(gradlew bootRun)와
           프론트엔드(npm run dev)를 백그라운드로 실행합니다. 로그는 .run\ 에 쌓입니다.
  stop   : 백엔드·프론트엔드 프로세스를 트리째 종료하고 DB 컨테이너를 중지합니다.
           DB 데이터(볼륨)는 지우지 않습니다.
  status : 각 구성 요소의 상태를 보여 줍니다.

  실행 정책 때문에 막히면:  powershell -ExecutionPolicy Bypass -File .\run.ps1

.PARAMETER Action
  start(기본) | stop | status

.PARAMETER KeepDb
  stop 할 때 DB 컨테이너는 계속 실행해 둡니다.

.EXAMPLE
  .\run.ps1
  .\run.ps1 stop
  .\run.ps1 stop -KeepDb
  .\run.ps1 status
#>
[CmdletBinding()]
param(
    [ValidateSet('start', 'stop', 'status')]
    [string]$Action = 'start',
    [switch]$KeepDb
)

# 네이티브 명령(docker, taskkill 등)의 stderr 출력이 예외로 바뀌지 않도록 Continue 로 두고,
# 종료 코드는 직접 확인합니다.
$ErrorActionPreference = 'Continue'

$Root         = $PSScriptRoot
$BackendDir   = Join-Path $Root 'backend'
$FrontendDir  = Join-Path $Root 'frontend'
$RunDir       = Join-Path $Root '.run'
$PidFile      = Join-Path $RunDir 'pids.json'
$BackendPort  = 8080
$FrontendPort = 5173

function Write-Step([string]$Message) { Write-Host "==> $Message" -ForegroundColor Cyan }
function Write-Ok([string]$Message)   { Write-Host "    $Message" -ForegroundColor Green }
function Write-Warn([string]$Message) { Write-Host "    $Message" -ForegroundColor Yellow }

function Fail([string]$Message) {
    Write-Host "오류: $Message" -ForegroundColor Red
    exit 1
}

function Test-Command([string]$Name) {
    return [bool](Get-Command $Name -ErrorAction SilentlyContinue)
}

# 해당 포트를 LISTEN 중인 프로세스 ID (없으면 $null)
function Get-PortOwner([int]$Port) {
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if ($conn) { return $conn.OwningProcess }
    return $null
}

function Test-Alive($ProcessId) {
    if (-not $ProcessId) { return $false }
    return [bool](Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)
}

# 프로세스와 그 자식(gradle -> java, npm -> node)을 함께 종료합니다.
function Stop-Tree($ProcessId) {
    if (Test-Alive $ProcessId) {
        & taskkill.exe /PID $ProcessId /T /F *> $null
    }
}

function Read-Pids {
    if (Test-Path $PidFile) {
        try { return Get-Content $PidFile -Raw | ConvertFrom-Json } catch { return $null }
    }
    return $null
}

function Wait-Http([string]$Url, [int]$TimeoutSec, $WatchPid) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $res = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 3
            if ($res.StatusCode -ge 200 -and $res.StatusCode -lt 400) { return $true }
        } catch { }
        if ($WatchPid -and -not (Test-Alive $WatchPid)) { return $false }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Wait-DbHealthy([int]$TimeoutSec) {
    $id = (& docker compose ps -q db 2>$null | Select-Object -First 1)
    if (-not $id) { return $false }
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $health = (& docker inspect -f '{{.State.Health.Status}}' $id 2>$null)
        if ($health -eq 'healthy') { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

# 이 PC 에서 해당 포트를 LISTEN 중인 프로세스 이름들
function Get-Listeners([int]$Port) {
    $names = @()
    $conns = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    foreach ($conn in $conns) {
        $proc = Get-Process -Id $conn.OwningProcess -ErrorAction SilentlyContinue
        $names += $(if ($proc) { $proc.ProcessName } else { "PID$($conn.OwningProcess)" })
    }
    return @($names | Select-Object -Unique)
}

# Docker(포트 포워딩) 이외의 프로세스가 쓰는 포트인지. 로컬에 설치된 PostgreSQL 등이 해당합니다.
function Test-PortForeign([int]$Port) {
    foreach ($name in (Get-Listeners $Port)) {
        if ($name -notmatch '^(com\.docker|docker|wslrelay|vpnkit)') { return $true }
    }
    return $false
}

# DB 컨테이너가 열 호스트 포트를 정합니다. 로컬 PostgreSQL 이 5432 를 쓰고 있어도 충돌하지 않도록,
# 이미 쓸 수 있는 매핑이 있으면 그것을, 아니면 아무도 쓰지 않는 첫 포트(5432~)를 고릅니다.
function Select-DbPort {
    $mapped = (& docker compose port db 5432 2>$null | Select-Object -First 1)
    if ($mapped -match ':(\d+)$') {
        $port = [int]$Matches[1]
        if (-not (Test-PortForeign $port)) { return $port }
    }
    foreach ($port in 5432..5532) {
        if ((Get-Listeners $port).Count -eq 0) { return $port }
    }
    return $null
}

# 실행 중인 DB 컨테이너의 DB 이름·사용자·비밀번호 (.env 를 직접 읽지 않고 컨테이너 설정에서 가져옵니다)
function Get-DbEnv {
    $id = (& docker compose ps -q db 2>$null | Select-Object -First 1)
    $result = @{}
    foreach ($line in (& docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' $id 2>$null)) {
        if ($line -match '^(POSTGRES_DB|POSTGRES_USER|POSTGRES_PASSWORD)=(.*)$') { $result[$Matches[1]] = $Matches[2] }
    }
    return $result
}

function Show-LogTail([string]$Name) {
    foreach ($suffix in @('.log', '.err.log')) {
        $path = Join-Path $RunDir "$Name$suffix"
        if ((Test-Path $path) -and (Get-Item $path).Length -gt 0) {
            Write-Host "--- $Name$suffix (마지막 20줄)" -ForegroundColor DarkGray
            Get-Content $path -Tail 20
        }
    }
}

function Get-DbState {
    $id = (& docker compose ps -q db 2>$null | Select-Object -First 1)
    if (-not $id) { return '중지됨' }
    $state = (& docker inspect -f '{{.State.Status}} / {{.State.Health.Status}}' $id 2>$null)
    if ($state) { return $state }
    return '중지됨'
}

function Invoke-Status {
    $pids = Read-Pids
    $backendPid  = if ($pids) { $pids.backend } else { $null }
    $frontendPid = if ($pids) { $pids.frontend } else { $null }

    Write-Host ''
    $dbPortInfo = if ($pids -and $pids.dbPort) { "  (localhost:$($pids.dbPort))" } else { '' }
    Write-Host ("DB        : {0}{1}" -f (Get-DbState), $dbPortInfo)
    Write-Host ("백엔드    : {0}  (http://localhost:{1})" -f $(if (Test-Alive $backendPid) { "실행 중 (PID $backendPid)" } else { '중지됨' }), $BackendPort)
    Write-Host ("프론트엔드: {0}  (http://localhost:{1})" -f $(if (Test-Alive $frontendPid) { "실행 중 (PID $frontendPid)" } else { '중지됨' }), $FrontendPort)
    Write-Host ''
}

function Invoke-Start {
    Write-Step '사전 점검'
    foreach ($cmd in @('docker', 'java', 'node', 'npm')) {
        if (-not (Test-Command $cmd)) { Fail "$cmd 명령을 찾을 수 없습니다. 설치 후 다시 실행하세요." }
    }
    & docker info *> $null
    if ($LASTEXITCODE -ne 0) { Fail 'Docker 에 연결할 수 없습니다. Docker Desktop 을 먼저 실행하세요.' }

    $existing = Read-Pids
    if ($existing -and ((Test-Alive $existing.backend) -or (Test-Alive $existing.frontend))) {
        Write-Warn '이미 실행 중입니다. 다시 시작하려면 먼저 .\run.ps1 stop 을 실행하세요.'
        Invoke-Status
        return
    }

    New-Item -ItemType Directory -Force -Path $RunDir | Out-Null
    Push-Location $Root
    try {
        # 전체 스택(컨테이너)의 백엔드가 8080 을 쓰고 있으면 로컬 백엔드와 충돌하므로 먼저 멈춥니다. (중지만, 삭제 아님)
        $containerBackend = (& docker compose ps -q --status running backend 2>$null)
        if ($containerBackend) {
            Write-Step 'Docker 의 backend/frontend 컨테이너를 중지합니다 (포트 충돌 방지, 데이터 유지)'
            & docker compose stop backend frontend *> $null
        }

        foreach ($entry in @(@{ Port = $BackendPort; Name = '백엔드' }, @{ Port = $FrontendPort; Name = '프론트엔드' })) {
            $owner = Get-PortOwner $entry.Port
            if ($owner) {
                $name = (Get-Process -Id $owner -ErrorAction SilentlyContinue).ProcessName
                Fail "$($entry.Name) 포트 $($entry.Port) 를 이미 다른 프로세스가 쓰고 있습니다 (PID $owner, $name)."
            }
        }

        $dbPort = Select-DbPort
        if (-not $dbPort) { Fail 'DB 에 쓸 수 있는 빈 포트(5432~5532)를 찾지 못했습니다.' }
        if ($dbPort -ne 5432) { Write-Warn "5432 포트는 다른 프로세스가 쓰고 있어 DB 컨테이너를 $dbPort 포트로 엽니다." }

        Write-Step "DB 컨테이너 시작 (docker compose up -d db, 포트 $dbPort)"
        $env:DB_PORT = "$dbPort"   # 셸 환경 변수가 .env 보다 우선합니다 (이 프로세스에서만 유효)
        & docker compose up -d db
        if ($LASTEXITCODE -ne 0) { Fail 'DB 컨테이너를 시작하지 못했습니다.' }
        if (-not (Wait-DbHealthy 90)) { Fail 'DB 가 90초 안에 healthy 상태가 되지 않았습니다. docker compose logs db 를 확인하세요.' }
        Write-Ok 'DB 준비 완료'

        # 백엔드가 방금 띄운 컨테이너 DB 에 붙도록 접속 정보를 환경 변수로 넘깁니다.
        # localhost 대신 127.0.0.1 을 써서 IPv6(::1) 로 로컬 PostgreSQL 에 붙는 일을 막습니다.
        $dbEnv = Get-DbEnv
        $dbName = if ($dbEnv['POSTGRES_DB']) { $dbEnv['POSTGRES_DB'] } else { 'myshop' }
        $env:DB_URL      = "jdbc:postgresql://127.0.0.1:$dbPort/$dbName"
        $env:DB_USERNAME = $dbEnv['POSTGRES_USER']
        $env:DB_PASSWORD = $dbEnv['POSTGRES_PASSWORD']
    } finally {
        Pop-Location
    }

    Write-Step '백엔드 시작 (gradlew bootRun)'
    $backend = Start-Process -FilePath (Join-Path $BackendDir 'gradlew.bat') `
        -ArgumentList @('bootRun', '--console=plain') `
        -WorkingDirectory $BackendDir -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $RunDir 'backend.log') `
        -RedirectStandardError (Join-Path $RunDir 'backend.err.log')

    if (-not (Test-Path (Join-Path $FrontendDir 'node_modules'))) {
        Write-Step '프론트엔드 의존성 설치 (npm ci)'
        Push-Location $FrontendDir
        try {
            & npm ci
            if ($LASTEXITCODE -ne 0) { Stop-Tree $backend.Id; Fail 'npm ci 에 실패했습니다.' }
        } finally {
            Pop-Location
        }
    }

    Write-Step '프론트엔드 시작 (npm run dev)'
    $frontend = Start-Process -FilePath 'cmd.exe' `
        -ArgumentList @('/c', 'npm run dev') `
        -WorkingDirectory $FrontendDir -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $RunDir 'frontend.log') `
        -RedirectStandardError (Join-Path $RunDir 'frontend.err.log')

    @{ backend = $backend.Id; frontend = $frontend.Id; dbPort = $dbPort } | ConvertTo-Json | Set-Content -Path $PidFile -Encoding ASCII

    Write-Step '기동 대기 (백엔드는 처음에 1~2분 걸릴 수 있습니다)'
    $backendOk = Wait-Http "http://localhost:$BackendPort/actuator/health" 180 $backend.Id
    if (-not $backendOk) {
        Show-LogTail 'backend'
        Invoke-Stop -Quiet
        Fail '백엔드가 시작되지 않았습니다. 위 로그를 확인하세요. 전체 로그: .run\backend.log'
    }
    Write-Ok "백엔드 준비 완료  http://localhost:$BackendPort"

    $frontendOk = Wait-Http "http://localhost:$FrontendPort" 60 $frontend.Id
    if (-not $frontendOk) {
        Show-LogTail 'frontend'
        Invoke-Stop -Quiet
        Fail '프론트엔드가 시작되지 않았습니다. 위 로그를 확인하세요. 전체 로그: .run\frontend.log'
    }
    Write-Ok "프론트엔드 준비 완료  http://localhost:$FrontendPort"

    Write-Host ''
    Write-Host '모두 실행했습니다.' -ForegroundColor Green
    Write-Host "  화면   : http://localhost:$FrontendPort"
    Write-Host "  로그   : $RunDir  (backend.log, frontend.log)"
    Write-Host '  중지   : .\run.ps1 stop'
}

function Invoke-Stop([switch]$Quiet) {
    if (-not $Quiet) { Write-Step '백엔드·프론트엔드 중지' }
    $pids = Read-Pids
    if ($pids) {
        Stop-Tree $pids.frontend
        Stop-Tree $pids.backend
    }

    # PID 파일이 없거나 PowerShell 창을 닫아 자식이 남은 경우: 이 프로젝트 경로로 실행된 프로세스만 포트 기준으로 정리합니다.
    foreach ($port in @($BackendPort, $FrontendPort)) {
        $owner = Get-PortOwner $port
        if ($owner) {
            $cmdLine = (Get-CimInstance Win32_Process -Filter "ProcessId = $owner" -ErrorAction SilentlyContinue).CommandLine
            if ($cmdLine -and $cmdLine.Contains($Root)) {
                Stop-Tree $owner
            } elseif (-not $Quiet) {
                Write-Warn "포트 $port 는 이 프로젝트가 아닌 프로세스(PID $owner)가 쓰고 있어 건드리지 않았습니다."
            }
        }
    }
    Remove-Item $PidFile -ErrorAction SilentlyContinue
    if (-not $Quiet) { Write-Ok '백엔드·프론트엔드 중지 완료' }

    if (-not $KeepDb) {
        if (-not $Quiet) { Write-Step 'DB 컨테이너 중지 (데이터는 유지)' }
        Push-Location $Root
        try { & docker compose stop db *> $null } finally { Pop-Location }
        if (-not $Quiet) { Write-Ok 'DB 중지 완료' }
    }
}

switch ($Action) {
    'start'  { Invoke-Start }
    'stop'   { Invoke-Stop; Invoke-Status }
    'status' { Invoke-Status }
}
