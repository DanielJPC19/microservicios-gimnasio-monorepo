package co.analisys.gimnasio.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import co.analisys.gimnasio.dto.MiembroDTO;
import co.analisys.gimnasio.dto.ResumenMiembroDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Controlador reactivo para el patron API Composition / Filtro de Agregacion de Respuestas.
 * Cumple con el Requisito 5 de 4_implementar.pdf y contexto.md.
 * Consulta de forma concurrente y no bloqueante a los servicios de miembros, clases y pagos
 * utilizando Spring Cloud LoadBalancer (lb://).
 */
@Slf4j
@RestController
@RequestMapping("/api/gimnasio/agregacion")
@RequiredArgsConstructor
public class ResumenMiembroAggregationController {

    private final WebClient.Builder webClientBuilder;

    @GetMapping("/miembro/{id}")
    public Mono<ResumenMiembroDTO> getResumenMiembro(
            @PathVariable Long id,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {

        log.info("Iniciando agregacion para miembro {}", id);
        WebClient client = webClientBuilder.build();

        // 1. Llamada concurrente a miembro-service
        Mono<MiembroDTO> miembroMono = client.get()
                .uri("lb://miembro-service/api/gimnasio/miembros/" + id)
                .headers(headers -> {
                    if (authHeader != null) headers.set(HttpHeaders.AUTHORIZATION, authHeader);
                })
                .retrieve()
                .bodyToMono(MiembroDTO.class)
                .onErrorResume(e -> {
                    log.error("Error al obtener miembro {}: {}", id, e.getMessage());
                    return Mono.just(new MiembroDTO(id, "Desconocido", null, null));
                });

        // 2. Llamada concurrente a clase-service
        Mono<List<Map<String, Object>>> clasesMono = client.get()
                .uri("lb://clase-service/api/gimnasio/clases")
                .headers(headers -> {
                    if (authHeader != null) headers.set(HttpHeaders.AUTHORIZATION, authHeader);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .onErrorResume(e -> {
                    log.error("Error al obtener clases: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                });

        // 3. Llamada concurrente a pago-service (filtrando los pagos del miembro consultado)
        Mono<List<Map<String, Object>>> pagosMono = client.get()
                .uri("lb://pago-service/api/gimnasio/pagos")
                .headers(headers -> {
                    if (authHeader != null) headers.set(HttpHeaders.AUTHORIZATION, authHeader);
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .map(pagos -> pagos.stream()
                        .filter(p -> p.get("miembroId") != null
                                && String.valueOf(id).equals(String.valueOf(p.get("miembroId"))))
                        .toList())
                .onErrorResume(e -> {
                    log.error("Error al obtener pagos: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                });

        // Combinacion reactiva con Mono.zip
        return Mono.zip(miembroMono, clasesMono, pagosMono)
                .map(tuple -> new ResumenMiembroDTO(
                        tuple.getT1(),
                        tuple.getT2(),
                        tuple.getT3(),
                        "Resumen consolidado generado por Spring Cloud Gateway"
                ));
    }
}
