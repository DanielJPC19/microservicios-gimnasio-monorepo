package co.analisys.gimnasio.service;

import co.analisys.gimnasio.dto.OcupacionClase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class OcupacionDashboardService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, OcupacionClase> ocupaciones = new ConcurrentHashMap<>();
    private final List<SseEmitter> suscriptores = new CopyOnWriteArrayList<>();

    public void actualizarDashboard(OcupacionClase ocupacion) {
        ocupaciones.put(ocupacion.getClaseId(), ocupacion);
        log.info("[DASHBOARD] Clase {} '{}': {}/{} ({}%)",
                ocupacion.getClaseId(),
                ocupacion.getNombreClase(),
                ocupacion.getOcupacionActual(),
                ocupacion.getCapacidadMaxima(),
                ocupacion.getPorcentajeOcupacion());

        for (SseEmitter suscriptor : suscriptores) {
            enviar(suscriptor, ocupacion);
        }
    }

    public Collection<OcupacionClase> obtenerOcupaciones() {
        return ocupaciones.values();
    }

    public SseEmitter suscribir() {
        SseEmitter suscriptor = new SseEmitter(SSE_TIMEOUT_MS);
        suscriptor.onCompletion(() -> suscriptores.remove(suscriptor));
        suscriptor.onTimeout(() -> suscriptores.remove(suscriptor));
        suscriptor.onError(error -> suscriptores.remove(suscriptor));
        suscriptores.add(suscriptor);

        // El cliente recibe primero el estado actual y luego cada cambio.
        ocupaciones.values().forEach(ocupacion -> enviar(suscriptor, ocupacion));
        return suscriptor;
    }

    private void enviar(SseEmitter suscriptor, OcupacionClase ocupacion) {
        try {
            suscriptor.send(SseEmitter.event().name("ocupacion").data(ocupacion));
        } catch (IOException | IllegalStateException e) {
            suscriptores.remove(suscriptor);
        }
    }
}
