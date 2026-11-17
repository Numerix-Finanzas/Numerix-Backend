package com.upc.numerix.security.services;

import com.upc.numerix.entities.Cliente;
import com.upc.numerix.entities.Negocio;
import com.upc.numerix.exceptions.RecursoNoEncontradoException;
import com.upc.numerix.repositories.ClienteRepositorio;
import com.upc.numerix.security.entities.Usuario;
import com.upc.numerix.security.repositories.UsuarioRepositorio;
import com.upc.numerix.util.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/*
 Resuelve quién hace la petición a partir del token y restringe los datos que puede ver:
 un AdminNegocio solo accede a los clientes y productos de su negocio, y un Cliente solo a lo suyo.
 */
@Service
public class AccesoService {

    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    @Autowired
    private ClienteRepositorio clienteRepositorio;

    public Usuario usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AccessDeniedException("Sin autenticación");
        }
        return usuarioRepositorio.findByUsuario(auth.getName())
                .orElseThrow(() -> new AccessDeniedException("Usuario no encontrado"));
    }

    // Negocio del AdminNegocio que hace la petición
    public Negocio negocioActual() {
        Usuario usuario = usuarioActual();
        if (!Constantes.ROL_ADMIN_NEGOCIO.equals(usuario.getRolUsuario()) || usuario.getNegocio() == null) {
            throw new AccessDeniedException("La operación es solo para administradores de negocio");
        }
        return usuario.getNegocio();
    }

    // Cliente al que el usuario actual tiene acceso (el suyo si es Cliente, o uno de su negocio si es AdminNegocio)
    public Cliente clienteAccesible(String idCliente) {
        Cliente cliente = clienteRepositorio.findById(idCliente)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + idCliente));
        Usuario usuario = usuarioActual();
        boolean permitido = switch (usuario.getRolUsuario()) {
            case Constantes.ROL_ADMIN_NEGOCIO -> usuario.getNegocio() != null
                    && usuario.getNegocio().getIdNegocio().equals(cliente.getNegocio().getIdNegocio());
            case Constantes.ROL_CLIENTE -> usuario.getCliente() != null
                    && usuario.getCliente().getIdCliente().equals(cliente.getIdCliente());
            default -> false;
        };
        if (!permitido) {
            throw new AccessDeniedException("Sin acceso al cliente " + idCliente);
        }
        return cliente;
    }
}
