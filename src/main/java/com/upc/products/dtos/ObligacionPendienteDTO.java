package com.upc.products.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Cuota pendiente que se selecciona en la pantalla Registro de pago
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObligacionPendienteDTO {
    private Long idCuota;
    private String idObligacion;
    private String idCompra;
    private String descripcionProducto;
    private LocalDate fechaCompra;
    private String tipoPago;
    private Integer numeroPeriodo;
    private Integer numeroCuotas;
    private LocalDate fechaPagoPactada;
    private BigDecimal deudaVencida;
    private String estadoCuota;
}
