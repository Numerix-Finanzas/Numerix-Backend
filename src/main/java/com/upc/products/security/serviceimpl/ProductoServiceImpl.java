package com.upc.numerix.serviceimpl;

import com.upc.numerix.dtos.ProductoDTO;
import com.upc.numerix.dtos.ProductoRequestDTO;
import com.upc.numerix.entities.Negocio;
import com.upc.numerix.entities.Producto;
import com.upc.numerix.exceptions.OperacionRechazadaException;
import com.upc.numerix.exceptions.RecursoNoEncontradoException;
import com.upc.numerix.repositories.ProductoRepositorio;
import com.upc.numerix.security.services.AccesoService;
import com.upc.numerix.services.ProductoService;
import com.upc.numerix.util.Constantes;
import com.upc.numerix.util.GeneradorId;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProductoServiceImpl implements ProductoService {

    @Autowired
    private ProductoRepositorio productoRepositorio;
    @Autowired
    private AccesoService accesoService;
    @Autowired
    private ModelMapper modelMapper;

    @Value("${numerix.uploads-dir}")
    private String uploadsDir;

    @Override
    public List<ProductoDTO> listarProductos() {
        Negocio negocio = accesoService.negocioActual();
        return productoRepositorio.findByNegocioIdNegocioOrderByDescripcionProductoAsc(negocio.getIdNegocio())
                .stream().map(p -> modelMapper.map(p, ProductoDTO.class)).toList();
    }

    @Override
    public ProductoDTO buscarProducto(String idProducto) {
        return modelMapper.map(obtener(idProducto), ProductoDTO.class);
    }

    @Transactional
    @Override
    public ProductoDTO registrarProducto(ProductoRequestDTO request) {
        validarPrecios(request);
        Producto producto = new Producto();
        producto.setIdProducto(GeneradorId.siguiente("PRD-", 5, productoRepositorio));
        producto.setNegocio(accesoService.negocioActual());
        producto.setFechaRegistro(LocalDateTime.now(Constantes.ZONA));
        copiar(request, producto);
        return modelMapper.map(productoRepositorio.save(producto), ProductoDTO.class);
    }

    @Transactional
    @Override
    public ProductoDTO modificarProducto(String idProducto, ProductoRequestDTO request) {
        validarPrecios(request);
        Producto producto = obtener(idProducto);
        copiar(request, producto);
        return modelMapper.map(productoRepositorio.save(producto), ProductoDTO.class);
    }

    @Transactional
    @Override
    public ProductoDTO cambiarEstado(String idProducto, String estado) {
        Producto producto = obtener(idProducto);
        producto.setEstadoProducto(estado);
        return modelMapper.map(productoRepositorio.save(producto), ProductoDTO.class);
    }

    // Guarda la imagen en la carpeta de uploads y devuelve la ruta pública (/uploads/productos/...)
    @Override
    public String guardarImagen(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        String extension = nombre.endsWith(".png") ? ".png"
                : (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg")) ? ".jpg" : null;
        if (archivo.isEmpty() || extension == null) {
            throw new OperacionRechazadaException("Formato de imagen no válido. Use .jpg o .png");
        }
        String nombreArchivo = UUID.randomUUID() + extension;
        try (InputStream entrada = archivo.getInputStream()) {
            Path carpeta = Path.of(uploadsDir, "productos");
            Files.createDirectories(carpeta);
            Files.copy(entrada, carpeta.resolve(nombreArchivo), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new OperacionRechazadaException("No se pudo guardar la imagen.");
        }
        return "/uploads/productos/" + nombreArchivo;
    }

    // Solo devuelve productos del negocio del usuario actual
    private Producto obtener(String idProducto) {
        Negocio negocio = accesoService.negocioActual();
        return productoRepositorio.findById(idProducto)
                .filter(p -> p.getNegocio().getIdNegocio().equals(negocio.getIdNegocio()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + idProducto));
    }

    private void validarPrecios(ProductoRequestDTO request) {
        if (request.getPrecioCredito().compareTo(request.getPrecioContado()) < 0) {
            throw new OperacionRechazadaException("El precio crédito no puede ser menor al precio contado (S/ "
                    + request.getPrecioContado() + ").");
        }
    }

    private void copiar(ProductoRequestDTO request, Producto producto) {
        producto.setDescripcionProducto(request.getDescripcionProducto().trim());
        producto.setMarcaProducto(request.getMarcaProducto());
        producto.setProveedorProducto(request.getProveedorProducto());
        producto.setUnidadMedida(request.getUnidadMedida().trim());
        producto.setImagenProducto(request.getImagenProducto());
        producto.setPrecioContado(request.getPrecioContado());
        producto.setPrecioCredito(request.getPrecioCredito());
        producto.setModalidadVentaProducto(request.getModalidadVentaProducto());
        producto.setEstadoProducto(request.getEstadoProducto() == null ? Constantes.ACTIVO : request.getEstadoProducto());
    }
}
