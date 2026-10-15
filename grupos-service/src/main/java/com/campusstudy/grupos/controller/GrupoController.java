package com.campusstudy.grupos.controller;

import com.campusstudy.grupos.dto.request.CrearGrupoRequest;
import com.campusstudy.grupos.dto.response.GrupoResponse;
import com.campusstudy.grupos.security.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
//Firma del endpoint
@RequestMapping("/api/v1/grupos")
public interface GrupoController {

    //le validamos los parametros de la solicitud osea lo que es @valid
    @PostMapping
    ResponseEntity<GrupoResponse> crear(@UsuarioAutenticado Long usuarioId,
                                        @Valid @RequestBody CrearGrupoRequest request);

    @GetMapping
    ResponseEntity<List<GrupoResponse>> listar(@UsuarioAutenticado Long usuarioId);

    @GetMapping("/{grupo_id}")
    ResponseEntity<GrupoResponse> obtener(@UsuarioAutenticado Long usuarioId,
                                          @PathVariable("grupo_id") Long grupoId);
}