package co.analisys.gimnasio.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class RestClientConfig {
    
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
            .requestInterceptor((request, body, execution) -> {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest httpRequest = attrs.getRequest();
                    String authorization = httpRequest.getHeader("Authorization");
                    if (authorization != null) {
                        request.getHeaders().set("Authorization", authorization);
                    }
                }
                return execution.execute(request, body);
            })
            .build();
    }
}