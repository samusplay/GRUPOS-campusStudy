package com.campusstudy.grupos.mapper;

import com.campusstudy.grupos.dto.request.CrearGrupoRequest;
import com.campusstudy.grupos.dto.response.GrupoResponse;
import com.campusstudy.grupos.dto.response.IntegranteResponse;
import com.campusstudy.grupos.model.Grupo;
import com.campusstudy.grupos.model.Integrante;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
//clase para mapear campos de la entidad a los dto
//los pdodemos ineyctar en el servicio
public class GrupoMapper {

    // Request + usuario del token → entidad (sin integrantes: eso lo decide el service)
    public Grupo toEntity(CrearGrupoRequest request, Long creadoPor) {
        return Grupo.builder()
                .nombre(request.nombre().trim())
                .materiaId(request.materiaId())
                .creadoPor(creadoPor)
                .build();
    }

    // Entidad → respuesta del contrato
    public GrupoResponse toResponse(Grupo grupo) {
        return new GrupoResponse(
                grupo.getId(),
                grupo.getNombre(),
                grupo.getMateriaId(),
                grupo.getCreadoPor(),
                toIntegranteResponseList(grupo.getIntegrantes()),
                grupo.getFechaCreacion()
        );
    }

    public List<GrupoResponse> toResponseList(List<Grupo> grupos) {
        return grupos.stream()
                .map(this::toResponse)
                .toList();
    }

    public IntegranteResponse toIntegranteResponse(Integrante integrante) {
        return new IntegranteResponse(
                integrante.getUsuarioId(),
                integrante.getRol(),
                integrante.getUnidoEn()
        );
    }

    private List<IntegranteResponse> toIntegranteResponseList(List<Integrante> integrantes) {
        return integrantes.stream()
                .map(this::toIntegranteResponse)
                .toList();
    }
}