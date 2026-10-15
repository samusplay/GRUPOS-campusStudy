package com.campusstudy.grupos.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

// Formato de error del contrato: { "error": "...", "errors": [{ "campo": "...", "mensaje": "..." }] }
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String error, List<CampoError> errors) {

    public record CampoError(String campo, String mensaje) {
    }

    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, null);
    }
}