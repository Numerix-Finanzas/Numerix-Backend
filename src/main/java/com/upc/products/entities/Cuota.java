package com.upc.products.entities;

import com.upc.numerix.entities.Obligacion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cuota", uniqueConstraints = @UniqueConstraint(columnNames = {"id_obligacion", "numero_cuota"}))
public class Cuota {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuota")
    private Long idCuota;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_obligacion")
    private Obligacion obligacion;

    @Column(name = "numero_cuota", nullable = false)
    private Integer numeroCuota;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "saldo_inicial", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "interes_compensatorio", nullable = false, precision = 18, scale = 2)
    private BigDecimal interesCompensatorio;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amortizacion;

    @Column(name = "monto_cuota", nullable = false, precision = 18, scale = 2)
    private BigDecimal montoCuota;

    @Column(name = "saldo_final", nullable = false, precision = 18, scale = 2)
    private BigDecimal saldoFinal;

    @Column(nullable = false)
    private boolean pagada;

    // Fecha real en que se pagó (permite consultar la deuda a una fecha pasada)
    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    public boolean pagadaAl(LocalDate fecha) {
        return pagada && fechaPago != null && !fechaPago.isAfter(fecha);
    }
}
