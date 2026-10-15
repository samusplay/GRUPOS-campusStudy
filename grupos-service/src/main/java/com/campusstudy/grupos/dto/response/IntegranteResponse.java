package com.campusstudy.grupos.dto.response;

import com.campusstudy.grupos.model.RolIntegrante;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record IntegranteResponse(
        Long usuarioId,
        RolIntegrante rol,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime unidoEn
) {
}