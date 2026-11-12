package com.upc.numerix.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Condiciones de crédito que el negocio otorga a un cliente (una por cliente)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "condicion_credito")
public class CondicionCredito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_condicion_credito")
    private Long idCondicionCredito;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_cliente", nullable = false, unique = true)
    private Cliente cliente;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "limite_credito", nullable = false, precision = 18, scale = 2)
    private BigDecimal limiteCredito;

    @Column(name = "tipo_tasa", nullable = false, length = 10)
    private String tipoTasa;

    @Column(name = "porcentaje_tasa", nullable = false, precision = 18, scale = 7)
    private BigDecimal porcentajeTasa;

    @Column(name = "tipo_tasa_moratoria", nullable = false, length = 10)
    private String tipoTasaMoratoria;

    @Column(name = "porcentaje_tasa_moratoria", nullable = false, precision = 18, scale = 7)
    private BigDecimal porcentajeTasaMoratoria;

    @Column(name = "periodo_tasa", nullable = false, length = 15)
    private String periodoTasa;

    @Column(name = "periodo_capitalizacion", nullable = false)
    private Integer periodoCapitalizacion;

    @Column(name = "dia_corte", nullable = false)
    private Integer diaCorte;

    @Column(name = "dia_pago_pactado", nullable = false)
    private Integer diaPagoPactado;

    @Column(name = "maximo_meses_credito", nullable = false)
    private Integer maximoMesesCredito;
}
