package com.upc.products.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pago")
public class Pago {
    @Id
    @Column(name = "id_pago", length = 20)
    private String idPago;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_obligacion")
    private Obligacion obligacion;

    // Cuota que cancela este pago (en un solo pago es la cuota única)
    @OneToOne(optional = false)
    @JoinColumn(name = "id_cuota", unique = true)
    private Cuota cuota;

    @Column(name = "fecha_pago_real", nullable = false)
    private LocalDate fechaPagoReal;

    @Column(name = "monto_pagado", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoPagado;

    @Column(name = "monto_imputado_mora", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoImputadoMora;

    @Column(name = "monto_imputado_interes", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoImputadoInteres;

    @Column(name = "monto_imputado_capital", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoImputadoCapital;

    @Column(name = "dias_mora", nullable = false)
    private Integer diasMora;

    @Column(name = "dias_anticipacion", nullable = false)
    private Integer diasAnticipacion;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
