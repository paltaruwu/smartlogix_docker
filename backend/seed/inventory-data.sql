-- Seed idempotente: solo inserta el producto si su code aún no existe.
-- (product.code no tiene UNIQUE, por eso INSERT IGNORE duplicaba en cada reinicio)
INSERT INTO product (code, title, description, stock, price, image_src)
SELECT 'P001', 'Laptop Gamer', 'Alto rendimiento', 10, 1000000.0, 'https://cl-dam-resizer.ecomm.cencosud.com/unsafe/adaptive-fit-in/3840x0/filters:quality(75)/paris/635077999/variant/images/8bfe5fc5-fab8-451b-afc4-fb29696ebd9d/635077999-0000-007.jpg'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product WHERE code = 'P001');

INSERT INTO product (code, title, description, stock, price, image_src)
SELECT 'P002', 'Mouse Óptico', 'Precisión total', 20, 25000.0, 'https://pe-media.hptiendaenlinea.com/magefan_blog/mouse_ptico_vs_l_ser.jpg'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product WHERE code = 'P002');

INSERT INTO product (code, title, description, stock, price, image_src)
SELECT 'P003', 'Teclado Mecánico', 'RGB retroiluminado', 15, 50000.0, 'https://pronobel.cl/cdn/shop/files/p-523776-2-ae5ed3e1-9529-40de-a630-b9c7872b3ae3.jpg?v=1734102555&width=1946'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM product WHERE code = 'P003');
