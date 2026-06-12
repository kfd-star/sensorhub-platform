$ports = 7529, 8123, 8090

foreach ($port in $ports) {
    $listeners = Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue
    if (-not $listeners) {
        Write-Host "No process is listening on port $port"
        continue
    }

    $listenerPids = $listeners | Select-Object -ExpandProperty OwningProcess -Unique
    foreach ($processId in $listenerPids) {
        try {
            Stop-Process -Id $processId -Force -ErrorAction Stop
            Write-Host "Stopped process on port $port (PID $processId)"
        } catch {
            Write-Warning "Failed to stop PID $processId on port $($port): $($_.Exception.Message)"
        }
    }
}
