-- Database: mop_test_assignment

-- DROP DATABASE IF EXISTS mop_test_assignment;

CREATE DATABASE mop_test_assignment
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'Croatian_Bosnia & Herzegovina.1252'
    LC_CTYPE = 'Croatian_Bosnia & Herzegovina.1252'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;



-- Table: public.temp_product_price

-- DROP TABLE IF EXISTS public.temp_product_price;

CREATE TABLE IF NOT EXISTS public.temp_product_price
(
    product_id integer NOT NULL,
    price numeric NOT NULL
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.temp_product_price
    OWNER to postgres;



-- Table: public.request_execution_time

-- DROP TABLE IF EXISTS public.request_execution_time;

CREATE TABLE IF NOT EXISTS public.request_execution_time
(
    request_id uuid NOT NULL,
    url text COLLATE pg_catalog."default" NOT NULL,
    request_executed_at timestamp without time zone NOT NULL,
    response_time integer NOT NULL,
    CONSTRAINT pk_request_id PRIMARY KEY (request_id)
)

TABLESPACE pg_default;

ALTER TABLE IF EXISTS public.request_execution_time
    OWNER to postgres;
