# Numerix – Backend

API REST de Numerix (Spring Boot 3.5, Java 17, PostgreSQL, JWT). Tiene la misma estructura que el backend de ejemplo del curso: `entities`, `repositories`, `dtos`, `services` + `serviceimpl`, `controllers` y `security`.

## Ejecutar

1. Crear la base de datos `db_numerix` en PostgreSQL. El usuario y la contraseña se configuran en `src/main/resources/application.properties`.
2. `./mvnw spring-boot:run` (en Windows: `mvnw.cmd spring-boot:run`).
3. Swagger: http://localhost:8080/swagger-ui.html. Primero se llama a `POST /api/authenticate`, luego se pulsa **Authorize** y se pega el `jwt`.

Pruebas automáticas (no necesitan PostgreSQL, usan H2 en memoria): `./mvnw test`.

### Script PostgreSQL (`docs/numerix-postgresql.sql`)

Crea las 10 tablas con sus claves, restricciones CHECK e índices, y carga los mismos datos iniciales que `import.sql`. Sirve para el informe o para crear la base a mano en pgAdmin o psql. Si lo usan así, cambien `spring.jpa.hibernate.ddl-auto` a `validate` o `none`, para que Hibernate no vuelva a crear las tablas.

### Base de datos inicial (`src/main/resources/import.sql`)

Con `spring.jpa.hibernate.ddl-auto=create-drop`, cada vez que se inicia la aplicación Hibernate crea las tablas y ejecuta `import.sql`. Ese archivo carga los roles (`AdminSistema`, `AdminNegocio`, `Cliente`), los usuarios de prueba y los negocios, clientes, condiciones de crédito y productos de los juegos de prueba 1 y 2. Al detener la aplicación se borran los datos registrados. Si cambian a `update`, los datos se conservan, pero `import.sql` ya no se ejecuta. Las contraseñas se guardan como hash BCrypt; para cambiar una, generen su hash en https://bcrypt-generator.com/.

| Rol | Usuario | Contraseña | Datos |
|---|---|---|---|
| AdminSistema | admin | Numerix2026 | — |
| AdminNegocio | donlucho | Bodega2026 | Bodega Don Lucho (juego 1) |
| AdminNegocio | laesquina | Bazar2026 | Bazar La Esquina (juego 2) |
| Cliente | 45871236 | NX45871236 | Rosa Quispe Huamán |
| Cliente | 70412589 | NX70412589 | Carlos Mendoza Ríos |

Al registrar un cliente, el sistema genera su usuario (número de documento) y su contraseña (`NX` + número de documento).

## Paquetes

| Paquete | Contenido |
|---|---|
| `finanzas.CalculadoraFinanciera` | Fórmulas del apartado 6.3: conversión de tasas, TED/TEM, conteo de días 30/360, fecha de pago según el corte, interés compuesto, gracia total, método francés y prepago |
| `finanzas.CuentaCorriente` | Crédito utilizado y disponible, deuda de cada cuota a una fecha (con mora o descuento por prepago) y estados |
| `serviceimpl` | Reglas de negocio: registros, compras, pagos, cronograma, reporte de deuda y movimientos |
| `security` | Login JWT. El rol de la cuenta (`AdminSistema`, `AdminNegocio`, `Cliente`) se usa en `@PreAuthorize` |

## Endpoints

Todos usan el prefijo `/api` y necesitan el header `Authorization: Bearer <jwt>`, salvo el login.

| Método | Ruta | Rol | Pantalla |
|---|---|---|---|
| POST | `/authenticate` `{usuario, contrasena, recordarContrasena}` | — | Inicio de sesión (con `recordarContrasena` el token dura 7 días) |
| GET | `/admin/dashboard` | AdminSistema | Dashboard del administrador |
| GET / POST | `/negocios` | AdminSistema | Listado y registro de negocios |
| PUT | `/negocios/{id}/estado` | AdminSistema | Activar o inactivar un negocio |
| GET / POST | `/productos` | AdminNegocio | Listado y registro de productos |
| PUT | `/productos/{id}`, `/productos/{id}/estado` | AdminNegocio | Editar un producto |
| POST | `/productos/imagen` (multipart `archivo`) | AdminNegocio | Subir imagen; devuelve la ruta para `imagenProducto` |
| GET / POST | `/clientes` | AdminNegocio | Listado y registro de clientes (devuelve las credenciales) |
| PUT | `/clientes/{id}`, `/clientes/{id}/estado` | AdminNegocio | Cambiar las condiciones o el estado de un cliente |
| GET | `/clientes/{id}` | AdminNegocio, Cliente | Datos del cliente y crédito disponible |
| POST | `/compras/simular` | AdminNegocio | Resumen antes de registrar (crédito disponible, cuota estimada) |
| POST | `/compras` | AdminNegocio | Registro de compra a crédito |
| GET | `/clientes/{id}/cronograma` | AdminNegocio, Cliente | Pantalla Cronograma: se elige el cliente y se ven los cronogramas de sus compras **en cuotas** y la próxima fecha de pago |
| GET | `/compras/{id}/cronograma` | AdminNegocio, Cliente | Cronograma de una compra en cuotas (una compra a un solo pago responde 422) |
| GET | `/clientes/{id}/obligaciones-pendientes` | AdminNegocio, Cliente | Combo de obligaciones del Registro de pago |
| GET | `/pagos/simular?idCuota=&fechaPagoReal=` | AdminNegocio | Resumen del pago (mora, interés, capital, total) |
| POST | `/pagos` | AdminNegocio | Registro de pago |
| GET | `/clientes/{id}/reporte-deuda?fecha=` | AdminNegocio, Cliente | Reporte de deuda (sin `fecha` se usa hoy) |
| GET | `/clientes/{id}/compras?fechaInicioConsulta=&fechaFinConsulta=` | AdminNegocio, Cliente | Movimientos – Compras |
| GET | `/clientes/{id}/pagos?fechaInicioConsulta=&fechaFinConsulta=` | AdminNegocio, Cliente | Movimientos – Pagos |

Una operación que incumple una regla responde **422** con `{"estadoOperacion": "Rechazada", "mensaje": "..."}`. Un dato mal ingresado responde **400** con `errores` por campo.

## Reglas implementadas (juegos de prueba del informe)

- Se usa un año de 360 días y meses de 30 días. TED y TEM se redondean a 7 decimales y los importes a 2.
- **Un solo pago:** la compra vence en la fecha de pago del ciclo. Si se hizo después del corte, vence el mes siguiente. Se cobra el interés compensatorio por los días que pasan entre la compra y esa fecha.
- **Cuotas:** los días que pasan entre la compra y la fecha de pago del ciclo son de gracia total y se capitalizan. Luego se aplica el método francés vencido ordinario, la primera cuota vence un mes después y la última cuota se ajusta para que el saldo quede en 0.00.
- **Mora:** se calcula sobre la deuda vencida con la TED moratoria, por los días de atraso.
- **Prepago:** la cuota se descuenta a la fecha de pago. En cuotas se usa la TEM y en un solo pago la TED.
- **Imputación:** el pago se aplica primero a la mora, luego al interés compensatorio y al final al capital.
- **Rechazos:**
  - No se aceptan pagos parciales.
  - No se aceptan compras mayores al crédito disponible.
  - No se aceptan más cuotas que el máximo de meses.
  - Un cliente inactivo no puede comprar.
  - Un producto inactivo no se puede vender, y se respeta su modalidad de venta.
  - Las cuotas se pagan en orden.
- Cada obligación guarda una copia de las tasas que tenía el cliente al comprar.
