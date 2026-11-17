package com.upc.numerix.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Respuesta del registro de cliente: muestra una sola vez las credenciales generadas
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteRegistradoDTO {
    private String mensaje;
    private ClienteDTO cliente;
    private String usuario;
    private String contrasena;
}
