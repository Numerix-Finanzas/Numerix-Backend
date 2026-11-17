package com.upc.numerix.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// Línea de la compra: producto, cantidad, precio crédito y subtotal
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompraDTO {
    private String idProducto;
    private String descripcionProducto;
    private Integer cantidadProducto;
    private BigDecimal precioCredito;
    private BigDecimal subtotal;
}
