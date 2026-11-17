package com.upc.numerix.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompraRequestDTO {
    @NotBlank(message = "Seleccione el producto.")
    private String idProducto;

    @NotNull(message = "La cantidad es obligatoria.")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0.")
    private Integer cantidadProducto = 1;
}
