param(
    [switch]$ForceRestart
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$runLogsDir = Join-Path $root "run-logs"
New-Item -ItemType Directory -Path $runLogsDir -Force | Out-Null

$jvmArgs = @(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.math=ALL-UNNAMED",
    "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED"
)

$services = @(
    @{
        Name = "sensorhub-backend"
        Port = 7529
        Jar = Join-Path $root "sensorhub-backend\target\sensorhub-backend-0.0.1-SNAPSHOT.jar"
        Log = Join-Path $runLogsDir "sensorhub-backend-jar.log"
        ErrLog = Join-Path $runLogsDir "sensorhub-backend-jar.err.log"
    },
    @{
        Name = "sensorhub-interface"
        Port = 8123
        Jar = Join-Path $root "sensorhub-backend\sensorhub-interface\target\sensorhub-interface-0.0.1-SNAPSHOT.jar"
        Log = Join-Path $runLogsDir "sensorhub-interface-jar.log"
        ErrLog = Join-Path $runLogsDir "sensorhub-interface-jar.err.log"
    },
    @{
        Name = "sensorhub-gateway"
        Port = 8090
        Jar = Join-Path $root "sensorhub-backend\sensorhub-gateway\target\sensorhub-gateway-0.0.1-SNAPSHOT.jar"
        Log = Join-Path $runLogsDir "sensorhub-gateway.log"
        ErrLog = Join-Path $runLogsDir "sensorhub-gateway.err.log"
    }
)

function Stop-ListenerOnPort {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $listeners = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    if (-not $listeners) {
        return
    }

    $listenerPids = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $listenerPids) {
        try {
            Stop-Process -Id $processId -Force -ErrorAction Stop
            Write-Host "Stopped existing process on port $Port (PID $processId)"
        } catch {
            Write-Warning "Failed to stop PID $processId on port $($Port): $($_.Exception.Message)"
        }
    }
}

function Wait-PortReady {
    param(
        [Parameter(Mandatory = $true)]
        [int]$Port,
        [int]$TimeoutSeconds = 45
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
        if ($listener) {
            return $true
        }
        Start-Sleep -Milliseconds 800
    }
    return $false
}

foreach ($service in $services) {
    if (-not (Test-Path $service.Jar)) {
        throw "Jar not found for $($service.Name): $($service.Jar)"
    }

    if ($ForceRestart) {
        Stop-ListenerOnPort -Port $service.Port
    } else {
        $existing = Get-NetTCPConnection -State Listen -LocalPort $service.Port -ErrorAction SilentlyContinue
        if ($existing) {
            Write-Host "$($service.Name) is already listening on port $($service.Port), skip start."
            continue
        }
    }

    if (Test-Path $service.Log) {
        Remove-Item -LiteralPath $service.Log -Force
    }
    if (Test-Path $service.ErrLog) {
        Remove-Item -LiteralPath $service.ErrLog -Force
    }

    $process = Start-Process -FilePath "java" `
        -ArgumentList @($jvmArgs + "-jar" + $service.Jar) `
        -WorkingDirectory $root `
        -RedirectStandardOutput $service.Log `
        -RedirectStandardError $service.ErrLog `
        -WindowStyle Hidden `
        -PassThru

    if (Wait-PortReady -Port $service.Port) {
        Write-Host "Started $($service.Name) on port $($service.Port) (PID $($process.Id))"
    } else {
        Write-Warning "Timed out waiting for $($service.Name) on port $($service.Port). Check $($service.Log) and $($service.ErrLog)"
    }
}
