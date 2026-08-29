package co.analisys.gimnasio.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/gimnasio/equipos")
@RequiredArgsConstructor
public class EquipoGatewayController {

    @Value("${equipo.service.url}")
    private String equipoServiceUrl;

    private final RestClient restClient;

    @GetMapping("")
    public ResponseEntity<String> obtenerTodosEquipos() {
        String respuesta = restClient.get()
                .uri(equipoServiceUrl + "/api/gimnasio/equipos")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("")
    public ResponseEntity<String> agregarEquipo(@RequestBody String equipo) {
        String respuesta = restClient.post()
                .uri(equipoServiceUrl + "/api/gimnasio/equipos")
                .body(equipo)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

}
