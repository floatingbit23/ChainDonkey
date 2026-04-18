$servers = Get-Content "servers.txt"
$success = $false

foreach ($line in $servers) {
    if ($line -match "(.+):(\d+)") {
        $ip = $matches[1]
        $port = $matches[2]
        
        Write-Host "`n[TEST] Probando servidor: ${ip}:${port}" -ForegroundColor Cyan
        
        # Ejecutamos el test con las propiedades del sistema
        mvn test "-Dtest=RealServerConnectionTest" "-Dserver.ip=$ip" "-Dserver.port=$port"
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "`n[EXITO] ¡Encontrado servidor activo! ${ip}:${port}" -ForegroundColor Green
            $success = $true
            break
        } else {
            Write-Host "[FALLO] El servidor ${ip}:${port} no respondió correctamente." -ForegroundColor Red
        }
    }
}

if (-not $success) {
    Write-Host "`n[ERROR] Ninguno de los servidores en la lista respondió." -ForegroundColor Red
}
