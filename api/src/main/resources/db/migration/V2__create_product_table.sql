CREATE TABLE product (
    id BIGSERIAL PRIMARY KEY,
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    category_id BIGINT NOT NULL REFERENCES category(id),
    price DOUBLE PRECISION NOT NULL,
    stock INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_product_category_id ON product(category_id);
CREATE INDEX idx_product_active ON product(active);
