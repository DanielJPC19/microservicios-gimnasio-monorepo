package co.analisys.gimnasio.controller;

import co.analisys.gimnasio.dto.OcupacionClase;
import co.analisys.gimnasio.dto.ResumenEntrenamiento;
import co.analisys.gimnasio.service.OcupacionDashboardService;
import co.analisys.gimnasio.service.RecuperacionService;
import co.analisys.gimnasio.service.ResumenEntrenamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gimnasio/monitoreo")
@RequiredArgsConstructor
@Tag(name = "Monitoreo", description = "Monitoreo en tiempo real y analisis de entrenamientos con Kafka")
public class MonitoreoController {

    private final OcupacionDashboardService dashboardService;
    private final ResumenEntrenamientoService resumenEntrenamientoService;
    private final RecuperacionService recuperacionService;

    @GetMapping("/ocupacion")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Ocupacion actual", description = "Estado actual de ocupacion de cada clase, construido desde el topic ocupacion-clases")
    @ApiResponse(responseCode = "200", description = "Ocupacion por clase")
    public Collection<OcupacionClase> obtenerOcupaciones() {
        return dashboardService.obtenerOcupaciones();
    }

    @GetMapping(value = "/ocupacion/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Dashboard en tiempo real (SSE)", description = "Stream Server-Sent Events con cada actualizacion de ocupacion")
    @ApiResponse(responseCode = "200", description = "Stream abierto")
    public SseEmitter streamOcupacion() {
        return dashboardService.suscribir();
    }

    @GetMapping("/entrenamiento/{miembroId}/resumen")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER', 'MEMBER')")
    @Operation(summary = "Resumen semanal de entrenamiento", description = "Ventanas de 7 dias calculadas por Kafka Streams sobre el topic datos-entrenamiento")
    @ApiResponse(responseCode = "200", description = "Resumenes por ventana")
    @ApiResponse(responseCode = "503", description = "Kafka Streams aun no esta listo")
    public List<ResumenEntrenamiento> obtenerResumenEntrenamiento(
            @PathVariable Long miembroId,
            @RequestParam(defaultValue = "4") int semanas) {
        return resumenEntrenamientoService.obtenerResumenes(miembroId, semanas);
    }

    @GetMapping("/recuperacion/estado")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Estado de recuperacion", description = "Checkpoints por particion y cantidad de eventos procesados por topic")
    @ApiResponse(responseCode = "200", description = "Checkpoints y conteo de eventos")
    @ApiResponse(responseCode = "403", description = "Acceso denegado")
    public Map<String, Object> obtenerEstadoRecuperacion() {
        return Map.of(
                "checkpoints", recuperacionService.obtenerCheckpoints(),
                "eventosProcesados", recuperacionService.contarEventosPorTopic());
    }
}
