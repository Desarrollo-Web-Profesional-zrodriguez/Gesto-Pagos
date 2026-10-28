package com.proyecto.servicios.entity.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cat_nacionalidades")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Nacionalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nacionalidad")
    private Long idNacionalidad;

    @Column(name = "clave_iso", nullable = false, unique = true)
    private String claveIso;

    @Column(name = "pais", nullable = false)
    private String pais;

    @Column(name = "gentilicio", nullable = false)
    private String gentilicio;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
