package com.upc.products.dtos;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NegocioRequestDTO {
    @NotBlank(message = "El nombre del negocio es obligatorio.")
    @Size(max = 100)
    private String nombreNegocio;

    @NotBlank(message = "El RUC es obligatorio.")
    @Pattern(regexp = "^\\d{11}$", message = "RUC no válido: debe tener 11 dígitos numéricos.")
    private String rucNegocio;

    @NotBlank(message = "El tipo de negocio es obligatorio.")
    @Size(max = 60)
    private String tipoNegocio;

    @NotBlank(message = "El nombre del administrador es obligatorio.")
    @Size(max = 100)
    private String nombreAdministradorNegocio;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Correo no válido (ejemplo: nombre@dominio.com).")
    @Size(max = 100)
    private String correoElectronico;

    @NotBlank(message = "El usuario es obligatorio.")
    @Size(max = 20, message = "El usuario admite como máximo 20 caracteres.")
    private String usuario;

    @NotNull(message = "La contraseña es obligatoria.")
    @Size(min = 8, max = 64, message = "La contraseña debe tener mínimo 8 caracteres.")
    private String contrasena;
}
