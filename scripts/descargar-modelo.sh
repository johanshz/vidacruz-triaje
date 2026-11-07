#!/usr/bin/env sh
set -eu

destino="${1:-infraestructura/src/main/resources/model}"
mkdir -p "$destino"

descargar() {
  nombre="$1"
  url="$2"
  esperado="$3"
  temporal="$destino/$nombre.download"
  if [ -f "$destino/$nombre" ]; then
    actual="$(sha256sum "$destino/$nombre" | cut -d ' ' -f 1)"
    if [ "$actual" = "$esperado" ]; then
      return
    fi
    echo "Checksum SHA-256 invÃ¡lido para archivo existente $nombre: $actual" >&2
    exit 1
  fi
  curl --fail --location --retry 3 --output "$temporal" "$url"
  actual="$(sha256sum "$temporal" | cut -d ' ' -f 1)"
  if [ "$actual" != "$esperado" ]; then
    rm -f "$temporal"
    echo "Checksum SHA-256 inválido para $nombre: $actual" >&2
    exit 1
  fi
  mv "$temporal" "$destino/$nombre"
}

base='https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main'
descargar model.onnx "$base/onnx/model_quint8_avx2.onnx" '98a01d88b7de996cdea58c32ca71208c09968d143798814b2ea09d3439dc334f'
descargar tokenizer.json "$base/tokenizer.json" '2c3387be76557bd40970cec13153b3bbf80407865484b209e655e5e4729076b8'
