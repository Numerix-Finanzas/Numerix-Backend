package com.upc.numerix.security.controllers;

import com.upc.numerix.security.dtos.AuthRequestDTO;
import com.upc.numerix.security.dtos.AuthResponseDTO;
import com.upc.numerix.security.entities.Usuario;
import com.upc.numerix.security.repositories.UsuarioRepositorio;
import com.upc.numerix.security.util.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

// Inicio de sesión: valida usuario y contraseña y devuelve el token con el rol de la cuenta
@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepositorio usuarioRepositorio;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          UsuarioRepositorio usuarioRepositorio) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponseDTO> createAuthenticationToken(@RequestBody AuthRequestDTO authRequest) {
        String username = authRequest.getUsuario() == null ? "" : authRequest.getUsuario().trim();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, authRequest.getContrasena())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Usuario usuario = usuarioRepositorio.findByUsuario(userDetails.getUsername()).orElseThrow();
        final String token = jwtUtil.generateToken(userDetails, usuario.getRolUsuario(),
                authRequest.isRecordarContrasena());

        Set<String> roles = userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        AuthResponseDTO authResponseDTO = new AuthResponseDTO();
        authResponseDTO.setAutenticacionValida(true);
        authResponseDTO.setRecordarContrasena(authRequest.isRecordarContrasena());
        authResponseDTO.setJwt(token);
        authResponseDTO.setRoles(roles);
        authResponseDTO.setRolUsuario(usuario.getRolUsuario());
        authResponseDTO.setUsuario(usuario.getUsuario());
        authResponseDTO.setNombreCompleto(usuario.getNombreCompleto());
        if (usuario.getNegocio() != null) {
            authResponseDTO.setIdNegocio(usuario.getNegocio().getIdNegocio());
            authResponseDTO.setNombreNegocio(usuario.getNegocio().getNombreNegocio());
        }
        if (usuario.getCliente() != null) {
            authResponseDTO.setIdCliente(usuario.getCliente().getIdCliente());
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("Authorization", token);
        return ResponseEntity.ok().headers(responseHeaders).body(authResponseDTO);
    }
}
