# Crea el clúster AKS (nivel gratis, 1 nodo) y despliega VidaCruz con los manifiestos de deploy/k8s.
# Antes: az login, y las imágenes subidas con construir-imagenes.ps1.
# Las claves se leen de variables de entorno (nunca se escriben en archivos):
#   VIDACRUZ_DB_PASSWORD, VIDACRUZ_TRIAGE_API_KEY, VIDACRUZ_ASISTENTE_API_KEY  (obligatorias)
#   ANTHROPIC_API_KEY                                                          (opcional: sin ella la IA responde 503)
# Uso, desde la raíz del repositorio:
#   .\deploy\azure\desplegar.ps1 -UsuarioGitHub johanshz
param(
    [Parameter(Mandatory = $true)][string]$UsuarioGitHub,
    [string]$Version = "1.0.0",
    [string]$Grupo = "rg-vidacruz",
    [string]$Region = "eastus",
    [string]$Cluster = "aks-vidacruz",
    [string]$TamanoNodo = "Standard_B2ms"
)
$ErrorActionPreference = 'Stop'
function Paso([string]$texto) { Write-Output ""; Write-Output "== $texto" }
function Revisar([string]$que) { if ($LASTEXITCODE -ne 0) { throw "Falló: $que" } }
function Variable([string]$nombre, [bool]$obligatoria) {
    $valor = [Environment]::GetEnvironmentVariable($nombre, 'Process')
    if ([string]::IsNullOrEmpty($valor)) { $valor = [Environment]::GetEnvironmentVariable($nombre, 'User') }
    if ($obligatoria -and [string]::IsNullOrEmpty($valor)) { throw "Falta la variable de entorno $nombre" }
    return $valor
}

$dbPassword = Variable "VIDACRUZ_DB_PASSWORD" $true
$claveTriage = Variable "VIDACRUZ_TRIAGE_API_KEY" $true
$claveAsistente = Variable "VIDACRUZ_ASISTENTE_API_KEY" $true
$claveAnthropic = Variable "ANTHROPIC_API_KEY" $false
$usuario = $UsuarioGitHub.ToLower()
$imagenTriage = "ghcr.io/$usuario/vidacruz-triage:$Version"
$imagenAsistente = "ghcr.io/$usuario/vidacruz-asistente:$Version"
$k8s = Join-Path $PSScriptRoot "..\k8s"

Paso "1. Revisar la sesión de Azure"
az account show --query "{suscripcion:name, usuario:user.name}" -o table; Revisar "az account show (¿hiciste az login?)"

Paso "2. Registrar el proveedor de AKS en la suscripción (solo tarda la primera vez)"
az provider register --namespace Microsoft.ContainerService --wait; Revisar "registro de Microsoft.ContainerService"

Paso "3. Crear el grupo de recursos $Grupo en $Region"
az group create --name $Grupo --location $Region -o none; Revisar "az group create"

Paso "4. Crear el clúster $Cluster (nivel gratis, 1 nodo $TamanoNodo). Tarda unos 5 a 10 minutos"
$existe = az aks list --resource-group $Grupo --query "[?name=='$Cluster'] | length(@)" -o tsv
if ($existe -eq "0") {
    az aks create --resource-group $Grupo --name $Cluster --tier free --node-count 1 --node-vm-size $TamanoNodo --generate-ssh-keys -o none
    Revisar "az aks create"
} else {
    Write-Output "El clúster ya existe; se reutiliza."
}

Paso "5. Conectar kubectl al clúster"
az aks get-credentials --resource-group $Grupo --name $Cluster --overwrite-existing; Revisar "az aks get-credentials"

Paso "6. Espacio de nombres y configuración"
kubectl apply -f (Join-Path $k8s "00-namespace.yaml"); Revisar "namespace"
kubectl apply -f (Join-Path $k8s "01-configuracion.yaml"); Revisar "configmap"

Paso "7. Secretos (desde variables de entorno; no se muestran)"
$argumentos = @("create", "secret", "generic", "vidacruz-secretos", "--namespace", "vidacruz",
    "--from-literal=DB_NAME=triage", "--from-literal=DB_USER=triage",
    "--from-literal=DB_PASSWORD=$dbPassword",
    "--from-literal=TRIAGE_API_KEY=$claveTriage",
    "--from-literal=ASISTENTE_API_KEY=$claveAsistente")
if (-not [string]::IsNullOrEmpty($claveAnthropic)) { $argumentos += "--from-literal=ANTHROPIC_API_KEY=$claveAnthropic" }
$argumentos += @("--dry-run=client", "-o", "yaml")
(& kubectl @argumentos) | kubectl apply -f - | Out-Null; Revisar "secret"

Paso "8. PostgreSQL, Kafka, topics y Redis"
foreach ($f in "10-postgres.yaml", "11-kafka.yaml", "12-topics.yaml", "13-redis.yaml") {
    kubectl apply -f (Join-Path $k8s $f); Revisar $f
}
kubectl -n vidacruz rollout status statefulset/postgres --timeout=300s; Revisar "postgres listo"
kubectl -n vidacruz rollout status statefulset/kafka --timeout=300s; Revisar "kafka listo"
kubectl -n vidacruz wait --for=condition=complete job/crear-topics --timeout=300s; Revisar "topics creados"

Paso "9. Triage y Asistente con las imágenes $Version"
(Get-Content (Join-Path $k8s "20-triage.yaml") -Raw) -replace 'IMAGEN_TRIAGE', $imagenTriage | kubectl apply -f -; Revisar "triage"
(Get-Content (Join-Path $k8s "21-asistente.yaml") -Raw) -replace 'IMAGEN_ASISTENTE', $imagenAsistente | kubectl apply -f -; Revisar "asistente"
kubectl -n vidacruz rollout status deployment/triage --timeout=600s; Revisar "triage listo"
kubectl -n vidacruz rollout status deployment/asistente --timeout=600s; Revisar "asistente listo"

Paso "10. Esperar las IP públicas"
foreach ($svc in "triage", "asistente") {
    $ip = ""
    for ($i = 0; $i -lt 60 -and [string]::IsNullOrEmpty($ip); $i++) {
        $ip = kubectl -n vidacruz get service $svc -o jsonpath="{.status.loadBalancer.ingress[0].ip}"
        if ([string]::IsNullOrEmpty($ip)) { Start-Sleep -Seconds 5 }
    }
    Write-Output "$svc -> http://$ip"
}
Write-Output ""
Write-Output "Prueba: .\probar-punta-a-punta.ps1 -Triage http://<ip-triage> -Asistente http://<ip-asistente> -ClaveTriage <clave> -ClaveAsistente <clave>"
