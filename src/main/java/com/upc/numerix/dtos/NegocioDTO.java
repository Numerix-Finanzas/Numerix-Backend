package com.upc.numerix.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegocioDTO {
    private String idNegocio;
    private String nombreNegocio;
    private String rucNegocio;
    private String tipoNegocio;
    private String estadoNegocio;
    private String nombreAdministradorNegocio;
    private String correoElectronico;
    private String usuario;
    private LocalDateTime fechaRegistro;
}
