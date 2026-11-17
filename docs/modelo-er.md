# Modelo entidad-relación – Numerix

El modelo se genera desde las entidades JPA (`entities/` y `security/entities/`). GitHub, IntelliJ y VS Code muestran el diagrama directamente; `modelo-er.png` y `modelo-er.svg` son la misma imagen exportada.

```mermaid
erDiagram
  ROL ||--o{ USUARIO : "se asigna a"
  NEGOCIO ||--o{ USUARIO : "administra"
  NEGOCIO ||--o{ CLIENTE : "registra"
  NEGOCIO ||--o{ PRODUCTO : "vende"
  CLIENTE |o--o| USUARIO : "accede con"
  CLIENTE ||--|| CONDICION_CREDITO : "tiene"
  CLIENTE ||--o{ COMPRA : "realiza"
  PRODUCTO ||--o{ COMPRA : "se compra en"
  COMPRA ||--|| OBLIGACION : "origina"
  OBLIGACION ||--|{ CUOTA : "se divide en"
  OBLIGACION ||--o{ PAGO : "recibe"
  CUOTA ||--o| PAGO : "se cancela con"

  ROL {
    bigint id_rol PK "IDENTITY"
    varchar(20) nombre UK "AdminSistema / AdminNegocio / Cliente"
  }
  USUARIO {
    bigint id_usuario PK "IDENTITY"
    varchar(20) usuario UK
    varchar(64) contrasena "hash BCrypt"
    bigint id_rol FK
    varchar(100) nombre_completo
    varchar(100) correo_electronico
    varchar(20) id_negocio FK "nulo para AdminSistema"
    varchar(20) id_cliente FK, UK "solo rol Cliente"
    boolean activo
    timestamp fecha_registro
  }
  NEGOCIO {
    varchar(20) id_negocio PK
    varchar(100) nombre_negocio
    varchar(11) ruc_negocio UK
    varchar(60) tipo_negocio
    varchar(10) estado_negocio "Activo / Inactivo"
    timestamp fecha_registro
  }
  CLIENTE {
    varchar(20) id_cliente PK
    varchar(20) id_negocio FK
    varchar(100) nombre_cliente
    varchar(10) tipo_documento_cliente "DNI / CE / RUC"
    varchar(12) numero_documento_cliente UK
    varchar(8) estado_cliente "Activo / Inactivo"
    timestamp fecha_registro
  }
  CONDICION_CREDITO {
    bigint id_condicion_credito PK "IDENTITY"
    varchar(20) id_cliente FK, UK
    varchar(3) moneda "PEN / USD"
    numeric limite_credito "18,2"
    varchar(10) tipo_tasa "Nominal / Efectiva"
    numeric porcentaje_tasa "18,7"
    varchar(10) tipo_tasa_moratoria "Nominal / Efectiva"
    numeric porcentaje_tasa_moratoria "18,7"
    varchar(15) periodo_tasa
    integer periodo_capitalizacion "por defecto 30"
    integer dia_corte "1-30"
    integer dia_pago_pactado "1-30"
    integer maximo_meses_credito "> 0"
  }
  PRODUCTO {
    varchar(20) id_producto PK
    varchar(20) id_negocio FK
    varchar(150) descripcion_producto
    varchar(50) marca_producto
    varchar(100) proveedor_producto
    varchar(20) unidad_medida
    varchar(255) imagen_producto
    numeric precio_contado "18,2"
    numeric precio_credito "18,2"
    varchar(10) modalidad_venta_producto "Contado / Crédito / Ambas"
    varchar(10) estado_producto
    timestamp fecha_registro
  }
  COMPRA {
    varchar(20) id_compra PK
    varchar(20) id_cliente FK
    varchar(20) id_producto FK
    date fecha_compra
    integer cantidad_producto
    varchar(15) tipo_pago "Un solo pago / Cuotas"
    integer numero_cuotas
    numeric precio_credito_aplicado "18,2"
    numeric monto_compra "18,2"
    timestamp fecha_registro
  }
  OBLIGACION {
    varchar(20) id_obligacion PK
    varchar(20) id_compra FK, UK
    varchar(3) moneda
    varchar(10) tipo_tasa "copia al comprar"
    numeric porcentaje_tasa "18,7"
    varchar(15) periodo_tasa
    integer periodo_capitalizacion
    varchar(10) tipo_tasa_moratoria
    numeric porcentaje_tasa_moratoria "18,7"
    numeric capital_inicial "18,2"
    numeric capital_actualizado "18,2 con gracia"
    integer dias_gracia
    timestamp fecha_registro
  }
  CUOTA {
    bigint id_cuota PK "IDENTITY"
    varchar(20) id_obligacion FK "UK con numero_cuota"
    integer numero_cuota
    date fecha_vencimiento
    numeric saldo_inicial "18,2"
    numeric interes_compensatorio "18,2"
    numeric amortizacion "18,2"
    numeric monto_cuota "18,2"
    numeric saldo_final "18,2"
    boolean pagada
    date fecha_pago
  }
  PAGO {
    varchar(20) id_pago PK
    varchar(20) id_obligacion FK
    bigint id_cuota FK, UK
    date fecha_pago_real
    numeric monto_pagado "18,2"
    numeric monto_imputado_mora "18,2"
    numeric monto_imputado_interes "18,2"
    numeric monto_imputado_capital "18,2"
    integer dias_mora
    integer dias_anticipacion
    timestamp fecha_registro
  }
```

## Base de datos inicial

`src/main/resources/import.sql` carga los 3 roles, el administrador del sistema y los negocios, clientes (con sus condiciones de crédito) y productos de los juegos de prueba 1 y 2. Hibernate lo ejecuta cada vez que crea las tablas (`spring.jpa.hibernate.ddl-auto=create-drop`).

## Cambios respecto al modelo del informe (figura 38)

| Tabla | Cambio | Motivo |
|---|---|---|
| `condicion_credito` | Tabla nueva (1 a 1 con `cliente`) con moneda, límite, tasas, periodos, corte, pago y máximo de meses | No son datos del cliente en sí, sino del crédito que el negocio le otorga. |
| `rol` + `usuario` | Reemplazan a `usuario_sistema` | El rol queda en su propia tabla y se carga en la base de datos inicial. |
| `obligacion` | + `dias_gracia` | El cronograma conserva los días de gracia que se capitalizaron. |
| `cuota` | + `fecha_pago` | Permite consultar el reporte de deuda a una fecha pasada. |
| `pago` | + `id_cuota` (único) | Cada pago cancela exactamente una cuota. |
| `pago` | + `dias_mora`, `dias_anticipacion` | El historial de movimientos muestra los días de atraso o de prepago. |
| Un solo pago | Se guarda como una obligación con 1 cuota | Así el pago, el reporte y el cronograma funcionan igual para las dos modalidades. |
