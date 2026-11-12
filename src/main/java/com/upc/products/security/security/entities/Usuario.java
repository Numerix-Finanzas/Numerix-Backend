package com.upc.numerix.security.entities;

import com.upc.numerix.entities.Cliente;
import com.upc.numerix.entities.Negocio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/*
 Cuenta de acceso a Numerix. Cada usuario tiene un rol:
 - AdminSistema: sin negocio ni cliente.
 - AdminNegocio: asociado a su negocio.
 - Cliente: asociado a su registro de cliente (usuario = número de documento).
 */
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario", nullable = false)
    private Long idUsuario;

    @Column(nullable = false, unique = true, length = 20)
    private String usuario;

    // Hash BCrypt (60 caracteres) de la contraseña
    @Column(nullable = false, length = 64)
    private String contrasena;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(name = "nombre_completo", length = 100)
    private String nombreCompleto;

    @Column(name = "correo_electronico", length = 100)
    private String correoElectronico;

    @ManyToOne
    @JoinColumn(name = "id_negocio")
    private Negocio negocio;

    @OneToOne
    @JoinColumn(name = "id_cliente", unique = true)
    private Cliente cliente;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    // rolUsuario de la tabla de datos: AdminSistema, AdminNegocio o Cliente
    public String getRolUsuario() {
        return rol.getNombre();
    }
}
