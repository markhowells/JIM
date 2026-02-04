# JIM Browser UI - Changes Summary

## Branch: `meh_refactor`

This document summarizes the changes made to implement and optimize the browser-based UI for JIM (Jim's Isochrone Method) marine route optimization.

---

## Issues Addressed

### 1. Route Not Following Optimal Path (SW First)

**Problem:** The computed route was heading directly SE to Cherbourg instead of first heading SW to take advantage of wind and tide conditions.

**Root Cause:** `MapService.parseRoute()` was not loading GRIB wind/tide/wave forecast files. It only created a `Prevailing` wind object with constant values, ignoring the actual forecast data that the desktop application uses.

**Solution:** Updated `MapService.parseRoute()` to:
- Parse "Using Wind:", "Using Tide:", "Using Waves:" directives from route files
- Load actual GRIB data via `SailDocs` and `Tide` classes
- Result: Route chain length changed from 118 to 73 segments (faster, more optimal route)

**Files Modified:**
- `src/uk/co/sexeys/ui/web/MapService.java`

---

### 2. Wave Heights Displaying as Huge Numbers

**Problem:** Wave heights in the info panel displayed as enormous 25-digit numbers (e.g., `99999e20`).

**Root Cause:** GRIB files use `99999e20f` as an UNDEFINED sentinel value (defined in `Grib1RecordBDS.java`). This value was being displayed directly without validation.

**Solution:** Added validation checks in multiple locations:
- `MapService.java`: Check `height < 0 || height > 1e10 || Float.isNaN(height) || Float.isInfinite(height)` before displaying; show "N/A" for invalid values
- `Waves.java`: Added same validation in `render()` and `draw()` methods before drawing red warning squares for high waves

**Files Modified:**
- `src/uk/co/sexeys/ui/web/MapService.java`
- `src/uk/co/sexeys/Waves.java`

---

### 3. Sluggish UI Performance

**Problem:** The browser UI was very laggy even on a fully powered desktop. Target platform is Raspberry Pi 5.

**Analysis Findings:**
- Render responses: 280-600KB per request
- Pan operations during drag: 18-72 MB/sec network traffic
- No request throttling or caching
- Full re-render on every mouse movement

**Solutions Implemented:**

#### Phase 1.1: Pan/Zoom Request Optimization
- Replaced per-event fetch calls with delta accumulation pattern
- Added `panInFlight` / `zoomInFlight` flags to prevent concurrent requests
- While request is in-flight, deltas continue accumulating
- On response, loop sends accumulated deltas until caught up

#### Phase 1.2: HTTP Response Compression
- Created `application.properties` with gzip compression enabled
- Configured for JSON responses over 1KB
- Expected 60-80% reduction in response sizes

#### Phase 1.3: Hover Throttling
- Added 150ms throttle on hover info requests
- Reduced from ~60 req/sec to ~7 req/sec

**Files Modified/Created:**
- `src/main/resources/static/index.html` - Refactored pan/zoom/hover handlers
- `src/main/resources/application.properties` - New file with compression settings

---

### 4. Pan/Zoom Delta Loss Bug

**Problem:** After initial optimization attempt, large mouse drags resulted in tiny (few pixel) pan movements.

**Root Cause:** The `requestAnimationFrame` throttling approach was resetting accumulated deltas before checking if a request could be sent, causing deltas to be lost when requests were in-flight.

**Solution:** Removed RAF-based throttling entirely. Simplified to:
1. Accumulate deltas on every mouse event
2. Call `sendPan()` immediately
3. If already in-flight, function returns (deltas keep accumulating)
4. On request completion, while-loop drains any accumulated deltas

**Files Modified:**
- `src/main/resources/static/index.html`

---

### 5. Config File Editor

**Feature Request:** Add a UI for creating/editing config files with ability to:
- Select start and destination points by clicking on the chart
- Enter coordinates manually in Lat/Lon format
- Choose/load config files through the UI
- Select GRIB files for wind/tide/waves
- Select boat polar data

**Implementation:**

#### Backend API (`/api/config`)
- `GET /list` - List available config YAML files
- `GET /current` - Get currently loaded configuration
- `GET /{name}` - Load a specific config file
- `POST /{name}` - Save a config file
- `POST /apply` - Apply config and recalculate route
- `GET /grib-files` - List available GRIB/NetCDF files
- `GET /polars` - List available polar directories
- `POST /screen-to-coords` - Convert screen click to lat/lon coordinates

#### Frontend UI
- Sliding config panel (toggle via "Config" button in header)
- Form fields for departure position, time, destination
- Click-to-select mode for picking points on the map
- Weather file management (add/remove GRIB files)
- Polar selection dropdown
- Save/Load config file controls
- Apply button to recalculate route with new settings

**Files Created:**
- `src/uk/co/sexeys/ui/web/ConfigController.java` - REST API endpoints
- `src/uk/co/sexeys/ui/web/ConfigService.java` - Config management logic

**Files Modified:**
- `src/uk/co/sexeys/ui/web/MapService.java` - Added `reinitialize()` and `screenToLatLon()` methods
- `src/main/resources/static/index.html` - Added config editor UI and JavaScript

---

## Current Status

### Working
- Route computation with GRIB forecast data
- Wave height validation and display
- HTTP compression enabled
- Simplified pan/zoom delta handling
- Config file editor with click-to-select functionality

### Known Issues
- Pan/zoom still reported as laggy/unpredictable
- May require architectural changes (see Future Work)

---

## Commits in Branch

| Commit | Description |
|--------|-------------|
| `a03a764` | Improved pan/scroll - added compression, refactored event handlers |
| `7b0dd67` | First browser based render. Some features are missing |
| `9791d19` | Split out display code from compute logic. Added Renderer base class |
| `75c16b6` | Tidied up. Replace println with slf4j logging |
| `46e9036` | Add GRIB file support for wind and wave data |

---

## Uncommitted Changes

| File | Status | Description |
|------|--------|-------------|
| `src/uk/co/sexeys/WVS.java` | Modified | Pending changes |
| `src/main/resources/static/index-leaflet.html` | New | Leaflet.js prototype |
| `src/uk/co/sexeys/ui/web/GeoJsonController.java` | New | GeoJSON API endpoint |
| `src/uk/co/sexeys/ui/web/GeoJsonService.java` | New | GeoJSON generation service |

---

## Future Work (From Performance Plan)

### Phase 2: Layer-Based Rendering API
- Separate static layers (shoreline, charts) from dynamic (route, wind, boat)
- Add viewport culling to skip off-screen elements
- Implement adaptive grid density based on zoom level

### Phase 3: Leaflet.js Migration
- Replace canvas-based rendering with Leaflet.js map library
- Use GeoJSON for vector data
- WebSocket for real-time updates
- Leverage Leaflet's built-in tile caching and pan/zoom

---

## File Reference

### Modified Files
- `src/uk/co/sexeys/ui/web/MapService.java` - GRIB loading, wave validation, reinitialize(), screenToLatLon()
- `src/uk/co/sexeys/Waves.java` - Wave height validation in render methods
- `src/main/resources/static/index.html` - Pan/zoom optimization, hover throttling, config editor UI

### New Files
- `src/main/resources/application.properties` - Spring Boot config with compression
- `src/uk/co/sexeys/ui/web/ConfigController.java` - Config management REST API
- `src/uk/co/sexeys/ui/web/ConfigService.java` - Config management business logic
- `src/main/resources/static/index-leaflet.html` - Leaflet.js prototype (untracked)
- `src/uk/co/sexeys/ui/web/GeoJsonController.java` - GeoJSON endpoints (untracked)
- `src/uk/co/sexeys/ui/web/GeoJsonService.java` - GeoJSON service (untracked)

### Plan Document
- `C:\Users\mark\.claude\plans\sleepy-sniffing-honey.md` - Detailed performance optimization plan
