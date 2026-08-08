INSERT INTO category (name) VALUES ('Electrónica'), ('Hogar'), ('Ropa')
ON CONFLICT (name) DO NOTHING;

INSERT INTO product (sku, name, description, category_id, price, stock, active, version)
SELECT 'DEMO-001', 'Producto de demostración', 'Sembrado por R__seed_data.sql', c.id, 19.99, 50, true, 0
FROM category c WHERE c.name = 'Electrónica'
ON CONFLICT (sku) DO NOTHING;
