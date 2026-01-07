# Booking Service - Point-to-Point Travel Management System

## Overview
The Booking Service is a sophisticated Spring Boot microservice for the Point-to-Point Travel Management System, specifically designed for Kenya's transportation sector. It features advanced **Uber-style ETA calculations**, graph-based routing, real-time traffic processing, and intelligent recommendations for various transport providers (Matatus, BRT, Taxis, Motorbikes, and EVs).

## 🚀 Advanced Features

### 🧠 Uber-Style ETA Calculation System
- **Graph-Based Routing**: Uses directed weighted graphs representing road networks with partitioned optimization
- **Real-Time Traffic Integration**: Processes live GPS data from drivers via Kafka streams
- **Kalman Filter + Viterbi Algorithm**: Advanced map matching for accurate GPS positioning
- **Pre-computed Path Optimization**: Reduces computation complexity from O(n²) to near O(1) for common routes
- **Alternative Route Suggestions**: K-shortest paths algorithm for route alternatives

### 📊 Traffic Intelligence
- **Real-Time Traffic Data**: Kafka-based processing of driver location updates
- **Historical Speed Learning**: Continuously evolving baseline speeds from traffic patterns
- **Predictive Traffic Modeling**: Time-of-day and day-of-week traffic predictions
- **Congestion Level Detection**: Automatic traffic condition assessment (Free Flow to Severe Congestion)
- **Dynamic Edge Weighting**: Traffic-adjusted travel times for accurate ETAs

### 🎯 Smart Recommendations
- **Route Recommendations**: ML-driven suggestions based on user history and preferences
- **Provider Recommendations**: Personalized provider suggestions using usage patterns
- **Optimal Departure Times**: Time recommendations for cost savings and traffic avoidance
- **Alternative Route Analysis**: Multiple route options with reasoning and benefits

### 🗺️ Graph-Based Navigation
- **Partitioned Road Networks**: City divided into geographic partitions for scalable routing
- **Dijkstra Optimization**: Modified algorithms optimized for real-time traffic data
- **Cross-Partition Routing**: Meta-graph approach for long-distance route planning
- **GeoHash Integration**: Efficient spatial indexing for location-based operations

## Core Functionality
- **Route Optimization**: Multi-factor intelligent route finding (distance, fare, provider ratings, traffic)
- **Dynamic Fare Calculation**: Real-time pricing with surge multipliers and traffic considerations
- **Complete Booking Workflow**: End-to-end booking processing from request to completion
- **Multi-Provider Support**: Unified interface for various transport provider types
- **Real-Time Vehicle Tracking**: Live vehicle availability and location monitoring
- **Driver Management**: Comprehensive driver profiles with ratings and performance tracking

## Integration Features
- **Advanced Kafka Processing**: 
  - Consumes `booking-requested` and `driver-locations` events
  - Publishes `booking-processed`, `trip-started`, and `trip-completed` events
- **GraphQL API**: Advanced querying capabilities with real-time data
- **RESTful APIs**: Comprehensive internal and external APIs
- **Intelligent Caching**: Multi-layer caching with Redis and in-memory optimization
- **Service Discovery**: Full Eureka integration with health monitoring

## 🏗️ Advanced Architecture

### Graph-Based Road Network
```
City Map → Directed Weighted Graph → Partitioned Subgraphs → Pre-computed Paths
```

### Real-Time Data Flow
```
GPS Data → Kafka → Traffic Processing → Edge Weight Updates → ETA Recalculation
```

### Database Schema
Enhanced entities with spatial and temporal data:
- **Providers**: Transport service providers with ratings and performance metrics
- **Vehicles**: Real-time vehicle tracking with GPS coordinates and status
- **Drivers**: Driver profiles with ratings, license info, and performance history
- **Routes**: Advanced route definitions with traffic-aware segments
- **Trips**: Enhanced trip tracking with real-time ETA updates and rating system
- **Trip Legs**: Multi-modal journey support with transfer optimization
- **Traffic Segments**: Real-time traffic data storage with historical analysis

### Event-Driven Architecture
- **Consumes**: 
  - `booking-requested` events from Django Booking Service
  - `driver-locations` events for real-time traffic analysis
- **Publishes**: 
  - `booking-processed` events with accurate ETAs
  - `trip-started` events with real-time tracking
  - `trip-completed` events with performance metrics

## 🛠️ API Endpoints

### Enhanced Booking APIs
- `POST /bookings/internal/process` - Process booking with real-time ETA calculation
- `GET /bookings/internal/{bookingId}` - Get booking details with live updates
- `GET /bookings/internal/user/{userId}` - Get user bookings with trip history
- `PUT /bookings/internal/{tripId}/status` - Update trip status with location tracking

### Advanced ETA & Traffic APIs
- `POST /bookings/internal/eta/calculate` - Calculate initial ETA with traffic analysis
- `POST /bookings/internal/eta/update` - Real-time ETA updates during trip
- `GET /bookings/internal/traffic/conditions/{partition}` - Live traffic conditions

### Smart Recommendation APIs
- `POST /bookings/internal/recommendations/routes` - Personalized route suggestions
- `POST /bookings/internal/recommendations/providers` - Provider recommendations
- `POST /bookings/internal/recommendations/times` - Optimal departure time suggestions

### Route & Provider APIs
- `POST /routes/search` - Advanced route search with traffic awareness
- `GET /providers` - Provider listings with real-time availability
- `GET /providers/vehicles/available` - Live vehicle availability with locations

### Enhanced GraphQL Queries
```graphql
query AdvancedRouteQuery {
  routes(
    startLocation: "Nairobi CBD"
    endLocation: "JKIA"
    providerType: "TAXI"
    departureTime: "2025-08-07T14:30:00"
  ) {
    id
    estimatedTimeMinutes
    confidence
    distanceKm
    baseFare
    surgeMultiplier
    trafficConditions
    alternativeRoutes {
      estimatedTimeMinutes
      reason
    }
    provider {
      name
      rating
      realTimeRating
    }
  }
}
```

## ⚙️ Advanced Configuration

### Application Properties
```yaml
spring:
  application:
    name: booking-service
  config:
    import: optional:configserver:https://config-p2p-travel.onrender.com
  kafka:
    consumer:
      topics: booking-requested,driver-locations
    producer:
      topics: booking-processed,trip-started,trip-completed
  cache:
    type: redis
    redis:
      time-to-live: 600000
server:
  port: 8084
eureka:
  client:
    service-url:
      defaultZone: https://eureka-p2p-travel.onrender.com/eureka/
```

### Environment Variables
- `DATABASE_URL`: PostgreSQL connection string
- `DATABASE_USERNAME`: Database username  
- `DATABASE_PASSWORD`: Database password
- `REDIS_HOST`: Redis server host for caching
- `REDIS_PORT`: Redis server port
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka server addresses for real-time data
- `TRAFFIC_UPDATE_INTERVAL`: Traffic data refresh interval (default: 300000ms)

## 📈 Advanced Algorithms

### ETA Calculation Algorithm
1. **Graph Vertex Mapping**: Convert coordinates to road network vertices
2. **Traffic Weight Retrieval**: Get real-time traffic data for route segments
3. **Partitioned Pathfinding**: Use pre-computed paths within partitions
4. **Cross-Partition Routing**: Meta-graph approach for long routes
5. **Confidence Scoring**: Calculate ETA reliability based on data quality

### Route Optimization Scoring
```
Final Score = (Distance Score × 0.3) + (Fare Score × 0.3) + (Provider Rating × 0.2) + 
              (Traffic Score × 0.15) + (User Preference × 0.05)
```

### Traffic Analysis Pipeline
1. **GPS Data Ingestion**: Real-time driver location processing
2. **Map Matching**: Kalman Filter + Viterbi algorithm for accuracy
3. **Speed Calculation**: Moving average of recent speed samples
4. **Congestion Detection**: Speed ratio analysis for traffic conditions
5. **Historical Learning**: Weighted average updates to baseline speeds

## 💰 Enhanced Fare Calculation

### Dynamic Base Rates (per km in KES)
- **Matatu**: 30 KES/km (with traffic adjustment)
- **BRT**: 25 KES/km (subsidized, fixed rate)
- **Taxi**: 50 KES/km (surge-sensitive)
- **Motorbike**: 40 KES/km (traffic-optimized)
- **EV**: 35 KES/km (eco-friendly discount)

### Intelligent Surge Pricing
- **Traffic-Based Surge**: Dynamic multiplier based on real-time congestion
- **Peak Hours** (7-9 AM, 5-7 PM): 1.5x multiplier
- **Night Hours** (10 PM - 6 AM): 1.3x multiplier
- **Weekend Premium**: 1.2x multiplier
- **Event-Based Surge**: Special event pricing integration

### Smart Special Requests
- Extra luggage: +100 KES
- Child seat: +150 KES
- Wheelchair accessible: +200 KES
- Pet transport: +300 KES
- Express/Priority: +500 KES
- **Traffic Avoidance**: +200 KES (premium routing)

## 🔧 Deployment & Scaling

### Performance Optimizations
- **Graph Partitioning**: Reduces route calculation complexity
- **Intelligent Caching**: Multi-layer caching strategy
- **Connection Pooling**: Optimized database connections
- **Async Processing**: Non-blocking real-time updates
- **Load Balancing**: Eureka-based service discovery

### Monitoring & Observability
- **Real-Time Metrics**: Traffic processing rates, ETA accuracy
- **Performance Dashboards**: Route calculation times, cache hit rates
- **Alert Systems**: Traffic data lag, service degradation warnings
- **Health Endpoints**: Comprehensive service health monitoring

### Scalability Features
- **Horizontal Scaling**: Stateless design with external caching
- **Database Optimization**: Spatial indexing for geographic queries
- **Traffic Data Partitioning**: Distributed traffic processing
- **Cache Warming**: Pre-computation of popular routes

## 🧪 Testing Strategy

### Comprehensive Test Coverage
- **Unit Tests**: Individual component testing (>90% coverage)
- **Integration Tests**: Full workflow testing with real data
- **Performance Tests**: Load testing for ETA calculations
- **Traffic Simulation**: Synthetic traffic data for algorithm validation

### Test Profiles
- `test`: H2 in-memory database with mock traffic data
- `integration`: PostgreSQL with Redis and Kafka containers
- `performance`: Full stack with synthetic load generation

## 📊 Monitoring & Analytics

### Key Performance Indicators
- **ETA Accuracy**: Percentage of ETAs within 10% of actual time
- **Route Optimization**: Average time savings vs. naive routing
- **Traffic Prediction**: Accuracy of congestion forecasts
- **User Satisfaction**: Rating correlation with recommendation quality

### Real-Time Dashboards
- Live traffic conditions across city partitions
- Route calculation performance metrics
- Cache hit rates and optimization effectiveness
- Service health and dependency status

This enhanced booking service represents a state-of-the-art transportation management platform, combining advanced algorithms, real-time data processing, and intelligent recommendations to provide an Uber-level user experience for Kenya's diverse transportation ecosystem.
