package com.campusstudy.grupos.service;

import com.campusstudy.grupos.dto.request.CrearGrupoRequest;
import com.campusstudy.grupos.dto.response.GrupoResponse;

import java.util.List;
//firmamos el servicio
public interface GrupoService {
    //cada firma es un contrato con su tipo de datos que nos compremetemos a devolver

    //firma de crear un grupo
    GrupoResponse crearGrupo(CrearGrupoRequest request, Long usuarioId);
    //lista

    List<GrupoResponse> listarMisGrupos(Long usuarioId);
     //obtener el grupo
    GrupoResponse obtenerGrupo(Long grupoId, Long usuarioId);

    //hacer aqui los demas servicios que faltan
}