-- Se ejecuta automáticamente la primera vez que el contenedor de MySQL
-- arranca (carpeta /docker-entrypoint-initdb.d). Reemplaza lo que antes
-- hacías a mano en phpMyAdmin/XAMPP.

CREATE DATABASE IF NOT EXISTS inventory_db CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS order_db     CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS shipment_db  CHARACTER SET utf8mb4;
