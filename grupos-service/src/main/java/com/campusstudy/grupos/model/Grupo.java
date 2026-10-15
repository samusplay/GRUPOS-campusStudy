package com.campusstudy.grupos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "grupos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_grupo_nombre_materia",
                columnNames = {"nombre", "materia_id"}),
        indexes = @Index(name = "idx_grupo_materia", columnList = "materia_id"))
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    // Solo el id: la materia vive en materias-service (sin FK)
    @Column(name = "materia_id", nullable = false)
    private Long materiaId;

    // usuario_id del creador, sacado del "sub" del JWT (sin FK)
    @Column(name = "creado_por", nullable = false, updatable = false)
    private Long creadoPor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "grupo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Integrante> integrantes = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (fechaCreacion == null) {
            // Hora Colombia (TZ del contenedor), sin nanosegundos: YYYY-MM-DDTHH:MM:SS
            fechaCreacion = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        }
    }

    // Mantiene los dos lados de la relación sincronizados
    public void agregarIntegrante(Integrante integrante) {
        integrantes.add(integrante);
        integrante.setGrupo(this);
    }
}