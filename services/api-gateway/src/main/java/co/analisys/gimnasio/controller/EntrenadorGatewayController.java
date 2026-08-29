package co.analisys.gimnasio.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/gimnasio/entrenadores")
@RequiredArgsConstructor
public class EntrenadorGatewayController {

    @Value("${entrenador.service.url}")
    private String entrenadorServiceUrl;

    private final RestClient restClient;

    @GetMapping("")
    public ResponseEntity<String> obtenerTodosEntrenadores() {
        String respuesta = restClient.get()
                .uri(entrenadorServiceUrl + "/api/gimnasio/entrenadores")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("")
    public ResponseEntity<String> agregarEntrenador(@RequestBody String entrenador) {
        String respuesta = restClient.post()
                .uri(entrenadorServiceUrl + "/api/gimnasio/entrenadores")
                .body(entrenador)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> obtenerEntrenadorPorId(@PathVariable Long id) {
        String respuesta = restClient.get()
                .uri(entrenadorServiceUrl + "/api/gimnasio/entrenadores/" + id)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

}
