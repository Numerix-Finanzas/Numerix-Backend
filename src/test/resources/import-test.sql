/*Datos iniciales de las pruebas automáticas: solo roles y administrador del sistema (admin / Numerix2026)*/
INSERT INTO rol (nombre) VALUES ('AdminSistema');
INSERT INTO rol (nombre) VALUES ('AdminNegocio');
INSERT INTO rol (nombre) VALUES ('Cliente');
INSERT INTO usuario (usuario, contrasena, id_rol, nombre_completo, correo_electronico, id_negocio, id_cliente, activo, fecha_registro) VALUES ('admin', '$2a$10$A3Ab9YfUqjF2esQm6jLf3evhDZJeW2OhF.E/0yFlpqJYEriroD5Zi', 1, 'Administrador Numerix', 'admin@numerix.pe', NULL, NULL, true, CURRENT_TIMESTAMP);
