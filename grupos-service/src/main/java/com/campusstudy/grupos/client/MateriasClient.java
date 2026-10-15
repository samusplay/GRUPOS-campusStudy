package com.campusstudy.grupos.client;

public interface MateriasClient {
    //no hay necesidad de pagr el token en la solictud ya que tenemos un interceptor el RestClientConfig
    //la firma habka en terminos de negocio

    // ¿La materia existe y es del usuario autenticado?
    boolean perteneceAlUsuario(Long materiaId);
}
