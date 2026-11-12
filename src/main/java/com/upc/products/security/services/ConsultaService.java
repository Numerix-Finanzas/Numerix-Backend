package com.upc.numerix.services;

import com.upc.numerix.dtos.CompraDTO;
import com.upc.numerix.dtos.CronogramaClienteDTO;
import com.upc.numerix.dtos.CronogramaDTO;
import com.upc.numerix.dtos.PagoDTO;
import com.upc.numerix.dtos.ReporteDeudaDTO;

import java.time.LocalDate;
import java.util.List;

public interface ConsultaService {
    public CronogramaDTO cronograma(String idCompra);
    public CronogramaClienteDTO cronogramaCliente(String idCliente);
    public ReporteDeudaDTO reporteDeuda(String idCliente, LocalDate fechaConsulta);
    public List<CompraDTO> movimientosCompras(String idCliente, LocalDate fechaInicioConsulta, LocalDate fechaFinConsulta);
    public List<PagoDTO> movimientosPagos(String idCliente, LocalDate fechaInicioConsulta, LocalDate fechaFinConsulta);
}
