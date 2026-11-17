package com.upc.numerix.controllers;

import com.upc.numerix.dtos.EstadoRequestDTO;
import com.upc.numerix.dtos.ImagenDTO;
import com.upc.numerix.dtos.ProductoDTO;
import com.upc.numerix.dtos.ProductoRequestDTO;
import com.upc.numerix.services.ProductoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Catálogo de productos del negocio del usuario autenticado
@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('AdminNegocio')")
public class ProductoController {
    @Autowired
    private ProductoService productoService;

    @GetMapping("/productos")
    public List<ProductoDTO> listarProductos() {
        return productoService.listarProductos();
    }

    @GetMapping("/productos/{idProducto}")
    public ProductoDTO buscarProducto(@PathVariable String idProducto) {
        return productoService.buscarProducto(idProducto);
    }

    @PostMapping("/productos")
    public ResponseEntity<ProductoDTO> registrarProducto(@Valid @RequestBody ProductoRequestDTO request) {
        return new ResponseEntity<>(productoService.registrarProducto(request), HttpStatus.CREATED);
    }

    @PutMapping("/productos/{idProducto}")
    public ProductoDTO modificarProducto(@PathVariable String idProducto, @Valid @RequestBody ProductoRequestDTO request) {
        return productoService.modificarProducto(idProducto, request);
    }

    @PutMapping("/productos/{idProducto}/estado")
    public ProductoDTO cambiarEstado(@PathVariable String idProducto, @Valid @RequestBody EstadoRequestDTO request) {
        return productoService.cambiarEstado(idProducto, request.getEstado());
    }

    // Sube la imagen (multipart, campo "archivo") y devuelve la ruta para guardarla en imagenProducto
    @PostMapping(value = "/productos/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImagenDTO subirImagen(@RequestParam("archivo") MultipartFile archivo) {
        return new ImagenDTO(productoService.guardarImagen(archivo));
    }
}
