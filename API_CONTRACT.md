# JIM Routing Service API Contract

## Document Status
**Version:** 0.1 (Draft)
**Status:** Preliminary design - not yet implemented
**Date:** January 2026

---

## 1. Executive Summary

This document outlines a proposed REST/WebSocket API for the JIM (Jim's Isochrone Method) marine route optimization engine. The goal is to decouple the core routing algorithms from the presentation layer, enabling:

- Multi-user concurrent access via web browsers
- Mobile application integration
- Third-party system integration
- Horizontal scaling for computation-intensive operations

---

## 2. Merits and Pitfalls

### 2.1 Merits

**Multi-user Support**
- Multiple users can compute routes simultaneously
- Each session maintains independent state
- Server resources shared efficiently across users

**Platform Independence**
- Browser-based UI requires no Java installation for end users
- Mobile apps (iOS/Android) can use the same API
- Desktop applications remain viable as API clients
- Existing Swing UI can be retained as a "thick client" option

**Scalability**
- Stateless request handling enables horizontal scaling
- Long-running computations can be distributed across worker nodes
- Weather/tide data can be cached and shared across users

**Maintainability**
- Clear separation of concerns (routing logic vs presentation)
- API versioning allows backward-compatible evolution
- Independent deployment of frontend and backend
- Easier testing of core algorithms in isolation

**Integration Opportunities**
- Embed routing in other marine applications
- Provide routing-as-a-service to third parties
- Integration with AIS, weather services, fleet management systems

### 2.2 Pitfalls

**Complexity Increase**
- Two codebases to maintain (server + client)
- Network latency added to all operations
- Session state management becomes explicit concern
- Authentication/authorization required for multi-user

**Real-time Interaction Challenges**
- Current UI provides immediate feedback on mouse movements
- WebSocket needed for responsive pan/zoom/hover operations
- "Continuous" optimization mode requires streaming updates
- Risk of degraded user experience if network is slow

**Data Volume Concerns**
- Weather grids, tidal streams, bathymetry are large datasets
- Transferring full route geometry for every update is expensive
- Need efficient delta/tile-based protocols
- Client-side caching strategy required

**State Management Complexity**
- Route computation is iterative (JIM expands over time)
- User may want to "undo" or branch from earlier state
- Long-running computations need cancellation support
- Session timeout/recovery adds complexity

**Deployment Overhead**
- Requires application server (Tomcat, Jetty, etc.)
- SSL certificates for production
- Monitoring, logging, health checks
- Container orchestration for scaling (Docker, Kubernetes)

**Development Effort**
- Significant refactoring of existing codebase
- New skills required (REST design, WebSocket, frontend frameworks)
- Testing across network boundaries
- Performance tuning for concurrent access

### 2.3 Recommended Approach

Given the significant effort required, we recommend a **phased approach**:

1. **Phase 1 (Current):** Continue with tightly-coupled browser UI as proof of concept
   - Use rendering abstraction layer already in progress
   - Validate that core algorithms work in browser context
   - Minimal architectural changes

2. **Phase 2:** Extract service layer within monolith
   - Create internal service interfaces
   - Separate computation from presentation logic
   - Prepare for future API extraction

3. **Phase 3:** Expose REST/WebSocket API
   - Implement endpoints defined in this contract
   - Deploy as standalone service
   - Develop lightweight browser client

---

## 3. API Overview

### 3.1 Base URL
```
https://{host}/api/v1
```

### 3.2 Authentication
```
Authorization: Bearer {jwt-token}
```

Sessions are stateful; a session ID is returned on authentication and must be included in subsequent requests.

### 3.3 Content Types
- Request: `application/json`
- Response: `application/json`
- Geospatial data: GeoJSON (RFC 7946)

---

## 4. Core Endpoints

### 4.1 Session Management

#### POST /session
Create a new routing session.

**Request:**
```json
{
  "polar": "IMOCA60",
  "config": {
    "useWater": true,
    "useIceZone": false,
    "crossDateLine": false
  }
}
```

**Response:**
```json
{
  "sessionId": "uuid-string",
  "expiresAt": "2026-01-20T15:30:00Z",
  "polar": {
    "name": "IMOCA60",
    "maxSpeed": 25.5,
    "angles": [0, 30, 45, 60, 75, 90, 110, 130, 150, 180]
  }
}
```

#### DELETE /session/{sessionId}
Terminate a session and release resources.

---

### 4.2 Route Computation

#### POST /session/{sessionId}/route
Define and compute a new route.

**Request:**
```json
{
  "departure": {
    "position": { "lat": 50.5775, "lon": -2.4072 },
    "time": "2025-11-25T15:00:00Z"
  },
  "destination": {
    "position": { "lat": 49.6733, "lon": -1.6567 },
    "radius": 370
  },
  "expand": {
    "distance": 37040,
    "bins": 360,
    "timeStep": 360000
  },
  "obstructions": [
    {
      "name": "Mid Channel",
      "polygon": [
        { "lat": 49.7708, "lon": -2.8375 },
        { "lat": 50.0586, "lon": -2.9561 },
        { "lat": 50.1428, "lon": -2.4706 },
        { "lat": 49.8547, "lon": -2.3508 },
        { "lat": 49.7708, "lon": -2.8375 }
      ]
    }
  ],
  "cutoffHours": 100
}
```

**Response:**
```json
{
  "routeId": "uuid-string",
  "status": "computing",
  "progress": 0,
  "estimatedCompletion": "2026-01-20T14:35:00Z"
}
```

#### GET /session/{sessionId}/route/{routeId}
Get current route state and results.

**Response:**
```json
{
  "routeId": "uuid-string",
  "status": "complete",
  "progress": 100,
  "result": {
    "duration": 43200000,
    "distance": 185000,
    "arrivalTime": "2025-11-26T03:00:00Z",
    "track": {
      "type": "Feature",
      "geometry": {
        "type": "LineString",
        "coordinates": [
          [-2.4072, 50.5775],
          [-2.3500, 50.4000],
          [-1.9000, 49.9500],
          [-1.6567, 49.6733]
        ]
      },
      "properties": {
        "times": ["2025-11-25T15:00:00Z", "2025-11-25T19:00:00Z", "2025-11-25T23:00:00Z", "2025-11-26T03:00:00Z"],
        "speeds": [8.5, 9.2, 7.8],
        "headings": [145, 158, 162]
      }
    },
    "isochrones": [
      {
        "time": "2025-11-25T16:00:00Z",
        "geometry": {
          "type": "LineString",
          "coordinates": [...]
        }
      }
    ],
    "waypoints": [
      {
        "position": { "lat": 50.4000, "lon": -2.3500 },
        "time": "2025-11-25T19:00:00Z",
        "twa": 125,
        "tws": 15.5,
        "sog": 9.2,
        "heading": 145
      }
    ]
  }
}
```

#### DELETE /session/{sessionId}/route/{routeId}
Cancel a running computation.

---

### 4.3 Route Refinement (Differential Evolution)

#### POST /session/{sessionId}/route/{routeId}/optimize
Run differential evolution optimization on computed route.

**Request:**
```json
{
  "legs": 8,
  "agents": 80,
  "iterations": 100,
  "crossoverRate": 0.9,
  "startTime": "2025-11-25T15:00:00Z",
  "endTime": "2025-11-25T20:00:00Z"
}
```

**Response:**
```json
{
  "optimizationId": "uuid-string",
  "status": "running",
  "progress": 0
}
```

#### GET /session/{sessionId}/route/{routeId}/optimize/{optimizationId}
Get optimization status and results.

---

### 4.4 Environmental Data

#### GET /weather
Get wind data for a viewport.

**Query Parameters:**
- `north`, `south`, `east`, `west` - bounding box (degrees)
- `time` - ISO 8601 timestamp
- `resolution` - grid spacing in degrees (default: 1.0)

**Response:**
```json
{
  "time": "2025-11-25T15:00:00Z",
  "source": "GFS",
  "grid": {
    "north": 51.0,
    "south": 49.0,
    "east": 0.0,
    "west": -3.0,
    "resolution": 0.5,
    "data": [
      {
        "lat": 51.0,
        "lon": -3.0,
        "u": 5.2,
        "v": -3.1,
        "speed": 6.1,
        "direction": 239
      }
    ]
  }
}
```

#### GET /tides
Get tidal current data for a viewport.

**Query Parameters:**
- `north`, `south`, `east`, `west` - bounding box
- `time` - ISO 8601 timestamp

**Response:**
```json
{
  "time": "2025-11-25T15:00:00Z",
  "streams": [
    {
      "position": { "lat": 50.5, "lon": -2.5 },
      "u": 0.8,
      "v": 0.3,
      "speed": 0.85,
      "direction": 69
    }
  ]
}
```

#### GET /waves
Get wave height data for a viewport.

**Query Parameters:**
- `north`, `south`, `east`, `west` - bounding box
- `time` - ISO 8601 timestamp

**Response:**
```json
{
  "time": "2025-11-25T15:00:00Z",
  "grid": {
    "resolution": 0.5,
    "data": [
      {
        "lat": 50.5,
        "lon": -2.5,
        "height": 2.3,
        "period": 8.5,
        "direction": 270
      }
    ]
  }
}
```

---

### 4.5 Reference Data

#### GET /polars
List available polar diagrams.

**Response:**
```json
{
  "polars": [
    { "id": "IMOCA60", "name": "IMOCA 60", "description": "Open 60 class yacht" },
    { "id": "Class40", "name": "Class 40", "description": "Class 40 racing yacht" },
    { "id": "Clipper70", "name": "Clipper 70", "description": "Clipper Race yacht" }
  ]
}
```

#### GET /polars/{polarId}
Get detailed polar data.

**Response:**
```json
{
  "id": "IMOCA60",
  "name": "IMOCA 60",
  "windSpeeds": [4, 6, 8, 10, 12, 14, 16, 20, 25, 30],
  "angles": [0, 30, 45, 60, 75, 90, 110, 130, 150, 180],
  "speeds": [
    [0, 2.1, 3.5, 4.8, 5.9, 6.8, 7.2, 6.9, 6.5, 6.0],
    ...
  ],
  "vmgAngles": {
    "upwind": [42, 42, 41, 40, 39, 38, 38, 37, 36, 35],
    "downwind": [145, 148, 150, 152, 155, 158, 160, 162, 165, 168]
  }
}
```

#### GET /tide-stations
List available tide stations.

**Query Parameters:**
- `north`, `south`, `east`, `west` - bounding box

**Response:**
```json
{
  "stations": [
    {
      "id": "DOVER",
      "name": "Dover",
      "position": { "lat": 51.1167, "lon": 1.3167 }
    }
  ]
}
```

---

## 5. WebSocket API

### 5.1 Connection
```
wss://{host}/api/v1/ws?sessionId={sessionId}
```

### 5.2 Message Types

#### Client → Server

**Subscribe to route updates:**
```json
{
  "type": "subscribe",
  "channel": "route",
  "routeId": "uuid-string"
}
```

**Subscribe to environmental data:**
```json
{
  "type": "subscribe",
  "channel": "weather",
  "bounds": { "north": 51, "south": 49, "east": 0, "west": -3 }
}
```

**Unsubscribe:**
```json
{
  "type": "unsubscribe",
  "channel": "route",
  "routeId": "uuid-string"
}
```

#### Server → Client

**Route computation progress:**
```json
{
  "type": "progress",
  "routeId": "uuid-string",
  "progress": 45,
  "currentTime": "2025-11-25T18:00:00Z",
  "agentCount": 1250
}
```

**Route computation complete:**
```json
{
  "type": "complete",
  "routeId": "uuid-string",
  "result": { ... }
}
```

**Optimization iteration:**
```json
{
  "type": "optimization",
  "optimizationId": "uuid-string",
  "iteration": 50,
  "bestFitness": 43150000,
  "currentTrack": { ... }
}
```

---

## 6. Error Handling

### 6.1 Error Response Format
```json
{
  "error": {
    "code": "ROUTE_NOT_FOUND",
    "message": "Route with ID xyz not found",
    "details": { ... }
  }
}
```

### 6.2 Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| SESSION_EXPIRED | 401 | Session has timed out |
| SESSION_NOT_FOUND | 404 | Invalid session ID |
| ROUTE_NOT_FOUND | 404 | Invalid route ID |
| COMPUTATION_FAILED | 500 | Route computation failed |
| INVALID_COORDINATES | 400 | Coordinates out of range |
| MISSING_WEATHER_DATA | 503 | Weather data unavailable for requested time |
| POLAR_NOT_FOUND | 404 | Unknown polar type |

---

## 7. Data Formats

### 7.1 Positions
All positions use WGS84 (EPSG:4326):
- Latitude: degrees, positive north (-90 to +90)
- Longitude: degrees, positive east (-180 to +180)

### 7.2 Times
All times are ISO 8601 format in UTC:
```
2025-11-25T15:00:00Z
```

### 7.3 Distances
- Distances in meters (SI units)
- Speeds in meters/second
- For display, clients convert to nautical miles/knots

### 7.4 Angles
- Degrees true (0-360)
- 0 = North, 90 = East

---

## 8. Rate Limits

| Endpoint | Limit |
|----------|-------|
| POST /route | 10/minute |
| POST /optimize | 5/minute |
| GET /weather | 60/minute |
| WebSocket messages | 100/second |

---

## 9. Versioning

API version is included in the URL path (`/api/v1/`).

Breaking changes will increment the major version. Deprecated endpoints will be supported for at least 6 months after deprecation announcement.

---

## 10. Future Considerations

### 10.1 Potential Additions
- **Fleet management:** Multiple boats, race tracking
- **Route comparison:** Compare alternatives side-by-side
- **Historical replay:** Analyze past voyages
- **AIS integration:** Real-time vessel positions
- **Collaborative planning:** Shared route editing

### 10.2 Performance Optimizations
- Tile-based weather/bathymetry delivery
- Delta updates for route modifications
- Computation result caching
- Geographic data compression (Protocol Buffers, MessagePack)

---

## Appendix A: GeoJSON Examples

### Route Track
```json
{
  "type": "Feature",
  "geometry": {
    "type": "LineString",
    "coordinates": [
      [-2.4072, 50.5775],
      [-2.3500, 50.4000],
      [-1.9000, 49.9500],
      [-1.6567, 49.6733]
    ]
  },
  "properties": {
    "name": "Portland to Cherbourg",
    "departure": "2025-11-25T15:00:00Z",
    "arrival": "2025-11-26T03:00:00Z",
    "distance": 185000
  }
}
```

### Obstruction Polygon
```json
{
  "type": "Feature",
  "geometry": {
    "type": "Polygon",
    "coordinates": [[
      [-2.8375, 49.7708],
      [-2.9561, 50.0586],
      [-2.4706, 50.1428],
      [-2.3508, 49.8547],
      [-2.8375, 49.7708]
    ]]
  },
  "properties": {
    "name": "Mid Channel Exclusion Zone"
  }
}
```
