CREATE TABLE return_requests (
    id           UUID        PRIMARY KEY,
    order_id     BIGINT      NOT NULL REFERENCES orders (id),
    sku          VARCHAR(64) NOT NULL,
    quantity     INT         NOT NULL CHECK (quantity > 0),
    requested_on DATE        NOT NULL,
    refund_cents BIGINT      NOT NULL CHECK (refund_cents >= 0),
    status       VARCHAR(16) NOT NULL
        CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'RECEIVED', 'REFUNDED'))
);
