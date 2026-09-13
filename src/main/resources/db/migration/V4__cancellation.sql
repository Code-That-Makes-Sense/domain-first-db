ALTER TABLE orders ADD COLUMN cancelled_on DATE;
ALTER TABLE orders ADD COLUMN cancellation_reason VARCHAR(255);
