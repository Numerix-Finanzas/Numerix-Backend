package com.upc.numerix.dtos;

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
public class ClienteRequestDTO {
    @NotBlank(message = "El nombre del cliente es obligatorio.")
    @Size(max = 100)
    private String nombreCliente;

    // Valores por defecto según la tabla de datos de entrada (apartado 6.1.1)
    @NotNull(message = "El tipo de documento es obligatorio.")
    @Pattern(regexp = "DNI|CE|RUC", message = "El tipo de documento solo puede ser DNI, CE o RUC.")
    private String tipoDocumentoCliente = "DNI";

    @NotBlank(message = "El número de documento es obligatorio.")
    @Size(max = 12)
    private String numeroDocumentoCliente;

    // Condiciones de crédito
    @NotNull(message = "La moneda es obligatoria.")
    @Pattern(regexp = "PEN|USD", message = "La moneda solo puede ser PEN o USD.")
    private String moneda = "PEN";

    @NotNull(message = "El límite de crédito es obligatorio.")
    @DecimalMin(value = "0.00", inclusive = false, message = "El límite de crédito debe ser mayor a 0.")
    @Digits(integer = 16, fraction = 2)
    private BigDecimal limiteCredito;

    @NotNull(message = "El tipo de tasa es obligatorio.")
    @Pattern(regexp = "Efectiva|Nominal", message = "El tipo de tasa solo puede ser 'Efectiva' o 'Nominal'.")
    private String tipoTasa = "Efectiva";

    @NotNull(message = "El porcentaje de tasa es obligatorio.")
    @DecimalMin(value = "0", inclusive = false, message = "La tasa compensatoria debe ser mayor a 0.")
    @Digits(integer = 11, fraction = 7)
    private BigDecimal porcentajeTasa;

    @NotNull(message = "El tipo de tasa moratoria es obligatorio.")
    @Pattern(regexp = "Efectiva|Nominal", message = "El tipo de tasa moratoria solo puede ser 'Efectiva' o 'Nominal'.")
    private String tipoTasaMoratoria = "Efectiva";

    @NotNull(message = "El porcentaje de tasa moratoria es obligatorio.")
    @DecimalMin(value = "0", inclusive = false, message = "La tasa moratoria debe ser mayor a 0.")
    @Digits(integer = 11, fraction = 7)
    private BigDecimal porcentajeTasaMoratoria;

    @NotNull(message = "El periodo de la tasa es obligatorio.")
    @Pattern(regexp = "Diario|Mensual|Trimestral|Semestral|Anual",
            message = "El periodo de la tasa solo puede ser Diario, Mensual, Trimestral, Semestral o Anual.")
    private String periodoTasa = "Mensual";

    // Días entre capitalizaciones; se usa si alguna tasa es nominal
    @NotNull(message = "El periodo de capitalización es obligatorio.")
    @Min(value = 1, message = "El periodo de capitalización debe ser mayor a 0.")
    private Integer periodoCapitalizacion = 30;

    @NotNull(message = "El día de corte es obligatorio.")
    @Min(value = 1, message = "El día de corte debe estar entre 1 y 30.")
    @Max(value = 30, message = "El día de corte debe estar entre 1 y 30.")
    private Integer diaCorte = 25;

    @NotNull(message = "El día de pago es obligatorio.")
    @Min(value = 1, message = "El día de pago debe estar entre 1 y 30.")
    @Max(value = 30, message = "El día de pago debe estar entre 1 y 30.")
    private Integer diaPagoPactado = 30;

    @NotNull(message = "El máximo de meses de crédito es obligatorio.")
    @Min(value = 1, message = "El máximo de meses de crédito debe ser mayor a 0.")
    private Integer maximoMesesCredito = 1;
}
