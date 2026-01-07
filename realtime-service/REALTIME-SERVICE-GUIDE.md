# Point-to-Point Travel Management System - Realtime Service

## Overview
The Realtime Service is a critical component of the P2P Travel Management System, providing real-time tracking, safety alerts, and emergency handling capabilities. This service leverages GeoHash technology for efficient spatial queries and WebSocket communication for instant updates to clients.

## Architecture

### Components
1. **Location Tracking System**: 
   - Uses GeoHash encoding to efficiently store and query vehicle positions
   - Enables proximity searches with O(1) complexity
   - Supports trip monitoring and ETA calculation

2. **WebSocket Communication**:
   - Provides real-time updates to clients
   - Channels for vehicle tracking, trip updates, safety alerts, and emergencies
   - Supports user-specific notifications

3. **Redis Caching**:
   - Improves response times for frequently accessed data
   - Caches vehicle locations and trip information
   - Reduces database load for spatial queries

4. **PostgreSQL Spatial Storage**:
   - Persists all tracking data with spatial indexing
   - Stores trip, alert, and emergency information
   - Supports geospatial queries using GeoHash prefixes

## Key Features

### Vehicle Tracking
- Update and query vehicle locations using efficient GeoHash encoding
- Find vehicles within a specified radius from any location
- Calculate distances and ETAs between locations

### Trip Monitoring
- Create and track trips with real-time updates
- Share trip information with other users via share codes
- Calculate completed and remaining distances

### Safety Alerts
- Create and manage location-based safety alerts
- Notify users in affected areas in real-time
- Filter alerts by severity and type

### Emergency Handling
- Report and respond to emergency situations
- Coordinate responses based on location
- Notify appropriate authorities and contacts

## API Endpoints

### Tracking API
- `POST /tracking/location` - Update vehicle location
- `POST /tracking/trips` - Start tracking a new trip
- `GET /tracking/trips/{tripId}` - Get trip details
- `GET /tracking/shared/{shareCode}` - View a shared trip
- `PUT /tracking/trips/{tripId}/status/{status}` - Update trip status
- `POST /tracking/trips/share` - Share a trip
- `POST /tracking/vehicles/nearby` - Find nearby vehicles

### Safety Alerts API
- `POST /alerts` - Create a new safety alert
- `GET /alerts` - Get all active alerts
- `GET /alerts/area/{geohashPrefix}` - Get alerts by area
- `GET /alerts/nearby` - Get alerts near a location
- `PUT /alerts/{alertId}/status` - Update alert status
- `PUT /alerts/{alertId}/verify` - Verify an alert

### Emergency API
- `POST /emergency` - Report an emergency
- `GET /emergency` - Get all active emergencies
- `GET /emergency/user/{userId}` - Get user emergencies
- `GET /emergency/trip/{tripId}` - Get trip emergencies
- `PUT /emergency/{emergencyId}/resolve` - Resolve an emergency

### WebSocket Endpoints
- `/ws/tracking` - WebSocket endpoint for tracking updates
- `/ws/notifications` - WebSocket endpoint for notifications

## WebSocket Topics
- `/topic/vehicle/{vehicleId}` - Updates for a specific vehicle
- `/topic/trip/{tripId}` - Updates for a specific trip
- `/topic/alerts/area/{geohashPrefix}` - Alerts in a specific area
- `/topic/emergency` - Emergency broadcasts
- `/user/{userId}/queue/trips` - User-specific trip updates
- `/user/{userId}/queue/emergency` - User-specific emergency notifications

## GeoHash Implementation
The service uses GeoHash to convert latitude/longitude coordinates into string representations for optimized spatial indexing:

1. **Encoding**: Converts coordinates to GeoHash strings
   ```
   Example: (-1.286389, 36.817223) → "kzf0b3"
   ```

2. **Proximity Search**: Finds nearby vehicles/alerts by matching GeoHash prefixes
   ```
   Prefix "kzf0" would match all locations within that cell
   ```

3. **Neighbor Cells**: Accounts for edge cases by including neighboring GeoHash cells
   ```
   For a cell "kzf0b3", also check "kzf0b2", "kzf0b6", etc.
   ```

## Running the Service
1. **Prerequisites**:
   - Java 17
   - Maven
   - PostgreSQL
   - Redis
   - Kafka (optional for event streaming)

2. **Configuration**:
   The service configuration is in `application.yml`. Key settings:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://localhost:5432/realtime_db
     redis:
       host: localhost
       port: 6379
   
   realtime:
     geohash:
       default-precision: 7
     tracking:
       proximity-search-radius-km: 2
   ```

3. **Starting the Service**:
   ```bash
   # Independently
   cd realtime-service
   mvn spring-boot:run
   
   # With all services
   ./build-and-run-all.sh
   ```

4. **Using Docker**:
   ```bash
   docker-compose up realtime-service
   ```

## Integration with Other Services
- **API Gateway**: Routes requests to the Realtime Service
- **Discovery Service**: Registers the Realtime Service for service discovery
- **Config Server**: Provides centralized configuration
- **Booking Service**: Integrates for trip creation and updates
- **User Service**: Accesses user information for notifications

## Database Schema
The service uses the following tables:
- `vehicle_locations`: Stores vehicle location history with GeoHash indexing
- `trip_tracking`: Manages active and completed trips
- `safety_alerts`: Stores safety alerts with spatial indexing
- `emergency_cases`: Handles emergency reports and resolutions

## Security
All endpoints can be secured with JWT authentication through the API Gateway. The service expects a user ID to be available for user-specific operations.

## Testing
To test the WebSocket functionality, you can use tools like WebSocket King or the browser's built-in WebSocket API. For example:

```javascript
// Connect to WebSocket
const socket = new WebSocket('ws://localhost:8083/ws/tracking');

// Listen for messages
socket.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Received:', data);
};

// Send a location update
socket.send(JSON.stringify({
  type: 'LOCATION_UPDATE',
  payload: {
    vehicleId: '550e8400-e29b-41d4-a716-446655440000',
    latitude: -1.286389,
    longitude: 36.817223,
    speed: 45.0
  }
}));
```

## Monitoring
The service exposes metrics through Spring Boot Actuator endpoints:
- Health status: `/actuator/health`
- Metrics: `/actuator/metrics`
- Info: `/actuator/info`
