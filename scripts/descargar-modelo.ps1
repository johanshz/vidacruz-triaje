param(
    [string]$Destino = (Join-Path $PSScriptRoot '..\infraestructura\src\main\resources\model')
)

$ErrorActionPreference = 'Stop'
$revision = 'main'
$archivos = @(
    @{
        Nombre = 'model.onnx'
        Url = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/$revision/onnx/model_quint8_avx2.onnx"
        Sha256 = '98a01d88b7de996cdea58c32ca71208c09968d143798814b2ea09d3439dc334f'
    },
    @{
        Nombre = 'tokenizer.json'
        Url = "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/$revision/tokenizer.json"
        Sha256 = '2c3387be76557bd40970cec13153b3bbf80407865484b209e655e5e4729076b8'
    }
)

$destinoCompleto = [System.IO.Path]::GetFullPath($Destino)
New-Item -ItemType Directory -Force -Path $destinoCompleto | Out-Null

foreach ($archivo in $archivos) {
    $rutaFinal = Join-Path $destinoCompleto $archivo.Nombre
    $rutaTemporal = "$rutaFinal.download"
    Invoke-WebRequest -Uri $archivo.Url -OutFile $rutaTemporal
    $hash = (Get-FileHash -Path $rutaTemporal -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($hash -ne $archivo.Sha256) {
        Remove-Item -LiteralPath $rutaTemporal -Force
        throw "Checksum SHA-256 inválido para $($archivo.Nombre): $hash"
    }
    Move-Item -LiteralPath $rutaTemporal -Destination $rutaFinal -Force
}
