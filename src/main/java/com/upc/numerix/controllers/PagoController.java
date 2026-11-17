package com.upc.numerix.controllers;

import com.upc.numerix.dtos.PagoDTO;
import com.upc.numerix.dtos.PagoRequestDTO;
import com.upc.numerix.dtos.PagoSimulacionDTO;
import com.upc.numerix.services.PagoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/pagos")
@PreAuthorize("hasRole('AdminNegocio')")
public class PagoController {
    @Autowired
    private PagoService pagoService;

    // Resumen del pago (días de mora, capital, intereses y total) para la fecha real de pago
    @GetMapping("/simular")
    public PagoSimulacionDTO simularPago(@RequestParam Long idCuota,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaPagoReal) {
        return pagoService.simularPago(idCuota, fechaPagoReal);
    }

    @PostMapping
    public ResponseEntity<PagoDTO> registrarPago(@Valid @RequestBody PagoRequestDTO request) {
        return new ResponseEntity<>(pagoService.registrarPago(request), HttpStatus.CREATED);
    }
}
