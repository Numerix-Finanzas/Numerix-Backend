package com.upc.numerix.repositories;

import com.upc.numerix.entities.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CompraRepositorio extends JpaRepository<Compra, String> {
    List<Compra> findByClienteIdClienteOrderByFechaCompraAscIdCompraAsc(String idCliente);
    List<Compra> findByClienteIdClienteAndFechaCompraBetweenOrderByFechaCompraAscIdCompraAsc(
            String idCliente, LocalDate desde, LocalDate hasta);
}
