package com.campusstudy.grupos.controller.impl;

import com.campusstudy.grupos.controller.GrupoController;
import com.campusstudy.grupos.dto.request.CrearGrupoRequest;
import com.campusstudy.grupos.dto.response.GrupoResponse;
import com.campusstudy.grupos.service.GrupoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
//delegamos al servicio
public class GrupoControllerImpl implements GrupoController {

    private final GrupoService grupoService;

    @Override
    public ResponseEntity<GrupoResponse> crear(Long usuarioId, CrearGrupoRequest request) {
        GrupoResponse grupo = grupoService.crearGrupo(request, usuarioId);
        return ResponseEntity
                .created(URI.create("/api/v1/grupos/" + grupo.grupoId()))
                .body(grupo);
    }

    @Override
    public ResponseEntity<List<GrupoResponse>> listar(Long usuarioId) {
        return ResponseEntity.ok(grupoService.listarMisGrupos(usuarioId));
    }

    @Override
    public ResponseEntity<GrupoResponse> obtener(Long usuarioId, Long grupoId) {
        return ResponseEntity.ok(grupoService.obtenerGrupo(grupoId, usuarioId));
    }
}