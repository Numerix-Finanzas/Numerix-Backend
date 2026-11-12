package com.upc.products.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "negocio")
public class Negocio {
    @Id
    @Column(name = "id_negocio", length = 20)
    private String idNegocio;

    @Column(name = "nombre_negocio", nullable = false, length = 100)
    private String nombreNegocio;

    @Column(name = "ruc_negocio", nullable = false, unique = true, length = 11)
    private String rucNegocio;

    @Column(name = "tipo_negocio", length = 60)
    private String tipoNegocio;

    @Column(name = "estado_negocio", nullable = false, length = 10)
    private String estadoNegocio;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
