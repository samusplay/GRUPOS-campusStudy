package com.campusstudy.grupos.repository;

import com.campusstudy.grupos.model.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GrupoRepository extends JpaRepository<Grupo, Long> {

    // POST: ¿ya existe un grupo con ese nombre en esa materia? → 409
    boolean existsByNombreIgnoreCaseAndMateriaId(String nombre, Long materiaId);

    // GET /grupos: los grupos donde el usuario es integrante, con TODOS sus integrantes
    @Query("""
            SELECT DISTINCT g FROM Grupo g
            LEFT JOIN FETCH g.integrantes
            WHERE g.id IN (
                SELECT i.grupo.id FROM Integrante i WHERE i.usuarioId = :usuarioId
            )
            ORDER BY g.fechaCreacion DESC
            """)
    List<Grupo> findAllByIntegrante(@Param("usuarioId") Long usuarioId);

    // GET /grupos/{id}: el grupo SOLO si el usuario pertenece a él
    @Query("""
            SELECT g FROM Grupo g
            LEFT JOIN FETCH g.integrantes
            WHERE g.id = :grupoId
              AND EXISTS (
                SELECT 1 FROM Integrante i
                WHERE i.grupo.id = :grupoId AND i.usuarioId = :usuarioId
              )
            """)
    Optional<Grupo> findByIdAndIntegrante(@Param("grupoId") Long grupoId,
                                          @Param("usuarioId") Long usuarioId);
}