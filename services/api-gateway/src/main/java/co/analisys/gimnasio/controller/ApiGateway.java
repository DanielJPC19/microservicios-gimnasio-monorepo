package co.analisys.gimnasio.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/gimnasio")
@RequiredArgsConstructor
public class ApiGateway {

    @GetMapping("")
    public ResponseEntity<String> getMethodName() {
        // Return that api is working
        return ResponseEntity.ok("API is working");
    }

}
