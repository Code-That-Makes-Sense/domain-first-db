CREATE TABLE stock_items (
    sku      VARCHAR(64) PRIMARY KEY,
    on_hand  INT NOT NULL,
    reserved INT NOT NULL
);
