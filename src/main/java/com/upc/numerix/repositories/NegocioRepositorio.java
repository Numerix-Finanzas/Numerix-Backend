package com.upc.numerix.repositories;

import com.upc.numerix.entities.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NegocioRepositorio extends JpaRepository<Negocio, String> {
    boolean existsByRucNegocio(String rucNegocio);
    long countByEstadoNegocio(String estadoNegocio);
    List<Negocio> findAllByOrderByFechaRegistroDesc();
}
