-- Additive upgrade for older stores. Existing products and prices are not changed.
-- The customer and admin catalog requests select both optional fields, so they
-- must exist even when no discounts or ratings have been entered yet.
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS original_price numeric(12,2);
ALTER TABLE public.products ADD COLUMN IF NOT EXISTS rating numeric(2,1);
COMMENT ON COLUMN public.products.original_price IS 'Optional pre-discount price; must be strictly above price when set.';
COMMENT ON COLUMN public.products.rating IS 'Optional average rating from 0 to 5; null means unrated.';
DO $migration$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conrelid = 'public.products'::regclass AND conname = 'products_original_price_above_price') THEN
    ALTER TABLE public.products ADD CONSTRAINT products_original_price_above_price CHECK (original_price IS NULL OR original_price > price);
  END IF;
  IF NOT EXISTS (SELECT 1 FROM pg_catalog.pg_constraint WHERE conrelid = 'public.products'::regclass AND conname = 'products_rating_range') THEN
    ALTER TABLE public.products ADD CONSTRAINT products_rating_range CHECK (rating IS NULL OR (rating >= 0 AND rating <= 5));
  END IF;
END
$migration$;
