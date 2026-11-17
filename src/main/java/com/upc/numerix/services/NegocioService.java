package com.upc.numerix.services;

import com.upc.numerix.dtos.DashboardAdminDTO;
import com.upc.numerix.dtos.NegocioDTO;
import com.upc.numerix.dtos.NegocioRequestDTO;

import java.util.List;

public interface NegocioService {
    public DashboardAdminDTO dashboard();
    public List<NegocioDTO> listarNegocios();
    public NegocioDTO buscarNegocio(String idNegocio);
    public NegocioDTO registrarNegocio(NegocioRequestDTO request);
    public NegocioDTO cambiarEstado(String idNegocio, String estado);
}
