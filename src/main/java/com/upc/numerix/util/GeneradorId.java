package com.upc.numerix.util;

import org.springframework.data.jpa.repository.JpaRepository;

// Genera códigos correlativos legibles (PRD-00001, CMP-000001, ...)
public final class GeneradorId {

    private GeneradorId() {
    }

    public static String siguiente(String prefijo, int digitos, JpaRepository<?, String> repositorio) {
        long numero = repositorio.count();
        String id;
        do {
            numero++;
            id = prefijo + String.format("%0" + digitos + "d", numero);
        } while (repositorio.existsById(id));
        return id;
    }
}
