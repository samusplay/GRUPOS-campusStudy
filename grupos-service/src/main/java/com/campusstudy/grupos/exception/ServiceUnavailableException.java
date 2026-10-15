package com.campusstudy.grupos.exception;

// 503: un servicio del que dependemos no respondió
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String message) {
        super(message);
    }
}