package com.upc.numerix.exceptions;

// Regla de negocio incumplida: la operación no se registra (estadoOperacion = Rechazada)
public class OperacionRechazadaException extends RuntimeException {
    public OperacionRechazadaException(String mensaje) {
        super(mensaje);
    }
}
