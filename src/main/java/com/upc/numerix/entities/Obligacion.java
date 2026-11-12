package com.upc.products.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/*
 Obligación de pago que nace de una compra al crédito. Guarda una copia de las condiciones
 de tasa del cliente al momento de la compra, para que un cambio posterior en las condiciones
 del cliente no altere las deudas ya contraídas.
 Una compra a un solo pago tiene una sola cuota (vence en la fecha de pago pactada);
 una compra en cuotas tiene el cronograma del método francés.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "obligacion")
public class Obligacion {
    @Id
    @Column(name = "id_obligacion", length = 20)
    private String idObligacion;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_compra", unique = true)
    private Compra compra;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "tipo_tasa", nullable = false, length = 10)
    private String tipoTasa;

    @Column(name = "porcentaje_tasa", nullable = false, precision = 18, scale = 7)
    private BigDecimal porcentajeTasa;

    @Column(name = "periodo_tasa", nullable = false, length = 15)
    private String periodoTasa;

    @Column(name = "periodo_capitalizacion", nullable = false)
    private Integer periodoCapitalizacion;

    @Column(name = "tipo_tasa_moratoria", nullable = false, length = 10)
    private String tipoTasaMoratoria;

    @Column(name = "porcentaje_tasa_moratoria", nullable = false, precision = 18, scale = 7)
    private BigDecimal porcentajeTasaMoratoria;

    @Column(name = "capital_inicial", nullable = false, precision = 18, scale = 2)
    private BigDecimal capitalInicial;

    // Capital después de capitalizar los días de gracia (igual al inicial en un solo pago)
    @Column(name = "capital_actualizado", nullable = false, precision = 18, scale = 2)
    private BigDecimal capitalActualizado;

    @Column(name = "dias_gracia", nullable = false)
    private Integer diasGracia;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @OneToMany(mappedBy = "obligacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroCuota ASC")
    private List<Cuota> cuotas = new ArrayList<>();
}
