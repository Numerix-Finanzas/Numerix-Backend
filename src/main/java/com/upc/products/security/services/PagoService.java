package com.upc.numerix.services;

import com.upc.numerix.dtos.ObligacionPendienteDTO;
import com.upc.numerix.dtos.PagoDTO;
import com.upc.numerix.dtos.PagoRequestDTO;
import com.upc.numerix.dtos.PagoSimulacionDTO;

import java.time.LocalDate;
import java.util.List;

public interface PagoService {
    public List<ObligacionPendienteDTO> listarPendientes(String idCliente);
    public PagoSimulacionDTO simularPago(Long idCuota, LocalDate fechaPagoReal);
    public PagoDTO registrarPago(PagoRequestDTO request);
}
