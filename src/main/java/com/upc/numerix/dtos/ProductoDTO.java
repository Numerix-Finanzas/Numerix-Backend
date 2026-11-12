package com.upc.numerix.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDTO {
    private String idProducto;
    private String descripcionProducto;
    private String marcaProducto;
    private String proveedorProducto;
    private String unidadMedida;
    private String imagenProducto;
    private BigDecimal precioContado;
    private BigDecimal precioCredito;
    private String modalidadVentaProducto;
    private String estadoProducto;
    private LocalDateTime fechaRegistro;
}
