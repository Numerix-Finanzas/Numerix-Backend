package com.upc.numerix.services;

import com.upc.numerix.dtos.CompraRequestDTO;
import com.upc.numerix.dtos.CompraResultadoDTO;

public interface CompraService {
    public CompraResultadoDTO simularCompra(CompraRequestDTO request);
    public CompraResultadoDTO registrarCompra(CompraRequestDTO request);
}
