package com.upc.numerix.services;

import com.upc.numerix.dtos.ClienteDTO;
import com.upc.numerix.dtos.ClienteRegistradoDTO;
import com.upc.numerix.dtos.ClienteRequestDTO;

import java.util.List;

public interface ClienteService {
    public List<ClienteDTO> listarClientes();
    public ClienteDTO buscarCliente(String idCliente);
    public ClienteRegistradoDTO registrarCliente(ClienteRequestDTO request);
    public ClienteDTO modificarCondiciones(String idCliente, ClienteRequestDTO request);
    public ClienteDTO cambiarEstado(String idCliente, String estado);
}
