package com.campusstudy.grupos.exception;

import lombok.Getter;

// 400 sobre un campo concreto, cuando la regla no se puede validar con anotaciones
@Getter
public class BadRequestException extends RuntimeException {

    private final String campo;

    public BadRequestException(String campo, String message) {
        super(message);
        this.campo = campo;
    }
}