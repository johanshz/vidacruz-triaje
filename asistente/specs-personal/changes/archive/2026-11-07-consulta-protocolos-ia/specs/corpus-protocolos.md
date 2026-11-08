# corpus-protocolos

## NUEVO

### Requirement: Corpus oficial e índice vectorial cargados al arrancar
La infraestructura carga los textos literales de PROTO-001, PROTO-002 y PROTO-003 (siete secciones), crea un fragmento por sección con metadatos `protocolo` y `seccion`, y los indexa en `SimpleVectorStore` usando embeddings ONNX locales. El dominio consulta mediante `BuscadorProtocolos` con umbral configurable y `topK` configurable. La aplicación falla al arrancar si falta o está dañado el corpus o el modelo. El cálculo de embeddings no bloquea el event loop.

**Origen en el RFC:** F-01, RN-03, RN-10, CA-18, CA-19

#### Scenario: Cargar corpus y metadatos al arrancar
**Origen en el RFC:** CA-18
- **WHEN** arranca la aplicación con los tres archivos válidos del corpus
- **THEN** el índice contiene siete fragmentos con metadatos de protocolo y sección

#### Scenario: Fallar rápido si falta el corpus o el modelo
**Origen en el RFC:** CA-18
- **WHEN** falta o está dañado un archivo del corpus, o no se puede cargar el modelo de embeddings
- **THEN** la aplicación no arranca

#### Scenario: Calibrar recuperación con las tres consultas de ejemplo
**Origen en el RFC:** CA-19
- **WHEN** se ejecuta la calibración con el modelo multilingüe real
- **THEN** las consultas de crisis hipertensiva y sepsis recuperan sus protocolos y la consulta de diabetes tipo 2 no supera el umbral, y se documenta el umbral resultante en README y `contexto/tecnico.md` S-08

#### Scenario: Registrar calibración pendiente si no está disponible el modelo real
**Origen en el RFC:** CA-19
- **WHEN** no hay red o no se dispone del modelo multilingüe real
- **THEN** la prueba de calibración se omite de forma explícita y queda un pendiente, sin declarar calibrado el índice
