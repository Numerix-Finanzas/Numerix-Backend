package com.upc.numerix.controllers;

import com.upc.numerix.dtos.*;
import com.upc.numerix.services.ClienteService;
import com.upc.numerix.services.ConsultaService;
import com.upc.numerix.services.PagoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/*
 Clientes del negocio y sus consultas. Las consultas también las puede usar el propio cliente
 (rol Cliente), pero solo sobre su registro: el servicio valida el acceso.
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {
    @Autowired
    private ClienteService clienteService;
    @Autowired
    private ConsultaService consultaService;
    @Autowired
    private PagoService pagoService;

    @GetMapping
    @PreAuthorize("hasRole('AdminNegocio')")
    public List<ClienteDTO> listarClientes() {
        return clienteService.listarClientes();
    }

    @GetMapping("/{idCliente}")
    @PreAuthorize("hasAnyRole('AdminNegocio', 'Cliente')")
    public ClienteDTO buscarCliente(@PathVariable String idCliente) {
        return clienteService.buscarCliente(idCliente);
    }

    @PostMapping
    @PreAuthorize("hasRole('AdminNegocio')")
    public ResponseEntity<ClienteRegistradoDTO> registrarCliente(@Valid @RequestBody ClienteRequestDTO request) {
        return new ResponseEntity<>(clienteService.registrarCliente(request), HttpStatus.CREATED);
    }

    @PutMapping("/{idCliente}")
    @PreAuthorize("hasRole('AdminNegocio')")
    public ClienteDTO modificarCondiciones(@PathVariable String idCliente, @Valid @RequestBody ClienteRequestDTO request) {
        return clienteService.modificarCondiciones(idCliente, request);
    }

    @PutMapping("/{idCliente}/estado")
    @PreAuthorize("hasRole('AdminNegocio')")
    public ClienteDTO cambiarEstado(@PathVariable String idCliente, @Valid @RequestBody EstadoRequestDTO request) {
        return clienteService.cambiarEstado(idCliente, request.getEstado());
    }

    // Pantalla Cronograma: cronogramas de las compras en cuotas del cliente y su próxima fecha de pago
    @GetMapping("/{idCliente}/cronograma")
    @PreAuthorize("hasAnyRole('AdminNegocio', 'Cliente')")
    public CronogramaClienteDTO cronograma(@PathVariable String idCliente) {
        return consultaService.cronogramaCliente(idCliente);
    }

    // Reporte de deuda; sin "fecha" se consulta a la fecha actual
    @GetMapping("/{idCliente}/reporte-deuda")
    @PreAuthorize("hasAnyRole('AdminNegocio', 'Cliente')")
    public ReporteDeudaDTO reporteDeuda(@PathVariable String idCliente,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return consultaService.reporteDeuda(idCliente, fecha);
    }

    @GetMapping("/{idCliente}/compras")
    @PreAuthorize("hasAnyRole('AdminNegocio', 'Cliente')")
    public List<CompraDTO> movimientosCompras(@PathVariable String idCliente,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicioConsulta,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFinConsulta) {
        return consultaService.movimientosCompras(idCliente, fechaInicioConsulta, fechaFinConsulta);
    }

    @GetMapping("/{idCliente}/pagos")
    @PreAuthorize("hasAnyRole('AdminNegocio', 'Cliente')")
    public List<PagoDTO> movimientosPagos(@PathVariable String idCliente,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicioConsulta,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFinConsulta) {
        return consultaService.movimientosPagos(idCliente, fechaInicioConsulta, fechaFinConsulta);
    }

    // Cuotas que se pueden pagar ahora (combo "Obligación" del Registro de pago)
    @GetMapping("/{idCliente}/obligaciones-pendientes")
    @PreAuthorize("hasAnyRole('AdminNegocio', 'Cliente')")
    public List<ObligacionPendienteDTO> obligacionesPendientes(@PathVariable String idCliente) {
        return pagoService.listarPendientes(idCliente);
    }
}
