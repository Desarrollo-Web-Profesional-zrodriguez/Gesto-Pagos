# ==============================================================================
# Gesto Pago - Automated E2E Test Suite Launcher (PowerShell)
# Verifies Python, checks backend status, auto-spawns if needed, and executes tests.
# ==============================================================================

param(
    [switch]$AutoSpawn = $false,
    [string]$BackendUrl = $env:BACKEND_URL,
    [string]$SonarUrl = $env:SONAR_URL,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ForwardArgs
)

$ErrorActionPreference = "Stop"

if (-not $BackendUrl) {
    $BackendUrl = "http://localhost:8085"
}
if (-not $SonarUrl) {
    $SonarUrl = "http://localhost:9000"
}

$env:BACKEND_URL = $BackendUrl
$env:SONAR_URL = $SonarUrl

Write-Host "================================================================================" -ForegroundColor Cyan
Write-Host " GESTO PAGO - E2E TEST RUNNER LAUNCHER (PowerShell)" -ForegroundColor Cyan
Write-Host " Backend Target: $BackendUrl" -ForegroundColor Yellow
Write-Host " SonarQube Target: $SonarUrl" -ForegroundColor Yellow
Write-Host "================================================================================" -ForegroundColor Cyan

# 1. Verify Python 3 Installation
try {
    $pythonVersion = python --version 2>&1
    Write-Host "[OK] Python detected: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Error "Python 3 is not found in system PATH. Please install Python 3.12+."
    exit 1
}

# 2. Check Backend Health
$backendUri = [System.Uri]$BackendUrl
$backendHost = $backendUri.Host
$backendPort = $backendUri.Port

$backendOnline = $false
try {
    $tcpClient = New-Object System.Net.Sockets.TcpClient
    $iar = $tcpClient.BeginConnect($backendHost, $backendPort, $null, $null)
    $success = $iar.AsyncWaitHandle.WaitOne(200, $false)
    if ($success -and $tcpClient.Connected) {
        $backendOnline = $true
        $tcpClient.EndConnect($iar)
        Write-Host "[OK] Backend is listening on port $backendPort." -ForegroundColor Green
    }
    $tcpClient.Close()
} catch {
    $backendOnline = $false
}

$spawnedProcess = $null

if (-not $backendOnline) {
    Write-Host "[INFO] Backend is currently OFFLINE at $BackendUrl." -ForegroundColor Yellow
    if ($AutoSpawn -or $env:SPAWN_BACKEND -eq "true") {
        Write-Host "[INFO] Auto-spawning backend on port $backendPort via gradlew.bat..." -ForegroundColor Cyan
        $spawnedProcess = Start-Process -FilePath ".\gradlew.bat" -ArgumentList "bootRun", "--args='--server.port=$backendPort'" -PassThru -NoNewWindow
        
        Write-Host "[INFO] Waiting for backend to become healthy (timeout 45s)..." -ForegroundColor Cyan
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $ready = $false
        while ($sw.Elapsed.TotalSeconds -lt 45 -and -not $ready) {
            Start-Sleep -Seconds 2
            try {
                $check = Invoke-WebRequest -Uri "$BackendUrl/catalogos/nacionalidades" -Method Get -TimeoutSec 2 -UseBasicParsing -ErrorAction SilentlyContinue
                if ($check.StatusCode -eq 200) {
                    $ready = $true
                    Write-Host "[OK] Backend booted and responding healthy!" -ForegroundColor Green
                }
            } catch {
                # Continue polling
            }
        }
        if (-not $ready) {
            Write-Host "[WARN] Backend failed to respond within 45s; proceeding with offline fallback." -ForegroundColor Yellow
        }
    } else {
        Write-Host "[INFO] Running tests in offline mode (HTTP calls skipped, contract & static tests executed)." -ForegroundColor DarkGray
        Write-Host "       (To auto-spawn backend, pass -AutoSpawn or set `$env:SPAWN_BACKEND='true')" -ForegroundColor DarkGray
    }
}

# 3. Execute run_all.py
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runAllScript = Join-Path $scriptDir "run_all.py"

$pythonArgs = @($runAllScript)
if ($ForwardArgs) {
    $pythonArgs += $ForwardArgs
}

Write-Host "`n[EXEC] Running: python $($pythonArgs -join ' ')`n" -ForegroundColor Cyan

& python @pythonArgs
$exitCode = $LASTEXITCODE

# 4. Clean up spawned background process if any
if ($spawnedProcess -and -not $spawnedProcess.HasExited) {
    Write-Host "[INFO] Stopping background backend process (PID: $($spawnedProcess.Id))..." -ForegroundColor Cyan
    Stop-Process -Id $spawnedProcess.Id -Force -ErrorAction SilentlyContinue
}

Write-Host "Exit Code: $exitCode" -ForegroundColor $(if ($exitCode -eq 0) { "Green" } else { "Red" })
exit $exitCode
