package com.upc.products.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EstadoRequestDTO {
    @NotNull(message = "El estado es obligatorio.")
    @Pattern(regexp = "Activo|Inactivo", message = "El estado solo puede ser 'Activo' o 'Inactivo'.")
    private String estado;
}
