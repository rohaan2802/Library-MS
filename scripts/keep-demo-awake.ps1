# Keep LibraryMS live demo awake (Aiven MySQL + SnapDeploy HTTP).
# Run every 10 minutes via Windows Task Scheduler — no new repo needed.
#
# Example (PowerShell as Admin once):
#   $action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"C:\Users\CodeTech\Desktop\LibraryMS\scripts\keep-demo-awake.ps1`""
#   $trigger = New-ScheduledTaskTrigger -Once -At (Get-Date) -RepetitionInterval (New-TimeSpan -Minutes 10) -RepetitionDuration ([TimeSpan]::MaxValue)
#   Register-ScheduledTask -TaskName "LibraryMS-KeepAwake" -Action $action -Trigger $trigger -Description "Ping Aiven + SnapDeploy"

$ErrorActionPreference = "Continue"

# --- Edit if Aiven host/port changes (from Aiven Overview) ---
$AivenHost = "mysql-1151d526-project-ec1.h.aivencloud.com"
$AivenPort = 25038
$AivenUser = "avnadmin"
$AivenDb   = "defaultdb"
# Prefer env var so password is not stored in git:
#   [System.Environment]::SetEnvironmentVariable("AIVEN_MYSQL_PASSWORD", "your-password", "User")
$AivenPassword = $env:AIVEN_MYSQL_PASSWORD
if (-not $AivenPassword) {
    Write-Host "Set user env AIVEN_MYSQL_PASSWORD first, then re-run."
    exit 1
}

$SnapUrl = if ($env:SNAPDEPLOY_URL) { $env:SNAPDEPLOY_URL.TrimEnd("/") } else { "https://libraryms-0899b.containers.snapdeploy.app" }

$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysql)) {
    $mysql = (Get-Command mysql -ErrorAction SilentlyContinue)?.Source
}

Write-Host "$(Get-Date -Format o) ping SnapDeploy $SnapUrl"
try {
    $r = Invoke-WebRequest -Uri "$SnapUrl/actuator/health" -TimeoutSec 90 -UseBasicParsing
    Write-Host "health $($r.StatusCode)"
} catch {
    try {
        $r2 = Invoke-WebRequest -Uri "$SnapUrl/login" -TimeoutSec 90 -UseBasicParsing
        Write-Host "login $($r2.StatusCode)"
    } catch {
        Write-Host "SnapDeploy still down: $($_.Exception.Message)"
    }
}

if ($mysql) {
    Write-Host "ping Aiven $AivenHost:$AivenPort/$AivenDb"
    $env:MYSQL_PWD = $AivenPassword
    & $mysql -h $AivenHost -P $AivenPort -u $AivenUser --ssl-mode=REQUIRED --connect-timeout=20 $AivenDb -e "SELECT 1 AS keep_awake, NOW() AS checked_at;"
    $code = $LASTEXITCODE
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    if ($code -eq 0) { Write-Host "Aiven OK" } else { Write-Host "Aiven failed exit=$code (Power on in Aiven console if powered off)" }
} else {
    Write-Host "mysql.exe not found — install MySQL client or fix path"
}
