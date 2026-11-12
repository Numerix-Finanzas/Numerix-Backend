package com.upc.numerix.security.services;

import com.upc.numerix.security.entities.Usuario;
import com.upc.numerix.security.repositories.UsuarioRepositorio;
import com.upc.numerix.util.Constantes;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Busca la cuenta por su usuario y la convierte en un UserDetails para Spring Security.
 * El rolUsuario se publica como authority ROLE_AdminSistema, ROLE_AdminNegocio o ROLE_Cliente,
 * de modo que en los controladores se usa @PreAuthorize("hasRole('AdminNegocio')").
 * La cuenta queda deshabilitada si está inactiva o si su negocio está inactivo.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepositorio usuarioRepositorio;

    public CustomUserDetailsService(UsuarioRepositorio usuarioRepositorio) {
        this.usuarioRepositorio = usuarioRepositorio;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepositorio.findByUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean negocioActivo = usuario.getNegocio() == null
                || Constantes.ACTIVO.equals(usuario.getNegocio().getEstadoNegocio());

        return org.springframework.security.core.userdetails.User
                .withUsername(usuario.getUsuario())
                .password(usuario.getContrasena())
                .authorities(new SimpleGrantedAuthority("ROLE_" + usuario.getRolUsuario()))
                .disabled(!usuario.isActivo() || !negocioActivo)
                .build();
    }
}
