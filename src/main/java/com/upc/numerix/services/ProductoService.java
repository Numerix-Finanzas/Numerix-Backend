package com.upc.numerix.services;

import com.upc.numerix.dtos.ProductoDTO;
import com.upc.numerix.dtos.ProductoRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductoService {
    public List<ProductoDTO> listarProductos();
    public ProductoDTO buscarProducto(String idProducto);
    public ProductoDTO registrarProducto(ProductoRequestDTO request);
    public ProductoDTO modificarProducto(String idProducto, ProductoRequestDTO request);
    public ProductoDTO cambiarEstado(String idProducto, String estado);
    public String guardarImagen(MultipartFile archivo);
}
