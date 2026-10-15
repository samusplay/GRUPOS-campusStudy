package com.campusstudy.grupos.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Marca un parámetro del controller para recibir el usuario_id del JWT
//una firma para decirle al servicio que queremos guardar el usuario
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface UsuarioAutenticado {
}