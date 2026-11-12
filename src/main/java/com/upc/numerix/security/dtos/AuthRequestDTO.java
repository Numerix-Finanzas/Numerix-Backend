package com.upc.numerix.security.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos de entrada del inicio de sesión (usuario, contrasena, recordarContrasena)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequestDTO {
    private String usuario;
    private String contrasena;
    // Si es true, el token dura más (las credenciales se recuerdan en el dispositivo)
    private boolean recordarContrasena = false;
}
