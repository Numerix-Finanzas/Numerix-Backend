package com.upc.numerix.serviceimpl;

import com.upc.numerix.dtos.DashboardAdminDTO;
import com.upc.numerix.dtos.NegocioDTO;
import com.upc.numerix.dtos.NegocioRequestDTO;
import com.upc.numerix.entities.Negocio;
import com.upc.numerix.exceptions.OperacionRechazadaException;
import com.upc.numerix.exceptions.RecursoNoEncontradoException;
import com.upc.numerix.repositories.NegocioRepositorio;
import com.upc.numerix.security.entities.Usuario;
import com.upc.numerix.security.entities.Rol;
import com.upc.numerix.security.repositories.RolRepositorio;
import com.upc.numerix.security.repositories.UsuarioRepositorio;
import com.upc.numerix.services.NegocioService;
import com.upc.numerix.util.Constantes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NegocioServiceImpl implements NegocioService {

    @Autowired
    private NegocioRepositorio negocioRepositorio;
    @Autowired
    private UsuarioRepositorio usuarioRepositorio;
    @Autowired
    private RolRepositorio rolRepositorio;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public DashboardAdminDTO dashboard() {
        return new DashboardAdminDTO(negocioRepositorio.count(),
                negocioRepositorio.countByEstadoNegocio(Constantes.ACTIVO),
                negocioRepositorio.countByEstadoNegocio(Constantes.INACTIVO));
    }

    @Override
    public List<NegocioDTO> listarNegocios() {
        return negocioRepositorio.findAllByOrderByFechaRegistroDesc().stream().map(this::aDTO).toList();
    }

    @Override
    public NegocioDTO buscarNegocio(String idNegocio) {
        return aDTO(obtener(idNegocio));
    }

    // El administrador del sistema registra el negocio y la cuenta de su administrador (rol AdminNegocio)
    @Transactional
    @Override
    public NegocioDTO registrarNegocio(NegocioRequestDTO request) {
        if (negocioRepositorio.existsByRucNegocio(request.getRucNegocio())) {
            throw new OperacionRechazadaException("Ya existe un negocio registrado con el RUC " + request.getRucNegocio() + ".");
        }
        if (usuarioRepositorio.existsByUsuario(request.getUsuario().trim())) {
            throw new OperacionRechazadaException("El usuario '" + request.getUsuario().trim() + "' ya está en uso.");
        }
        Negocio negocio = new Negocio();
        negocio.setIdNegocio("NEG-" + request.getRucNegocio());
        negocio.setNombreNegocio(request.getNombreNegocio().trim());
        negocio.setRucNegocio(request.getRucNegocio());
        negocio.setTipoNegocio(request.getTipoNegocio().trim());
        negocio.setEstadoNegocio(Constantes.ACTIVO);
        negocio.setFechaRegistro(LocalDateTime.now(Constantes.ZONA));
        negocio = negocioRepositorio.save(negocio);

        Usuario admin = new Usuario();
        admin.setUsuario(request.getUsuario().trim());
        admin.setContrasena(passwordEncoder.encode(request.getContrasena()));
        admin.setRol(rol(Constantes.ROL_ADMIN_NEGOCIO));
        admin.setNombreCompleto(request.getNombreAdministradorNegocio().trim());
        admin.setCorreoElectronico(request.getCorreoElectronico().trim());
        admin.setNegocio(negocio);
        admin.setActivo(true);
        admin.setFechaRegistro(LocalDateTime.now(Constantes.ZONA));
        usuarioRepositorio.save(admin);
        return aDTO(negocio);
    }

    @Transactional
    @Override
    public NegocioDTO cambiarEstado(String idNegocio, String estado) {
        Negocio negocio = obtener(idNegocio);
        negocio.setEstadoNegocio(estado);
        return aDTO(negocioRepositorio.save(negocio));
    }

    private Negocio obtener(String idNegocio) {
        return negocioRepositorio.findById(idNegocio)
                .orElseThrow(() -> new RecursoNoEncontradoException("Negocio no encontrado: " + idNegocio));
    }

    private NegocioDTO aDTO(Negocio negocio) {
        Usuario admin = usuarioRepositorio
                .findFirstByNegocioIdNegocioAndRolNombre(negocio.getIdNegocio(), Constantes.ROL_ADMIN_NEGOCIO)
                .orElse(null);
        return NegocioDTO.builder()
                .idNegocio(negocio.getIdNegocio())
                .nombreNegocio(negocio.getNombreNegocio())
                .rucNegocio(negocio.getRucNegocio())
                .tipoNegocio(negocio.getTipoNegocio())
                .estadoNegocio(negocio.getEstadoNegocio())
                .nombreAdministradorNegocio(admin != null ? admin.getNombreCompleto() : null)
                .correoElectronico(admin != null ? admin.getCorreoElectronico() : null)
                .usuario(admin != null ? admin.getUsuario() : null)
                .fechaRegistro(negocio.getFechaRegistro())
                .build();
    }

    private Rol rol(String nombre) {
        return rolRepositorio.findByNombre(nombre)
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + nombre + " en la tabla rol (import.sql)"));
    }
}
