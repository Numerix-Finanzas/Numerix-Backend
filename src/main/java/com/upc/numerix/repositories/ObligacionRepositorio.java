package com.upc.numerix.repositories;

import com.upc.numerix.entities.Obligacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ObligacionRepositorio extends JpaRepository<Obligacion, String> {
    List<Obligacion> findByCompraClienteIdClienteOrderByCompraFechaCompraAscCompraIdCompraAsc(String idCliente);
}
