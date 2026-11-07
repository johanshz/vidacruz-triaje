$ErrorActionPreference = 'Stop'
$directorio = Split-Path -Parent $PSScriptRoot
$archivoEnv = Join-Path $directorio '.env'
$creoEnv = -not (Test-Path -LiteralPath $archivoEnv)
$proyectoCompose = 'asistente-ca25-qa'

if ($creoEnv) {
    [System.IO.File]::WriteAllText($archivoEnv, 'ASISTENTE_API_KEY=ca25-test-key', [System.Text.UTF8Encoding]::new($false))
}

$env:ASISTENTE_API_KEY = 'ca25-test-key'
Push-Location $directorio
try {
    docker compose -p $proyectoCompose -f docker-compose.yml -f docker-compose.qa.yml up --build -d
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose no pudo levantar los servicios QA.' }

    $puertos = docker compose -p $proyectoCompose -f docker-compose.yml -f docker-compose.qa.yml port asistente 8080
    if ($LASTEXITCODE -ne 0 -or ($puertos | Where-Object { $_ -notmatch ':18080$' })) {
        throw "El Asistente no quedó publicado únicamente en el puerto QA 18080: $puertos"
    }

    $listo = $false
    for ($intento = 0; $intento -lt 30; $intento++) {
        try {
            $respuesta = Invoke-WebRequest -UseBasicParsing -TimeoutSec 2 http://localhost:18080/actuator/health/readiness
            if ($respuesta.StatusCode -eq 200) { $listo = $true; break }
        } catch { }
        Start-Sleep -Seconds 2
    }
    if (-not $listo) { throw 'Readiness no respondió 200 dentro de 60 segundos.' }

    $respuestaFinal = Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 http://localhost:18080/actuator/health/readiness
    Write-Output "Readiness respondió HTTP $($respuestaFinal.StatusCode)."
} finally {
    docker compose -p $proyectoCompose -f docker-compose.yml -f docker-compose.qa.yml down
    if ($creoEnv) { Remove-Item -LiteralPath $archivoEnv }
    Remove-Item Env:ASISTENTE_API_KEY -ErrorAction SilentlyContinue
    Pop-Location
}
