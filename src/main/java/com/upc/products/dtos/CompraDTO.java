package com.upc.products.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

// Fila de Movimientos - Compras
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompraDTO {
    private String idCompra;
    private String idObligacion;
    private LocalDate fechaCompra;
    private String descripcionProducto;
    private Integer cantidadProducto;
    private String tipoPago;
    private Integer numeroCuotas;
    private BigDecimal montoCompra;
    private String estadoDeuda;
}
