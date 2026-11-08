# Apaga o enciende el clúster para no gastar crédito cuando nadie lo está usando.
# Apagado, el nodo no cobra; los discos y los datos se conservan.
#   .\deploy\azure\apagar-encender.ps1 -Accion apagar
#   .\deploy\azure\apagar-encender.ps1 -Accion encender
param(
    [Parameter(Mandatory = $true)][ValidateSet("apagar", "encender")][string]$Accion,
    [string]$Grupo = "rg-vidacruz",
    [string]$Cluster = "aks-vidacruz"
)
$ErrorActionPreference = 'Stop'
if ($Accion -eq "apagar") {
    az aks stop --resource-group $Grupo --name $Cluster
} else {
    az aks start --resource-group $Grupo --name $Cluster
}
if ($LASTEXITCODE -ne 0) { throw "Falló az aks $Accion" }
Write-Output "Clúster ${Cluster}: $Accion listo."
