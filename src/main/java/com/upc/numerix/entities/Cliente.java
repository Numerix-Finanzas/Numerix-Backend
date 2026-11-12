package com.upc.products.entities;

import com.upc.numerix.entities.CondicionCredito;
import com.upc.numerix.entities.Negocio;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

// Cliente de un negocio. Sus condiciones de crédito están en la tabla condicion_credito
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cliente")
public class Cliente {
    @Id
    @Column(name = "id_cliente", length = 20)
    private String idCliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_negocio")
    private Negocio negocio;

    @Column(name = "nombre_cliente", nullable = false, length = 100)
    private String nombreCliente;

    @Column(name = "tipo_documento_cliente", nullable = false, length = 10)
    private String tipoDocumentoCliente;

    @Column(name = "numero_documento_cliente", nullable = false, unique = true, length = 12)
    private String numeroDocumentoCliente;

    @Column(name = "estado_cliente", nullable = false, length = 8)
    private String estadoCliente;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL)
    private CondicionCredito condicionCredito;
}
