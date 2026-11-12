package com.upc.numerix.dtos;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompraRequestDTO {
    @NotBlank(message = "Seleccione el cliente.")
    private String idCliente;

    @NotBlank(message = "Seleccione el producto.")
    private String idProducto;

    @NotNull(message = "La cantidad es obligatoria.")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0.")
    private Integer cantidadProducto = 1;

    @NotNull(message = "El tipo de pago es obligatorio.")
    @Pattern(regexp = "Un solo pago|Cuotas", message = "El tipo de pago solo puede ser 'Un solo pago' o 'Cuotas'.")
    private String tipoPago = "Un solo pago";

    // Solo se usa con tipoPago = Cuotas (en un solo pago siempre es 1)
    @NotNull(message = "El número de cuotas es obligatorio.")
    @Min(value = 1, message = "El número de cuotas debe ser mayor a 0.")
    private Integer numeroCuotas = 1;

    // Si no se envía, se usa la fecha actual
    private LocalDate fechaCompra;
}
