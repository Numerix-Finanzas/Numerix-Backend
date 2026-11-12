package com.upc.numerix.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PagoRequestDTO {
    @NotNull(message = "Seleccione la obligación (cuota) a pagar.")
    private Long idCuota;

    // Si no se envía, se usa la fecha actual
    private LocalDate fechaPagoReal;

    @NotNull(message = "El monto pagado es obligatorio.")
    @DecimalMin(value = "0.00", inclusive = false, message = "El monto pagado debe ser mayor a 0.")
    @Digits(integer = 16, fraction = 2)
    private BigDecimal montoPagado;
}
