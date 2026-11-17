package com.upc.numerix.controllers;

import com.upc.numerix.dtos.DashboardAdminDTO;
import com.upc.numerix.dtos.EstadoRequestDTO;
import com.upc.numerix.dtos.NegocioDTO;
import com.upc.numerix.dtos.NegocioRequestDTO;
import com.upc.numerix.services.NegocioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Módulo del administrador del sistema: dashboard y registro de negocios
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('AdminSistema')")
public class NegocioController {
    @Autowired
    private NegocioService negocioService;

    @GetMapping("/admin/dashboard")
    public DashboardAdminDTO dashboard() {
        return negocioService.dashboard();
    }

    @GetMapping("/negocios")
    public List<NegocioDTO> listarNegocios() {
        return negocioService.listarNegocios();
    }

    @GetMapping("/negocios/{idNegocio}")
    public NegocioDTO buscarNegocio(@PathVariable String idNegocio) {
        return negocioService.buscarNegocio(idNegocio);
    }

    @PostMapping("/negocios")
    public ResponseEntity<NegocioDTO> registrarNegocio(@Valid @RequestBody NegocioRequestDTO request) {
        return new ResponseEntity<>(negocioService.registrarNegocio(request), HttpStatus.CREATED);
    }

    @PutMapping("/negocios/{idNegocio}/estado")
    public NegocioDTO cambiarEstado(@PathVariable String idNegocio, @Valid @RequestBody EstadoRequestDTO request) {
        return negocioService.cambiarEstado(idNegocio, request.getEstado());
    }
}
