package com.upc.products.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Resumen del Registro de pago: lo que el cliente debe pagar en la fecha real de pago
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagoSimulacionDTO {
    private Long idCuota;
    private String idObligacion;
    private String idCompra;
    private String descripcionProducto;
    private Integer numeroPeriodo;
    private String moneda;
    private LocalDate fechaPagoPactada;
    private LocalDate fechaPagoReal;
    private Integer diasMora;
    private Integer diasAnticipacion;
    private BigDecimal deudaVencida;
    private BigDecimal capitalPendienteDeuda;
    private BigDecimal interesCompensatorioDeuda;
    private BigDecimal interesMoratorioObligacion;
    private BigDecimal montoTotalObligacion;
    private String ordenPrelacion;
}
