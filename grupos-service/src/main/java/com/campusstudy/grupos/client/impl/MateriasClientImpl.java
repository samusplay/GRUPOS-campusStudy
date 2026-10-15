package com.campusstudy.grupos.client.impl;

import com.campusstudy.grupos.client.MateriasClient;

import com.campusstudy.grupos.dto.MateriaResumen;
import com.campusstudy.grupos.exception.ServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MateriasClientImpl implements MateriasClient {

    private static final String MATERIAS_PATH = "/api/v1/materias";

    private final RestClient materiasRestClient;

    @Override
    public boolean perteneceAlUsuario(Long materiaId) {
        List<MateriaResumen> materias = obtenerMateriasDelUsuario();
        return materias.stream()
                .anyMatch(materia -> materia.materiaId().equals(materiaId));
    }

    // Materias filtra por el "sub" del token: solo devuelve las del usuario
    private List<MateriaResumen> obtenerMateriasDelUsuario() {
        try {
            List<MateriaResumen> materias = materiasRestClient.get()
                    .uri(MATERIAS_PATH)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<MateriaResumen>>() {});

            return materias != null ? materias : List.of();

        } catch (ResourceAccessException ex) {
            // No conectó o se pasó del timeout: materias caído o colgado
            log.error("materias-service no responde: {}", ex.getMessage());
            throw new ServiceUnavailableException("Servicio de materias no disponible");

        } catch (RestClientResponseException ex) {
            // Respondió, pero con error (401, 500...). No podemos validar la materia
            log.error("materias-service respondió {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            throw new ServiceUnavailableException("Servicio de materias no disponible");

        } catch (RestClientException ex) {
            // Cualquier otro problema (ej. respuesta que no se pudo leer)
            log.error("Error llamando a materias-service", ex);
            throw new ServiceUnavailableException("Servicio de materias no disponible");
        }
    }
}