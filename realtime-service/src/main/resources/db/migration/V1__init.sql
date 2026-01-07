-- Vehicle locations table with geohash indexing
CREATE TABLE IF NOT EXISTS vehicle_locations (
    id CHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::CHAR(36),
    vehicle_id CHAR(36) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    geohash VARCHAR(12) NOT NULL,
    speed DOUBLE PRECISION,
    bearing DOUBLE PRECISION,
    accuracy DOUBLE PRECISION,
    altitude DOUBLE PRECISION,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_vehicle_location_vehicle_id ON vehicle_locations(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_location_geohash ON vehicle_locations(geohash);
CREATE INDEX IF NOT EXISTS idx_vehicle_location_timestamp ON vehicle_locations(timestamp);

-- Trip tracking table
CREATE TABLE IF NOT EXISTS trip_tracking (
    id CHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::CHAR(36),
    trip_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    vehicle_id CHAR(36) NOT NULL,
    driver_id CHAR(36) NOT NULL,
    start_location_lat DOUBLE PRECISION NOT NULL,
    start_location_lng DOUBLE PRECISION NOT NULL,
    start_location_geohash VARCHAR(12) NOT NULL,
    destination_lat DOUBLE PRECISION NOT NULL,
    destination_lng DOUBLE PRECISION NOT NULL,
    destination_geohash VARCHAR(12) NOT NULL,
    current_location_lat DOUBLE PRECISION,
    current_location_lng DOUBLE PRECISION,
    current_location_geohash VARCHAR(12),
    start_time TIMESTAMP WITH TIME ZONE,
    estimated_arrival_time TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL,
    distance_km DOUBLE PRECISION,
    share_code VARCHAR(10),
    is_shared BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_trip_trip_id ON trip_tracking(trip_id);
CREATE INDEX IF NOT EXISTS idx_trip_user_id ON trip_tracking(user_id);
CREATE INDEX IF NOT EXISTS idx_trip_vehicle_id ON trip_tracking(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_trip_status ON trip_tracking(status);
CREATE INDEX IF NOT EXISTS idx_trip_geohash ON trip_tracking(current_location_geohash);
CREATE INDEX IF NOT EXISTS idx_trip_share_code ON trip_tracking(share_code);

-- Safety alerts table
CREATE TABLE IF NOT EXISTS safety_alerts (
    id CHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::CHAR(36),
    alert_type VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    geohash VARCHAR(12) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    radius_km DOUBLE PRECISION,
    reported_by CHAR(36) NOT NULL,
    verified BOOLEAN DEFAULT FALSE NOT NULL,
    start_time TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_alert_geohash ON safety_alerts(geohash);
CREATE INDEX IF NOT EXISTS idx_alert_active ON safety_alerts(is_active);
CREATE INDEX IF NOT EXISTS idx_alert_alert_type ON safety_alerts(alert_type);
CREATE INDEX IF NOT EXISTS idx_alert_severity ON safety_alerts(severity);
CREATE INDEX IF NOT EXISTS idx_alert_reported_by ON safety_alerts(reported_by);

-- Emergency cases table
CREATE TABLE IF NOT EXISTS emergency_cases (
    id CHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::CHAR(36),
    user_id CHAR(36) NOT NULL,
    trip_id CHAR(36),
    emergency_type VARCHAR(50) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    geohash VARCHAR(12) NOT NULL,
    message VARCHAR(1000),
    is_resolved BOOLEAN DEFAULT FALSE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE,
    shared_with_contacts BOOLEAN DEFAULT FALSE NOT NULL,
    shared_with_authorities BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT TIMEZONE('utc', NOW()) NOT NULL
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_emergency_user_id ON emergency_cases(user_id);
CREATE INDEX IF NOT EXISTS idx_emergency_trip_id ON emergency_cases(trip_id);
CREATE INDEX IF NOT EXISTS idx_emergency_is_resolved ON emergency_cases(is_resolved);
CREATE INDEX IF NOT EXISTS idx_emergency_geohash ON emergency_cases(geohash);
