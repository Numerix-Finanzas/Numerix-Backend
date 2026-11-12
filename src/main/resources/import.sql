/*Base de datos inicial: Hibernate ejecuta este archivo al crear las tablas (ddl-auto=create-drop). Cada sentencia va en una sola línea*/
/*Roles*/
INSERT INTO rol (nombre) VALUES ('AdminSistema');
INSERT INTO rol (nombre) VALUES ('AdminNegocio');
INSERT INTO rol (nombre) VALUES ('Cliente');

/*Administrador del sistema | usuario: admin | contraseña: Numerix2026*/
INSERT INTO usuario (usuario, contrasena, id_rol, nombre_completo, correo_electronico, id_negocio, id_cliente, activo, fecha_registro) VALUES ('admin', '$2a$10$A3Ab9YfUqjF2esQm6jLf3evhDZJeW2OhF.E/0yFlpqJYEriroD5Zi', 1, 'Administrador Numerix', 'admin@numerix.pe', NULL, NULL, true, CURRENT_TIMESTAMP);

/*Negocios de los juegos de prueba 1 y 2*/
INSERT INTO negocio (id_negocio, nombre_negocio, ruc_negocio, tipo_negocio, estado_negocio, fecha_registro) VALUES ('NEG-20601234561', 'Bodega Don Lucho', '20601234561', 'Bodega', 'Activo', CURRENT_TIMESTAMP);
INSERT INTO negocio (id_negocio, nombre_negocio, ruc_negocio, tipo_negocio, estado_negocio, fecha_registro) VALUES ('NEG-20609876541', 'Bazar La Esquina', '20609876541', 'Bazar', 'Activo', CURRENT_TIMESTAMP);

/*Administradores de negocio | usuario: donlucho | contraseña: Bodega2026*/
INSERT INTO usuario (usuario, contrasena, id_rol, nombre_completo, correo_electronico, id_negocio, id_cliente, activo, fecha_registro) VALUES ('donlucho', '$2a$10$pYZtUwAqb/noolg15J0s6.ssUzCdbPBQsxMY1pRwoW7tgSbP647IG', 2, 'Luis Ramírez Torres', 'donlucho@numerix.pe', 'NEG-20601234561', NULL, true, CURRENT_TIMESTAMP);
/*usuario: laesquina | contraseña: Bazar2026*/
INSERT INTO usuario (usuario, contrasena, id_rol, nombre_completo, correo_electronico, id_negocio, id_cliente, activo, fecha_registro) VALUES ('laesquina', '$2a$10$.EE.TT/OU0zKvRW2knulW.SVlI3MA9OY9i2d7bzckTmu/Qqj5tSkS', 2, 'Ana Torres Salas', 'laesquina@numerix.pe', 'NEG-20609876541', NULL, true, CURRENT_TIMESTAMP);

/*Productos*/
INSERT INTO producto (id_producto, id_negocio, descripcion_producto, marca_producto, proveedor_producto, unidad_medida, imagen_producto, precio_contado, precio_credito, modalidad_venta_producto, estado_producto, fecha_registro) VALUES ('PRD-00001', 'NEG-20601234561', 'Arroz Costeño 5 kg', 'Costeño', 'Alicorp', 'Saco', NULL, 28.00, 30.00, 'Ambas', 'Activo', CURRENT_TIMESTAMP);
INSERT INTO producto (id_producto, id_negocio, descripcion_producto, marca_producto, proveedor_producto, unidad_medida, imagen_producto, precio_contado, precio_credito, modalidad_venta_producto, estado_producto, fecha_registro) VALUES ('PRD-00002', 'NEG-20601234561', 'Aceite Primor 1 L', 'Primor', 'Alicorp', 'Botella', NULL, 9.50, 10.00, 'Ambas', 'Activo', CURRENT_TIMESTAMP);
INSERT INTO producto (id_producto, id_negocio, descripcion_producto, marca_producto, proveedor_producto, unidad_medida, imagen_producto, precio_contado, precio_credito, modalidad_venta_producto, estado_producto, fecha_registro) VALUES ('PRD-00003', 'NEG-20601234561', 'Leche Gloria 400 g', 'Gloria', 'Gloria S.A.', 'Lata', NULL, 3.80, 4.00, 'Ambas', 'Activo', CURRENT_TIMESTAMP);
INSERT INTO producto (id_producto, id_negocio, descripcion_producto, marca_producto, proveedor_producto, unidad_medida, imagen_producto, precio_contado, precio_credito, modalidad_venta_producto, estado_producto, fecha_registro) VALUES ('PRD-00004', 'NEG-20609876541', 'Licuadora Oster 1.5 L', 'Oster', 'Sunbeam', 'Unidad', NULL, 420.00, 450.00, 'Ambas', 'Activo', CURRENT_TIMESTAMP);
INSERT INTO producto (id_producto, id_negocio, descripcion_producto, marca_producto, proveedor_producto, unidad_medida, imagen_producto, precio_contado, precio_credito, modalidad_venta_producto, estado_producto, fecha_registro) VALUES ('PRD-00005', 'NEG-20609876541', 'Tostadora Imaco', 'Imaco', 'Imaco', 'Unidad', NULL, 45.00, 50.00, 'Ambas', 'Activo', CURRENT_TIMESTAMP);

/*Clientes y sus condiciones de crédito*/
INSERT INTO cliente (id_cliente, id_negocio, nombre_cliente, tipo_documento_cliente, numero_documento_cliente, estado_cliente, fecha_registro) VALUES ('CLI-45871236', 'NEG-20601234561', 'Rosa Quispe Huamán', 'DNI', '45871236', 'Activo', CURRENT_TIMESTAMP);
INSERT INTO condicion_credito (id_cliente, moneda, limite_credito, tipo_tasa, porcentaje_tasa, tipo_tasa_moratoria, porcentaje_tasa_moratoria, periodo_tasa, periodo_capitalizacion, dia_corte, dia_pago_pactado, maximo_meses_credito) VALUES ('CLI-45871236', 'PEN', 500.00, 'Efectiva', 30.0000000, 'Efectiva', 15.0000000, 'Anual', 30, 25, 30, 3);
INSERT INTO cliente (id_cliente, id_negocio, nombre_cliente, tipo_documento_cliente, numero_documento_cliente, estado_cliente, fecha_registro) VALUES ('CLI-70412589', 'NEG-20609876541', 'Carlos Mendoza Ríos', 'DNI', '70412589', 'Activo', CURRENT_TIMESTAMP);
INSERT INTO condicion_credito (id_cliente, moneda, limite_credito, tipo_tasa, porcentaje_tasa, tipo_tasa_moratoria, porcentaje_tasa_moratoria, periodo_tasa, periodo_capitalizacion, dia_corte, dia_pago_pactado, maximo_meses_credito) VALUES ('CLI-70412589', 'PEN', 1000.00, 'Nominal', 24.0000000, 'Nominal', 12.0000000, 'Anual', 30, 20, 26, 6);

/*Clientes | usuario: 45871236 | contraseña: NX45871236*/
INSERT INTO usuario (usuario, contrasena, id_rol, nombre_completo, correo_electronico, id_negocio, id_cliente, activo, fecha_registro) VALUES ('45871236', '$2a$10$EjEcLDohd9vkTFpPy7YAbOk1KvJwlwfXVZrUB8sxJEmI7sx9COS8m', 3, 'Rosa Quispe Huamán', NULL, 'NEG-20601234561', 'CLI-45871236', true, CURRENT_TIMESTAMP);
/*usuario: 70412589 | contraseña: NX70412589*/
INSERT INTO usuario (usuario, contrasena, id_rol, nombre_completo, correo_electronico, id_negocio, id_cliente, activo, fecha_registro) VALUES ('70412589', '$2a$10$GiA8Ik43MQe3YG.tYj4hUudT/7Z9xpBY3nWp73bhiQByeQ.eH0PGm', 3, 'Carlos Mendoza Ríos', NULL, 'NEG-20609876541', 'CLI-70412589', true, CURRENT_TIMESTAMP);
