-- Initialize database for Location Sync Gateway
-- This script runs automatically when PostgreSQL container starts

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";  -- For text search optimization

-- Create custom types
CREATE TYPE resolution_outcome_type AS ENUM ('AUTO_ACCEPT', 'AUTO_REVERT', 'FLAG_FOR_REVIEW');
CREATE TYPE data_source_type AS ENUM ('GOOGLE_MY_BUSINESS', 'POS_SYSTEM', 'ERP_SYSTEM', 'WEBHOOK');
CREATE TYPE field_type_enum AS ENUM ('PHONE', 'OPENING_HOURS', 'COORDINATES', 'CATEGORIES', 'ADDRESS', 'TEMPORARY_CLOSURE');
CREATE TYPE severity_type AS ENUM ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW');

-- Grant privileges to application user
GRANT ALL PRIVILEGES ON DATABASE location_sync_gateway TO lsg;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO lsg;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO lsg;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO lsg;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO lsg;

-- Create application schema if needed
CREATE SCHEMA IF NOT EXISTS lsg AUTHORIZATION lsg;

-- Add comments for documentation
COMMENT ON DATABASE location_sync_gateway IS 'Location Sync Gateway - Conflict Resolution Engine';
COMMENT ON SCHEMA public IS 'Public schema for Location Sync Gateway tables';