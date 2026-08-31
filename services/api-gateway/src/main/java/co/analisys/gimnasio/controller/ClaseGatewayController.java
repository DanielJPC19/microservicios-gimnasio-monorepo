package co.analisys.gimnasio.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/gimnasio/clases")
@RequiredArgsConstructor
public class ClaseGatewayController {

    @Value("${clase.service.url}")
    private String claseServiceUrl;

    private final RestClient restClient;

    @GetMapping("")
    public ResponseEntity<String> obtenerTodasClases() {
        String respuesta = restClient.get()
                .uri(claseServiceUrl + "/api/gimnasio/clases")
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("")
    public ResponseEntity<String> programarClase(@RequestBody String clase) {
        String respuesta = restClient.post()
                .uri(claseServiceUrl + "/api/gimnasio/clases")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(clase)
                .retrieve()
                .body(String.class);

        return ResponseEntity.ok(respuesta);
    }

}
