ALTER TABLE generation_history
ADD COLUMN input_token_count INT,
ADD COLUMN output_token_count INT,
ADD COLUMN estimated_cost_usd NUMERIC(12, 6);
