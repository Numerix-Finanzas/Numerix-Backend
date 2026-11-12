package com.upc.products.dtos;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRequestDTO {
    @NotBlank(message = "La descripción del producto es obligatoria.")
    @Size(max = 150)
    private String descripcionProducto;

    @Size(max = 50)
    private String marcaProducto;

    @Size(max = 100)
    private String proveedorProducto;

    @NotBlank(message = "La unidad de medida es obligatoria.")
    @Size(max = 20)
    private String unidadMedida = "Unidad";

    @Size(max = 255)
    @Pattern(regexp = "^$|(?i).*\\.(jpg|jpeg|png)$", message = "Formato de imagen no válido. Use .jpg o .png")
    private String imagenProducto;

    @NotNull(message = "El precio contado es obligatorio.")
    @DecimalMin(value = "0.00", inclusive = false, message = "El precio contado debe ser mayor a 0.")
    @Digits(integer = 16, fraction = 2)
    private BigDecimal precioContado;

    @NotNull(message = "El precio crédito es obligatorio.")
    @DecimalMin(value = "0.00", inclusive = false, message = "El precio crédito debe ser mayor a 0.")
    @Digits(integer = 16, fraction = 2)
    private BigDecimal precioCredito;

    @NotNull(message = "La modalidad de venta es obligatoria.")
    @Pattern(regexp = "Contado|Crédito|Ambas", message = "La modalidad de venta solo puede ser 'Contado', 'Crédito' o 'Ambas'.")
    private String modalidadVentaProducto;

    @Pattern(regexp = "Activo|Inactivo", message = "El estado solo puede ser 'Activo' o 'Inactivo'.")
    private String estadoProducto = "Activo";
}
