package com.campusstudy.grupos.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(
        name = "integrantes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_integrante_grupo_usuario",
                columnNames = {"grupo_id", "usuario_id"}),
        indexes = @Index(name = "idx_integrante_usuario", columnList = "usuario_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Integrante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    // usuario de users-auth (sin FK)
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RolIntegrante rol;

    @Column(name = "unido_en", nullable = false, updatable = false)
    private LocalDateTime unidoEn;

    @PrePersist
    void prePersist() {
        if (unidoEn == null) {
            unidoEn = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        }
    }
}