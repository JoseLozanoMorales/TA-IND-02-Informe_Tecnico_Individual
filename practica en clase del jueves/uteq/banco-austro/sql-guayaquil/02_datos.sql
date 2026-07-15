-- Datos iniciales del nodo Guayaquil
INSERT INTO clientes (cedula, nombre, ciudad) VALUES
    ('0901234567', 'Ana Zambrano', 'Guayaquil'),
    ('0909876543', 'Carlos Vera', 'Guayaquil'),
    ('0904567890', 'Daniela Moran', 'Guayaquil');

INSERT INTO cuentas (numero, cliente_id, saldo, oficina) VALUES
    ('0901000001', 1, 12500.00, 'GUAYAQUIL'),
    ('0901000002', 2, 3600.25, 'GUAYAQUIL'),
    ('0901000003', 3, 1100.80, 'GUAYAQUIL');

INSERT INTO transacciones (cuenta_orig, cuenta_dest, monto, oficina) VALUES
    ('0901000001', '0901000002', 450.00, 'GUAYAQUIL'),
    ('0901000002', '0901000003', 90.50, 'GUAYAQUIL');
