param(
    [string]$Serial = "",
    [string]$Package = "com.aura",
    [switch]$Reset
)

$ErrorActionPreference = "Stop"
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$target = if ($Serial) { @("-s", $Serial) } else { @() }

function Invoke-Adb { & $adb @target @args }

if ($Reset) {
    Invoke-Adb shell dumpsys gfxinfo $Package reset | Out-Null
    Write-Host "Counters reset. Use the app, then run again without -Reset." -ForegroundColor Cyan
    exit 0
}

$raw = Invoke-Adb shell dumpsys gfxinfo $Package

$totalMatch = $raw | Select-String "Total frames rendered:\s+(\d+)"
if (-not $totalMatch) {
    Write-Host "No data. Is the app running? Start it, then: .\tools\jank.ps1 -Reset" -ForegroundColor Yellow
    exit 1
}

function Get-Stat($pattern, $group = 1) {
    $m = $raw | Select-String $pattern
    if ($m) { $m.Matches.Groups[$group].Value } else { "n/a" }
}

$total      = Get-Stat "Total frames rendered:\s+(\d+)"
$jankyCount = Get-Stat "Janky frames:\s+(\d+)\s+\(([\d.]+)%\)" 1
$jankyPct   = [double](Get-Stat "Janky frames:\s+(\d+)\s+\(([\d.]+)%\)" 2)
$p50        = Get-Stat "50th percentile:\s+(\d+)ms"
$p90        = Get-Stat "90th percentile:\s+(\d+)ms"
$p95        = Get-Stat "95th percentile:\s+(\d+)ms"
$p99        = Get-Stat "99th percentile:\s+(\d+)ms"
$missed     = Get-Stat "Number Missed Vsync:\s+(\d+)"
$slowUi     = Get-Stat "Number Slow UI thread:\s+(\d+)"
$slowDraw   = Get-Stat "Number Slow issue draw commands:\s+(\d+)"
$slowBmp    = Get-Stat "Number Slow bitmap uploads:\s+(\d+)"

$color = if ($jankyPct -lt 5) { "Green" } elseif ($jankyPct -lt 15) { "Yellow" } else { "Red" }

Write-Host ""
Write-Host "  Frames rendered : $total"
Write-Host "  Janky frames    : $jankyCount ($jankyPct%)" -ForegroundColor $color
Write-Host ""
Write-Host "  Frame time (budget 16.7ms at 60Hz):"
Write-Host "    p50 : ${p50}ms"
Write-Host "    p90 : ${p90}ms"
Write-Host "    p95 : ${p95}ms"
Write-Host "    p99 : ${p99}ms   <- freezes live here"
Write-Host ""
Write-Host "  Causes:"
Write-Host "    missed vsync        : $missed"
Write-Host "    slow UI thread      : $slowUi   (composition/layout - work on the main thread)"
Write-Host "    slow issue draw     : $slowDraw   (heavy drawing)"
Write-Host "    slow bitmap uploads : $slowBmp   (image decoding)"
Write-Host ""
Write-Host "  For a per-frame breakdown use Perfetto: https://ui.perfetto.dev" -ForegroundColor DarkGray
Write-Host ""
