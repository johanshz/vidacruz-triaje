# Construye las imágenes Docker de Triage y Asistente y las sube a GitHub Container Registry (GHCR).
# Antes, una sola vez: docker login ghcr.io -u <tu-usuario-github>   (la clave es un token de GitHub con permiso write:packages)
# Uso, desde la raíz del repositorio:
#   .\deploy\azure\construir-imagenes.ps1 -UsuarioGitHub johanshz
param(
    [Parameter(Mandatory = $true)][string]$UsuarioGitHub,
    [string]$Version = "1.0.0"
)
$ErrorActionPreference = 'Stop'
$usuario = $UsuarioGitHub.ToLower()   # GHCR exige minúsculas
$imagenes = @(
    @{ nombre = "vidacruz-triage";    carpeta = "triage" },
    @{ nombre = "vidacruz-asistente"; carpeta = "asistente" }
)
foreach ($img in $imagenes) {
    $etiqueta = "ghcr.io/$usuario/$($img.nombre):$Version"
    Write-Output "== Construyendo $etiqueta desde ./$($img.carpeta)"
    docker build -t $etiqueta "./$($img.carpeta)"
    if ($LASTEXITCODE -ne 0) { throw "Falló la construcción de $etiqueta" }
    Write-Output "== Subiendo $etiqueta"
    docker push $etiqueta
    if ($LASTEXITCODE -ne 0) { throw "Falló la subida de $etiqueta (¿hiciste docker login ghcr.io?)" }
}
Write-Output ""
Write-Output "Listo. En GitHub > tu perfil > Packages, pon los dos paquetes como 'Public' para que AKS los descargue sin clave."
