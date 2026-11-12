package com.upc.numerix.security.dtos;

import lombok.Data;

import java.util.Set;

@Data
public class AuthResponseDTO {
    private boolean autenticacionValida;
    private String jwt;
    private Set<String> roles;
    private String rolUsuario;
    private String usuario;
    private String nombreCompleto;
    private String idNegocio;
    private String nombreNegocio;
    private String idCliente;
    private boolean recordarContrasena;
}
