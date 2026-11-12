package com.upc.numerix.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

// Pantalla Cronograma: se elige el cliente y se muestran los cronogramas de sus compras en cuotas
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CronogramaClienteDTO {
    private String idCliente;
    private String nombreCliente;
    // Vencimiento más cercano entre las cuotas no pagadas de todas sus compras en cuotas
    private LocalDate proximaFechaPago;
    private List<CronogramaDTO> cronogramas;
}
