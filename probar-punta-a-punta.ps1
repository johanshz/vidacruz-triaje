# Prueba de punta a punta de VidaCruz con el docker-compose único (Triage + Asistente + un solo Kafka).
# Uso (PowerShell, en esta carpeta, con "docker compose up -d --build" ya corriendo):
#   .\probar-punta-a-punta.ps1
#   .\probar-punta-a-punta.ps1 -ProbarAlerta     # además espera la alerta SSE (levanta con SLA crítico = 1 min)
param(
    [string]$Triage = "http://localhost:8081",
    [string]$Asistente = "http://localhost:8082",
    [string]$ClaveTriage = "clave-local-triage",
    [string]$ClaveAsistente = "clave-local-asistente",
    [switch]$ProbarAlerta,
    # En Azure (AKS) los logs se leen con kubectl en vez de docker
    [switch]$Kubernetes
)
$ErrorActionPreference = 'Stop'
$resultados = New-Object System.Collections.Generic.List[string]
function Anotar([bool]$ok, [string]$texto) {
    $marca = if ($ok) { 'OK ' } else { 'FALLA' }
    $linea = "[$marca] $texto"
    $resultados.Add($linea)
    Write-Output $linea
}
function Esperar([scriptblock]$condicion, [int]$segundos) {
    $limite = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $limite) {
        try { if (& $condicion) { return $true } } catch { }
        Start-Sleep -Seconds 1
    }
    return $false
}
# La coma evita que PowerShell desarme el arreglo de bytes al devolverlo
function JsonUtf8([string]$json) { return ,([System.Text.Encoding]::UTF8.GetBytes($json)) }
function Logs([string]$contenedor) {
    # docker escribe parte de los logs por stderr; se juntan sin que PowerShell lo trate como error
    $anterior = $ErrorActionPreference; $ErrorActionPreference = 'Continue'
    try {
        if ($Kubernetes) {
            $app = if ($contenedor -like "*asistente*") { "asistente" } else { "triage" }
            return (cmd /c "kubectl -n vidacruz logs -l app=$app --tail=-1 2>&1")
        }
        return (cmd /c "docker logs $contenedor 2>&1")
    } finally { $ErrorActionPreference = $anterior }
}
function Tablero() {
    return Invoke-RestMethod -Uri "$Asistente/asistente/estado-urgencias" -Headers @{ 'X-API-Key' = $ClaveAsistente }
}

Write-Output "== 1. Los dos servicios están listos"
$triageListo = Esperar { (Invoke-WebRequest -UseBasicParsing "$Triage/actuator/health/readiness").StatusCode -eq 200 } 180
$asistenteListo = Esperar { (Invoke-WebRequest -UseBasicParsing "$Asistente/actuator/health/readiness").StatusCode -eq 200 } 180
Anotar $triageListo "Triage readiness 200"
Anotar $asistenteListo "Asistente readiness 200"
if (-not ($triageListo -and $asistenteListo)) { throw "Los servicios no están listos; revisa 'docker compose ps' y 'docker compose logs'." }

Write-Output "== 2. La enfermera registra a María (URGENTE por fiebre)"
$antes = (Tablero).porNivel.URGENTE
$cuerpo = '{"nombreCompleto":"María López","edad":34,"frecuenciaCardiaca":88,"presionSistolica":118,"presionDiastolica":76,"spo2":97,"temperatura":38.7,"dolor":4,"gcs":15,"quejaPrincipal":"Fiebre y dolor de garganta"}'
$respuesta = Invoke-WebRequest -UseBasicParsing -Method Post -Uri "$Triage/triage/pacientes" `
    -Headers @{ 'X-API-Key' = $ClaveTriage } -ContentType 'application/json; charset=utf-8' -Body (JsonUtf8 $cuerpo)
$maria = $respuesta.Content | ConvertFrom-Json
$traceId = $respuesta.Headers['X-Trace-Id']
Anotar ($respuesta.StatusCode -eq 201 -and $maria.nivel -eq 'URGENTE') "POST /triage/pacientes -> 201 URGENTE (id $($maria.id))"

Write-Output "== 3. El Asistente la cuenta en el tablero (Triage -> outbox -> Kafka -> Asistente -> Redis)"
$llego = Esperar { (Tablero).porNivel.URGENTE -eq ($antes + 1) } 30
Anotar $llego "Tablero URGENTE pasó de $antes a $($antes + 1)"

Write-Output "== 4. El traceId de la respuesta del Triage llega a los logs del Asistente (por Kafka)"
if ($traceId) {
    $enAsistente = Esperar { (Logs "vidacruz-asistente-1" | Select-String -SimpleMatch $traceId | Measure-Object).Count -gt 0 } 15
    Anotar $enAsistente "traceId $traceId (X-Trace-Id del Triage) en logs del Asistente"
} else {
    Anotar $false "La respuesta no trajo X-Trace-Id"
}

Write-Output "== 5. El médico la atiende y le da de alta; el tablero la quita"
$hdrT = @{ 'X-API-Key' = $ClaveTriage }
$c1 = Invoke-RestMethod -Method Patch -Uri "$Triage/triage/pacientes/$($maria.id)/estado" -Headers $hdrT -ContentType 'application/json' -Body '{"estado":"EN_CONSULTA"}'
$c2 = Invoke-RestMethod -Method Patch -Uri "$Triage/triage/pacientes/$($maria.id)/estado" -Headers $hdrT -ContentType 'application/json' -Body '{"estado":"ALTA"}'
Anotar ($c1.estado -eq 'EN_CONSULTA' -and $c2.estado -eq 'ALTA') "PATCH EN_CONSULTA y ALTA -> 200"
$salio = Esperar { (Tablero).porNivel.URGENTE -eq $antes } 30
Anotar $salio "Tablero URGENTE volvió a $antes"

Write-Output "== 6. Volver atrás está prohibido"
try {
    Invoke-RestMethod -Method Patch -Uri "$Triage/triage/pacientes/$($maria.id)/estado" -Headers $hdrT -ContentType 'application/json' -Body '{"estado":"REGISTRADO"}' | Out-Null
    Anotar $false "ALTA -> REGISTRADO debía dar 409"
} catch {
    Anotar ($_.Exception.Response.StatusCode.value__ -eq 409) "ALTA -> REGISTRADO -> 409"
}

Write-Output "== 7. Reporte por hora"
$desde = (Get-Date).ToUniversalTime().AddHours(-1).ToString("yyyy-MM-ddTHH:mm:ssZ")
$hasta = (Get-Date).ToUniversalTime().AddHours(1).ToString("yyyy-MM-ddTHH:mm:ssZ")
$reporte = Invoke-RestMethod -Uri "$Triage/triage/reportes/operacion?desde=$desde&hasta=$hasta" -Headers $hdrT
Anotar (@($reporte.horas).Count -ge 1) "GET /triage/reportes/operacion -> $(@($reporte.horas).Count) hora(s) con datos"

Write-Output "== 8. Asistente de protocolos"
$hdrA = @{ 'X-API-Key' = $ClaveAsistente }
$diabetes = Invoke-RestMethod -Method Post -Uri "$Asistente/asistente/consulta" -Headers $hdrA -ContentType 'application/json; charset=utf-8' `
    -Body (JsonUtf8 '{"pregunta":"¿Cuál es la primera línea en diabetes tipo 2?"}')
Anotar ($diabetes.respuesta -eq 'No existe un protocolo disponible para esta pregunta' -and @($diabetes.fuentes).Count -eq 0) "Diabetes -> frase fija sin fuentes (sin llamar a la IA)"
try {
    $crisis = Invoke-RestMethod -Method Post -Uri "$Asistente/asistente/consulta" -Headers $hdrA -ContentType 'application/json; charset=utf-8' `
        -Body (JsonUtf8 '{"pregunta":"¿Cómo se maneja una crisis hipertensiva?"}')
    $citaProto001 = @($crisis.fuentes | Where-Object { $_.protocolo -eq 'PROTO-001' }).Count -gt 0
    Anotar $citaProto001 "Crisis hipertensiva -> respuesta con fuentes PROTO-001"
} catch {
    $codigo = $_.Exception.Response.StatusCode.value__
    Anotar ($codigo -eq 503) "Crisis hipertensiva -> 503 IA_NO_DISPONIBLE (sin ANTHROPIC_API_KEY es lo esperado)"
}

if ($ProbarAlerta) {
    Write-Output "== 9. SLA vencido -> alerta SSE (levanta con VIDACRUZ_SLA_MINUTOS_CRITICO=1 y VIDACRUZ_VIGILANCIA_INTERVALO=PT10S)"
    $archivo = Join-Path $env:TEMP "vidacruz-sse.txt"
    if (Test-Path $archivo) { Remove-Item $archivo }
    # Un solo texto con comillas: Windows PowerShell no las agrega solo y la cabecera se partiría
    $argumentos = "-sN --max-time 150 -H `"X-API-Key: $ClaveTriage`" -H `"Accept: text/event-stream`" -o `"$archivo`" $Triage/triage/alertas/flujo"
    $sse = Start-Process -FilePath "curl.exe" -ArgumentList $argumentos -PassThru -WindowStyle Hidden
    Start-Sleep -Seconds 3
    $luis = '{"nombreCompleto":"Luis Critico","edad":70,"frecuenciaCardiaca":150,"presionSistolica":80,"presionDiastolica":50,"spo2":85,"temperatura":39.5,"dolor":9,"gcs":9,"quejaPrincipal":"Dolor toracico"}'
    $registro = Invoke-RestMethod -Method Post -Uri "$Triage/triage/pacientes" -Headers $hdrT -ContentType 'application/json' -Body (JsonUtf8 $luis)
    $alerta = Esperar { (Test-Path $archivo) -and ((Get-Content $archivo -Raw) -match $registro.id) } 140
    Anotar $alerta "Alerta SSE de Luis ($($registro.id)) recibida"
    if (-not $sse.HasExited) { Stop-Process -Id $sse.Id -Force }
}

Write-Output ""
Write-Output "== Resumen"
$resultados | ForEach-Object { Write-Output $_ }
$fallas = @($resultados | Where-Object { $_ -like '*FALLA*' }).Count
if ($fallas -gt 0) { Write-Output "$fallas falla(s)"; exit 1 } else { Write-Output "Todo OK" }
