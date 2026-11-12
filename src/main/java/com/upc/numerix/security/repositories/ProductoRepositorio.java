package com.upc.numerix.repositories;

import com.upc.numerix.entities.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepositorio extends JpaRepository<Producto, String> {
    List<Producto> findByNegocioIdNegocioOrderByDescripcionProductoAsc(String idNegocio);
}
