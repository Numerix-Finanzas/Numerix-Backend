package com.upc.numerix.repositories;

import com.upc.numerix.entities.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface PagoRepositorio extends JpaRepository<Pago, String> {
    List<Pago> findByObligacionCompraClienteIdClienteAndFechaPagoRealBetweenOrderByFechaPagoRealAscIdPagoAsc(
            String idCliente, LocalDate desde, LocalDate hasta);
}
