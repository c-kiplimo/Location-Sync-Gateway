-- V2__seed_initial_data.sql
-- Seed initial data for development and testing

-- Seed default resolution rules
INSERT INTO resolution_rules (id, name, field_type, source, location_pattern, default_outcome, minimum_confidence, conditions, priority, enabled) VALUES
(gen_random_uuid(), 'google-opening-hours-franchisee', 'OPENING_HOURS', 'GOOGLE_MY_BUSINESS', 'franchise.*', 'AUTO_ACCEPT', 0.9,
 '{"type": "VALUE_WITHIN_RANGE", "field": "openingHourSpan", "min": 6, "max": 16}'::jsonb, 100, true),

(gen_random_uuid(), 'google-phone-hq-override', 'PHONE', 'GOOGLE_MY_BUSINESS', NULL, 'AUTO_REVERT', 1.0,
 NULL, 200, true),

(gen_random_uuid(), 'google-category-brand-standard', 'CATEGORIES', 'GOOGLE_MY_BUSINESS', NULL, 'AUTO_REVERT', 1.0,
 NULL, 200, true),

(gen_random_uuid(), 'pos-temporary-closure', 'TEMPORARY_CLOSURE', 'POS_SYSTEM', NULL, 'FLAG_FOR_REVIEW', 0.85,
 '{"notifyChannel": "slack", "notifyTarget": "#location-ops-alerts"}'::jsonb, 150, true),

(gen_random_uuid(), 'erp-planned-closure', 'TEMPORARY_CLOSURE', 'ERP_SYSTEM', NULL, 'AUTO_ACCEPT', 1.0,
 NULL, 250, true),

(gen_random_uuid(), 'google-coordinates-minor-change', 'COORDINATES', 'GOOGLE_MY_BUSINESS', NULL, 'AUTO_ACCEPT', 0.95,
 '{"type": "CHANGE_MAGNITUDE", "field": "coordinates", "maxDistanceKm": 0.1}'::jsonb, 100, true),

(gen_random_uuid(), 'google-coordinates-major-change', 'COORDINATES', 'GOOGLE_MY_BUSINESS', NULL, 'FLAG_FOR_REVIEW', 0.8,
 '{"type": "CHANGE_MAGNITUDE", "field": "coordinates", "maxDistanceKm": 10}'::jsonb, 50, true),

(gen_random_uuid(), 'google-address-update', 'ADDRESS', 'GOOGLE_MY_BUSINESS', NULL, 'AUTO_ACCEPT', 0.9,
 NULL, 100, true),

(gen_random_uuid(), 'google-website-update', 'WEBSITE', 'GOOGLE_MY_BUSINESS', NULL, 'AUTO_ACCEPT', 0.95,
 NULL, 100, true),

(gen_random_uuid(), 'pos-hours-emergency-closure', 'OPENING_HOURS', 'POS_SYSTEM', NULL, 'FLAG_FOR_REVIEW', 0.7,
 '{"type": "PATTERN_MATCH", "pattern": "zero_transactions_3_days"}'::jsonb, 150, true);

-- Seed sample ML models metadata (for demonstration)
INSERT INTO ml_models (id, model_name, version, algorithm, trained_at, training_samples, metrics, features, deployed_at, status, model_path) VALUES
(gen_random_uuid(), 'anomaly-detector', '1.0.0', 'IsolationForest', NOW() - INTERVAL '7 days', 50000,
 '{"precision": 0.92, "recall": 0.88, "f1Score": 0.90}'::jsonb,
 '["coord_distance", "quality_delta", "fields_changed", "time_since_last_change"]'::jsonb,
 NOW() - INTERVAL '6 days', 'ACTIVE', './models/anomaly-detector/v1.0.0/model.bin'),

(gen_random_uuid(), 'resolution-predictor', '1.0.0', 'XGBoost', NOW() - INTERVAL '14 days', 100000,
 '{"precision": 0.87, "recall": 0.85, "f1Score": 0.86, "accuracy": 0.86}'::jsonb,
 '["field_type", "source", "quality_score", "anomaly_score", "historical_accuracy", "day_of_week", "hour"]'::jsonb,
 NOW() - INTERVAL '13 days', 'ACTIVE', './models/resolution-predictor/v1.0.0/model.bin');

-- Seed sample test data for development (4 test locations)
-- Location 1001: Phone conflict (AUTO_REVERT expected)
INSERT INTO conflicts (id, location_id, field_type, source, external_value, uberall_value, confidence, quality_score, detected_at) VALUES
(gen_random_uuid(), 1001, 'PHONE', 'GOOGLE_MY_BUSINESS',
 '"+254799999999"'::jsonb, '"+254712345678"'::jsonb, 1.0,
 '{"overallScore": 85, "breakdown": {"coordinates": 20, "address": 20, "openingHours": 15, "keywords": 15, "consistency": 15}, "severity": "LOW"}'::jsonb,
 NOW() - INTERVAL '1 hour');

-- Location 1002: Opening hours conflict (AUTO_ACCEPT expected)
INSERT INTO conflicts (id, location_id, field_type, source, external_value, uberall_value, confidence, quality_score, detected_at) VALUES
(gen_random_uuid(), 1002, 'OPENING_HOURS', 'GOOGLE_MY_BUSINESS',
 '{"monday": {"open": "08:00", "close": "20:00"}, "tuesday": {"open": "08:00", "close": "20:00"}}'::jsonb,
 '{"monday": {"open": "09:00", "close": "18:00"}, "tuesday": {"open": "09:00", "close": "18:00"}}'::jsonb, 0.95,
 '{"overallScore": 90, "breakdown": {"coordinates": 20, "address": 20, "openingHours": 20, "keywords": 15, "consistency": 15}, "severity": "LOW"}'::jsonb,
 NOW() - INTERVAL '2 hours');

-- Location 1003: Categories conflict (AUTO_REVERT expected)
INSERT INTO conflicts (id, location_id, field_type, source, external_value, uberall_value, confidence, quality_score, detected_at) VALUES
(gen_random_uuid(), 1003, 'CATEGORIES', 'GOOGLE_MY_BUSINESS',
 '["bar", "nightclub"]'::jsonb, '["restaurant"]'::jsonb, 1.0,
 '{"overallScore": 80, "breakdown": {"coordinates": 20, "address": 20, "openingHours": 15, "keywords": 15, "consistency": 10}, "severity": "MEDIUM"}'::jsonb,
 NOW() - INTERVAL '3 hours');

-- Location 1004: POS temporary closure (FLAG_FOR_REVIEW expected)
INSERT INTO conflicts (id, location_id, field_type, source, external_value, uberall_value, confidence, quality_score, detected_at) VALUES
(gen_random_uuid(), 1004, 'TEMPORARY_CLOSURE', 'POS_SYSTEM',
 '{"closed": true, "reason": "zero_transactions_3_days", "startDate": "2026-09-20", "endDate": null}'::jsonb,
 '{"closed": false}'::jsonb, 0.8,
 '{"overallScore": 75, "breakdown": {"coordinates": 20, "address": 20, "openingHours": 15, "keywords": 10, "consistency": 10}, "severity": "MEDIUM"}'::jsonb,
 NOW() - INTERVAL '30 minutes');

-- Seed sample location snapshots
INSERT INTO location_snapshots (id, location_id, source, snapshot_data, quality_score, captured_at, processed_at) VALUES
(gen_random_uuid(), 1001, 'GOOGLE_MY_BUSINESS',
 '{"phone": "+254799999999", "name": "Test Restaurant Nairobi", "address": {"street": "Moi Avenue", "city": "Nairobi", "postalCode": "00100"}}'::jsonb,
 '{"overallScore": 85}'::jsonb, NOW() - INTERVAL '1 hour', NOW() - INTERVAL '59 minutes'),

(gen_random_uuid(), 1002, 'GOOGLE_MY_BUSINESS',
 '{"phone": "+254711111111", "name": "Mombasa Cafe", "openingHours": {"monday": {"open": "08:00", "close": "20:00"}}}'::jsonb,
 '{"overallScore": 90}'::jsonb, NOW() - INTERVAL '2 hours', NOW() - INTERVAL '119 minutes'),

(gen_random_uuid(), 1003, 'GOOGLE_MY_BUSINESS',
 '{"phone": "+254722222222", "name": "Kisumu Diner", "categories": ["bar", "nightclub"]}'::jsonb,
 '{"overallScore": 80}'::jsonb, NOW() - INTERVAL '3 hours', NOW() - INTERVAL '179 minutes'),

(gen_random_uuid(), 1004, 'POS_SYSTEM',
 '{"transactionCount": 0, "consecutiveDaysNoTransactions": 3, "lastTransactionDate": "2026-09-17"}'::jsonb,
 '{"overallScore": 75}'::jsonb, NOW() - INTERVAL '30 minutes', NOW() - INTERVAL '29 minutes');

-- Seed sample quality baselines
INSERT INTO quality_baselines (id, location_id, baseline_data, quality_score, calculated_at, sample_count) VALUES
(gen_random_uuid(), 1001,
 '{"avgPhone": "+254712345678", "avgCoordinates": {"lat": -1.2921, "lng": 36.8219}, "avgOpeningHours": {"monday": {"open": "09:00", "close": "18:00"}}}'::jsonb,
 '{"overallScore": 88, "historicalAverage": 87}'::jsonb, NOW() - INTERVAL '7 days', 30),

(gen_random_uuid(), 1002,
 '{"avgPhone": "+254799999999", "avgCoordinates": {"lat": -4.0435, "lng": 39.6682}}'::jsonb,
 '{"overallScore": 92, "historicalAverage": 91}'::jsonb, NOW() - INTERVAL '7 days', 28),

(gen_random_uuid(), 1003,
 '{"avgPhone": "+254788888888", "avgCategories": ["restaurant"], "avgCoordinates": {"lat": -0.0917, "lng": 34.7680}}'::jsonb,
 '{"overallScore": 85, "historicalAverage": 84}'::jsonb, NOW() - INTERVAL '7 days', 25),

(gen_random_uuid(), 1004,
 '{"avgTransactionCount": 150, "avgDailyRevenue": 45000}'::jsonb,
 '{"overallScore": 80, "historicalAverage": 82}'::jsonb, NOW() - INTERVAL '7 days', 20);

-- Add comments
COMMENT ON TABLE resolution_rules IS 'Seeded with default resolution rules for common conflict scenarios';
COMMENT ON TABLE ml_models IS 'Seeded with initial ML model metadata for demonstration';
COMMENT ON TABLE conflicts IS 'Seeded with 4 test conflicts covering different scenarios';
COMMENT ON TABLE location_snapshots IS 'Seeded with sample snapshots for test locations';
COMMENT ON TABLE quality_baselines IS 'Seeded with quality baselines for 4 test locations';