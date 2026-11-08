package co.vidacruz.triage.alerta.consumidor;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MemoriaIdsEventoProcesados {
    private final int capacidad;
    private final ConcurrentHashMap<UUID, Boolean> ids = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<UUID> orden = new ConcurrentLinkedQueue<>();

    public MemoriaIdsEventoProcesados(@Value("${vidacruz.alerta.memoria-ids:10000}") int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("vidacruz.alerta.memoria-ids debe ser mayor que cero");
        }
        this.capacidad = capacidad;
    }

    public boolean registrar(UUID idEvento) {
        AtomicBoolean nuevo = new AtomicBoolean();
        ids.computeIfAbsent(idEvento, ignorado -> {
            nuevo.set(true);
            orden.add(idEvento);
            return Boolean.TRUE;
        });
        if (nuevo.get()) {
            while (ids.size() > capacidad) {
                UUID masAntiguo = orden.poll();
                if (masAntiguo != null) {
                    ids.remove(masAntiguo);
                }
            }
        }
        return nuevo.get();
    }

    public void eliminar(UUID idEvento) {
        ids.remove(idEvento);
        orden.remove(idEvento);
    }
}
