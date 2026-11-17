package com.upc.numerix.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
/*
 Compra a crédito con uno o varios productos:
 - "detalles": [{"idProducto": "PRD-00001", "cantidadProducto": 2}, …]  (varios productos)
 - "idProducto" + "cantidadProducto"                                    (un solo producto)
 */
public class CompraRequestDTO {
    @NotBlank(message = "Seleccione el cliente.")
    private String idCliente;

    @Valid
    private List<DetalleCompraRequestDTO> detalles = new ArrayList<>();

    // Atajo para una compra de un solo producto (se usa si "detalles" viene vacío)
    private String idProducto;

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

    // Productos de la compra, en cualquiera de las dos formas
    public List<DetalleCompraRequestDTO> lineas() {
        if (detalles != null && !detalles.isEmpty()) {
            return detalles;
        }
        if (idProducto != null && !idProducto.isBlank()) {
            return List.of(new DetalleCompraRequestDTO(idProducto, cantidadProducto == null ? 1 : cantidadProducto));
        }
        return List.of();
    }
}
