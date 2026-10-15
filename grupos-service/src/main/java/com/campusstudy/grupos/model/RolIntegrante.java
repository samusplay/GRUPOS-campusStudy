package com.campusstudy.grupos.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RolIntegrante {

    LIDER("lider"),
    MIEMBRO("miembro");

    // Valor que viaja en el JSON, como pide el contrato: "lider" / "miembro"
    @JsonValue
    private final String valor;
}