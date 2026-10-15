package com.campusstudy.grupos.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record GrupoResponse(
        Long grupoId,
        String nombre,
        Long materiaId,
        Long creadoPor,
        List<IntegranteResponse> integrantes,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime fechaCreacion
) {
}