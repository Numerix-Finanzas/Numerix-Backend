package com.upc.numerix.repositories;

import com.upc.numerix.entities.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClienteRepositorio extends JpaRepository<Cliente, String> {
    boolean existsByNumeroDocumentoCliente(String numeroDocumentoCliente);
    List<Cliente> findByNegocioIdNegocioOrderByNombreClienteAsc(String idNegocio);
}
