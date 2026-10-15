package com.campusstudy.grupos.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
//montamos la configuracion con el otro microservicio
//es un cliente http
//lo pegamos a todas las llamadas
public class RestClientConfig {

    @Bean
    public RestClient materiasRestClient(RestClient.Builder builder,
                                         @Value("${app.clients.materias-url}") String materiasUrl,
                                         @Value("${app.clients.timeout-ms}") int timeoutMs) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        return builder
                .baseUrl(materiasUrl)
                .requestFactory(factory)
                .requestInterceptor(reenviarToken())
                .build();
    }

    // Token relay: copia el "Authorization" de la petición que llegó a grupos
    // en la petición que grupos le hace a materias
    private ClientHttpRequestInterceptor reenviarToken() {
        return (request, body, execution) -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                HttpServletRequest entrante = attrs.getRequest();
                String authorization = entrante.getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
                }
            }
            return execution.execute(request, body);
        };
    }
}