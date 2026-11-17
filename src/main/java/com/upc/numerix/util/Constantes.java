package com.upc.numerix.util;

import java.time.LocalDate;
import java.time.ZoneId;

// Valores permitidos definidos en el análisis de datos (apartado 6.1 del informe)
public final class Constantes {

    private Constantes() {
    }

    public static final ZoneId ZONA = ZoneId.of("America/Lima");

    // rolUsuario
    public static final String ROL_ADMIN_SISTEMA = "AdminSistema";
    public static final String ROL_ADMIN_NEGOCIO = "AdminNegocio";
    public static final String ROL_CLIENTE = "Cliente";

    // estadoNegocio, estadoCliente, estadoProducto
    public static final String ACTIVO = "Activo";
    public static final String INACTIVO = "Inactivo";

    // tipoPago
    public static final String UN_SOLO_PAGO = "Un solo pago";
    public static final String CUOTAS = "Cuotas";

    // modalidadVentaProducto: Contado = solo un pago, Crédito = solo cuotas
    public static final String MODALIDAD_CONTADO = "Contado";
    public static final String MODALIDAD_CREDITO = "Crédito";
    public static final String MODALIDAD_AMBAS = "Ambas";

    // tipoTasa, tipoTasaMoratoria
    public static final String EFECTIVA = "Efectiva";
    public static final String NOMINAL = "Nominal";

    // estadoDeuda, estadoCuota
    public static final String PENDIENTE = "Pendiente";
    public static final String PAGADO = "Pagado";
    public static final String VENCIDO = "Vencido";

    // estadoOperacion
    public static final String PROCESADA = "Procesada";
    public static final String RECHAZADA = "Rechazada";

    public static final String ORDEN_PRELACION = "Mora-Interes-Capital";

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    public static String simboloMoneda(String moneda) {
        return "USD".equals(moneda) ? "US$" : "S/";
    }
}
