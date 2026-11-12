package com.upc.products.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Fila del historial de deudas del Reporte de deuda
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeudaDTO {
    private LocalDate fechaCompra;
    private String idCompra;
    private String idObligacion;
    private Long idCuota;
    private Integer numeroPeriodo;
    private Integer numeroCuotas;
    private String descripcionProducto;
    private LocalDate fechaPagoPactada;
    private Integer diasMora;
    private BigDecimal capitalPendienteDeuda;
    private BigDecimal interesCompensatorioDeuda;
    private BigDecimal interesMoratorioDeuda;
    private BigDecimal montoTotalDeuda;
    private String estadoDeuda;
}
