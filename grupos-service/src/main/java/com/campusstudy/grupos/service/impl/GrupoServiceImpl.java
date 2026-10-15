package com.campusstudy.grupos.service.impl;

import com.campusstudy.grupos.client.MateriasClient;
import com.campusstudy.grupos.dto.request.CrearGrupoRequest;
import com.campusstudy.grupos.dto.response.GrupoResponse;
import com.campusstudy.grupos.exception.BadRequestException;
import com.campusstudy.grupos.exception.ConflictException;
import com.campusstudy.grupos.exception.NotFoundException;
import com.campusstudy.grupos.mapper.GrupoMapper;
import com.campusstudy.grupos.model.Grupo;
import com.campusstudy.grupos.model.Integrante;
import com.campusstudy.grupos.model.RolIntegrante;
import com.campusstudy.grupos.repository.GrupoRepository;
import com.campusstudy.grupos.service.GrupoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GrupoServiceImpl implements GrupoService {

    private final GrupoRepository grupoRepository;
    private final GrupoMapper grupoMapper;
    private final MateriasClient materiasClient;

    @Override
    @Transactional
    public GrupoResponse crearGrupo(CrearGrupoRequest request, Long usuarioId) {
        String nombre = request.nombre().trim();

        // 1. La materia debe existir y ser del usuario (pregunta a materias-service) → 400 / 503
        if (!materiasClient.perteneceAlUsuario(request.materiaId())) {
            throw new BadRequestException("materia_id", "La materia no existe o no te pertenece");
        }

        // 2. No repetir nombre dentro de la misma materia → 409
        if (grupoRepository.existsByNombreIgnoreCaseAndMateriaId(nombre, request.materiaId())) {
            throw new ConflictException(
                    "Ya existe un grupo llamado '" + nombre + "' en esta materia");
        }

        // 3. Request + usuario → entidad
        Grupo grupo = grupoMapper.toEntity(request, usuarioId);

        // 4. El creador entra como LIDER
        grupo.agregarIntegrante(Integrante.builder()
                .usuarioId(usuarioId)
                .rol(RolIntegrante.LIDER)
                .build());

        // 5. Guarda grupo + integrante en cascada
        Grupo guardado = grupoRepository.save(grupo);
        log.info("Grupo {} creado por el usuario {} en la materia {}",
                guardado.getId(), usuarioId, guardado.getMateriaId());

        return grupoMapper.toResponse(guardado);
    }

    @Override
    public List<GrupoResponse> listarMisGrupos(Long usuarioId) {
        return grupoMapper.toResponseList(grupoRepository.findAllByIntegrante(usuarioId));
    }

    @Override
    public GrupoResponse obtenerGrupo(Long grupoId, Long usuarioId) {
        Grupo grupo = grupoRepository.findByIdAndIntegrante(grupoId, usuarioId)
                .orElseThrow(() -> new NotFoundException("El grupo no existe"));

        return grupoMapper.toResponse(grupo);
    }
}