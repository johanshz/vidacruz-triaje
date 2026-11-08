package co.vidacruz.asistente.urgencias.modelo.dto;

import co.vidacruz.asistente.urgencias.modelo.entidad.NivelUrgencias;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public record DtoEstadoUrgencias(long totalActivos, Map<NivelUrgencias, Long> porNivel, Instant consultadoEn) {
    public DtoEstadoUrgencias {
        EnumMap<NivelUrgencias, Long> ordenados = new EnumMap<>(NivelUrgencias.class);
        for (NivelUrgencias nivel : NivelUrgencias.enOrden()) {
            ordenados.put(nivel, porNivel.getOrDefault(nivel, 0L));
        }
        porNivel = Collections.unmodifiableMap(ordenados);
        totalActivos = ordenados.values().stream().mapToLong(Long::longValue).sum();
    }
}
