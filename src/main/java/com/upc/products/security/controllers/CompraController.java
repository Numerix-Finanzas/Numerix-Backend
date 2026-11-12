package com.upc.numerix.controllers;

import com.upc.numerix.dtos.CompraRequestDTO;
import com.upc.numerix.dtos.CompraResultadoDTO;
import com.upc.numerix.dtos.CronogramaDTO;
import com.upc.numerix.services.CompraService;
import com.upc.numerix.services.ConsultaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compras")
public class CompraController {
    @Autowired
    private CompraService compraService;
    @Autowired
    private ConsultaService consultaService;

    // Calcula el resumen (crédito disponible, intereses, cuota estimada) sin registrar la compra
    @PostMapping("/simular")
    @PreAuthorize("hasRole('AdminNegocio')")
    public CompraResultadoDTO simularCompra(@Valid @RequestBody CompraRequestDTO request) {
        return compraService.simularCompra(request);
    }

    @PostMapping
    @PreAuthorize("hasRole('AdminNegocio')")
    public ResponseEntity<CompraResultadoDTO> registrarCompra(@Valid @RequestBody CompraRequestDTO request) {
        return new ResponseEntity<>(compraService.registrarCompra(request), HttpStatus.CREATED);
    }

    @GetMapping("/{idCompra}/cronograma")
    @PreAuthorize("hasAnyRole('AdminNegocio', 'Cliente')")
    public CronogramaDTO cronograma(@PathVariable String idCompra) {
        return consultaService.cronograma(idCompra);
    }
}
