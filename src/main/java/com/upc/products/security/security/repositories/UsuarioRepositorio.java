package com.upc.numerix.security.repositories;

import com.upc.numerix.security.entities.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuario(String usuario);
    boolean existsByUsuario(String usuario);
    Optional<Usuario> findFirstByNegocioIdNegocioAndRolNombre(String idNegocio, String nombreRol);
}
