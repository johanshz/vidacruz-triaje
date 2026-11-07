package co.vidacruz.asistente.protocolo;

final class ModeloOnnxCondicion {
    private ModeloOnnxCondicion() {
    }

    static boolean estaDisponible() {
        ClassLoader cargador = Thread.currentThread().getContextClassLoader();
        return cargador.getResource("model/model.onnx") != null
                && cargador.getResource("model/tokenizer.json") != null;
    }
}
