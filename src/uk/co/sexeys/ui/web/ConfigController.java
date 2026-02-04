package uk.co.sexeys.ui.web;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

/**
 * REST controller for config file management.
 * Provides endpoints for:
 * - Listing available config files
 * - Loading/saving config files
 * - Getting available GRIB files
 * - Getting available polar files
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    /**
     * List available config files.
     */
    @GetMapping("/list")
    public ListResponse listConfigs() {
        return new ListResponse(configService.listConfigFiles());
    }

    /**
     * Get the current loaded config.
     */
    @GetMapping("/current")
    public ConfigData getCurrentConfig() {
        return configService.getCurrentConfig();
    }

    /**
     * Load a specific config file.
     */
    @GetMapping("/{name}")
    public ResponseEntity<ConfigData> loadConfig(@PathVariable String name) {
        ConfigData config = configService.loadConfig(name);
        if (config == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(config);
    }

    /**
     * Save a config file.
     */
    @PostMapping("/{name}")
    public ResponseEntity<SaveResponse> saveConfig(
            @PathVariable String name,
            @RequestBody ConfigData config) {
        boolean success = configService.saveConfig(name, config);
        return ResponseEntity.ok(new SaveResponse(success, success ? "Saved successfully" : "Failed to save"));
    }

    /**
     * Apply a config and recalculate the route.
     */
    @PostMapping("/apply")
    public ResponseEntity<ApplyResponse> applyConfig(@RequestBody ConfigData config) {
        try {
            configService.applyConfig(config);
            return ResponseEntity.ok(new ApplyResponse(true, "Config applied, route recalculating..."));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApplyResponse(false, "Error: " + e.getMessage()));
        }
    }

    /**
     * List available GRIB files.
     */
    @GetMapping("/grib-files")
    public ListResponse listGribFiles() {
        return new ListResponse(configService.listGribFiles());
    }

    /**
     * List available polar directories.
     */
    @GetMapping("/polars")
    public ListResponse listPolars() {
        return new ListResponse(configService.listPolars());
    }

    /**
     * Convert screen coordinates to lat/lon (for click-to-select).
     */
    @PostMapping("/screen-to-coords")
    public CoordsResponse screenToCoords(@RequestBody ScreenCoordsRequest request) {
        return configService.screenToCoords(request.x, request.y);
    }

    // DTOs

    public static class ListResponse {
        public List<String> items;

        public ListResponse() {}
        public ListResponse(List<String> items) {
            this.items = items;
        }
    }

    public static class SaveResponse {
        public boolean success;
        public String message;

        public SaveResponse() {}
        public SaveResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static class ApplyResponse {
        public boolean success;
        public String message;

        public ApplyResponse() {}
        public ApplyResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    public static class ScreenCoordsRequest {
        public double x;
        public double y;
    }

    public static class CoordsResponse {
        public double lat;
        public double lon;
        public String latDMS;
        public String lonDMS;

        public CoordsResponse() {}
        public CoordsResponse(double lat, double lon, String latDMS, String lonDMS) {
            this.lat = lat;
            this.lon = lon;
            this.latDMS = latDMS;
            this.lonDMS = lonDMS;
        }
    }

    /**
     * Config data transfer object containing all route-relevant settings.
     */
    public static class ConfigData {
        // Route settings
        public String polar;
        public DeparturePoint departure;
        public DestinationPoint destination;
        public List<WeatherFile> weatherFiles;
        public List<ObstructionArea> obstructions;
        public ExpandSettings expand;

        // Display settings
        public String screen;

        public ConfigData() {}
    }

    public static class DeparturePoint {
        public double lat;
        public double lon;
        public String latDMS;
        public String lonDMS;
        public String time;  // "2025/11/25 15:00 UTC" format

        public DeparturePoint() {}
    }

    public static class DestinationPoint {
        public double lat;
        public double lon;
        public String latDMS;
        public String lonDMS;
        public double radiusNm;
        public int bins;
        public double binSizeNm;
        public double stepHours;

        public DestinationPoint() {}
    }

    public static class WeatherFile {
        public String type;  // "Wind", "Tide", "Waves", "Current"
        public String file;

        public WeatherFile() {}
        public WeatherFile(String type, String file) {
            this.type = type;
            this.file = file;
        }
    }

    public static class ObstructionArea {
        public String name;
        public String polygon;  // Semicolon-separated DMS coordinates

        public ObstructionArea() {}
    }

    public static class ExpandSettings {
        public double distanceNm;
        public int bins;
        public double binSizeNm;
        public double stepHours;

        public ExpandSettings() {}
    }
}
