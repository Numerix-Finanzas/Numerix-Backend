package com.upc.products.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardAdminDTO {
    private long totalNegocios;
    private long totalNegociosActivos;
    private long totalNegociosInactivos;
}
