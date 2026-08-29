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
@RequestMapping("/api/gimnasio/miembros")
@RequiredArgsConstructor
public class MiembroGatewayController {

    @Value("${miembro.service.url}")
    private String miembroServiceUrl;

    private final RestClient restClient;

    @GetMapping("")
    public ResponseEntity<String> obtenerTodosMiembros() {
        String respuesta = restClient.get()
                .uri(miembroServiceUrl + "/api/gimnasio/miembros")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("")
    public ResponseEntity<String> registrarMiembro(@RequestBody String miembro) {
        String respuesta = restClient.post()
                .uri(miembroServiceUrl + "/api/gimnasio/miembros")
                .body(miembro)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

}
