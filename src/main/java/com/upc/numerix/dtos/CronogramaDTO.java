package com.upc.numerix.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CronogramaDTO {
    private String idCompra;
    private String idObligacion;
    private String idCliente;
    private String nombreCliente;
    private String descripcionProducto;
    private String moneda;
    private LocalDate fechaCompra;
    private BigDecimal montoCompra;
    private String tipoPago;
    private Integer numeroCuotas;
    private Integer totalPeriodosAmortizacion;
    private BigDecimal tedCompensatoria;
    private BigDecimal temCompensatoria;
    private Integer diasGracia;
    private BigDecimal factorCapitalizacion;
    private BigDecimal capitalActualizado;
    private BigDecimal montoCuota;
    private Integer cuotasPagadas;
    private BigDecimal totalInteres;
    private BigDecimal totalAmortizacion;
    private BigDecimal totalCuotas;
    private LocalDate proximaFechaPago;
    private String estadoDeuda;
    private List<CuotaDTO> cuotas;
}
