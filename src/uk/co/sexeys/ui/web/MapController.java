package uk.co.sexeys.ui.web;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for the map rendering API.
 * Provides endpoints for:
 * - Getting current render commands
 * - Handling user interactions (pan, zoom, click)
 * - Querying map state
 */
@RestController
@RequestMapping("/api/map")
@CrossOrigin(origins = "*")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    /**
     * Get render commands for the current map state.
     */
    @GetMapping("/render")
    public RenderResponse render(
            @RequestParam(defaultValue = "1200") int width,
            @RequestParam(defaultValue = "800") int height) {
        return mapService.render(width, height);
    }

    /**
     * Pan the map by a delta in screen coordinates.
     */
    @PostMapping("/pan")
    public RenderResponse pan(@RequestBody PanRequest request) {
        mapService.pan(request.deltaX, request.deltaY);
        return mapService.render(request.width, request.height);
    }

    /**
     * Zoom the map centered on a point.
     */
    @PostMapping("/zoom")
    public RenderResponse zoom(@RequestBody ZoomRequest request) {
        mapService.zoom(request.factor, request.centerX, request.centerY);
        return mapService.render(request.width, request.height);
    }

    /**
     * Set the display time.
     */
    @PostMapping("/time")
    public RenderResponse setTime(@RequestBody TimeRequest request) {
        mapService.setTime(request.timestamp);
        return mapService.render(request.width, request.height);
    }

    /**
     * Adjust the display time by a delta.
     */
    @PostMapping("/time/adjust")
    public RenderResponse adjustTime(@RequestBody TimeAdjustRequest request) {
        mapService.adjustTime(request.deltaMinutes);
        return mapService.render(request.width, request.height);
    }

    /**
     * Handle a click on the map.
     */
    @PostMapping("/click")
    public ClickResponse click(@RequestBody ClickRequest request) {
        return mapService.handleClick(request.x, request.y, request.button, request.modifiers);
    }

    /**
     * Handle mouse move (for hover info).
     */
    @PostMapping("/hover")
    public HoverResponse hover(@RequestBody HoverRequest request) {
        return mapService.handleHover(request.x, request.y);
    }

    /**
     * Get current viewport information.
     */
    @GetMapping("/viewport")
    public ViewportInfo getViewport() {
        return mapService.getViewport();
    }

    /**
     * Toggle layer visibility.
     */
    @PostMapping("/layers/{layer}/toggle")
    public RenderResponse toggleLayer(
            @PathVariable String layer,
            @RequestParam(defaultValue = "1200") int width,
            @RequestParam(defaultValue = "800") int height) {
        mapService.toggleLayer(layer);
        return mapService.render(width, height);
    }

    /**
     * Get current time as displayed.
     */
    @GetMapping("/time")
    public TimeInfo getTime() {
        return mapService.getTimeInfo();
    }

    // Request/Response DTOs

    public static class RenderResponse {
        public List<DrawCommand> commands;
        public long timestamp;
        public ViewportInfo viewport;

        public RenderResponse() {}

        public RenderResponse(List<DrawCommand> commands, long timestamp, ViewportInfo viewport) {
            this.commands = commands;
            this.timestamp = timestamp;
            this.viewport = viewport;
        }
    }

    public static class PanRequest {
        public double deltaX;
        public double deltaY;
        public int width;
        public int height;
    }

    public static class ZoomRequest {
        public double factor;
        public double centerX;
        public double centerY;
        public int width;
        public int height;
    }

    public static class TimeRequest {
        public long timestamp;
        public int width;
        public int height;
    }

    public static class TimeAdjustRequest {
        public int deltaMinutes;
        public int width;
        public int height;
    }

    public static class ClickRequest {
        public double x;
        public double y;
        public int button;  // 1=left, 2=middle, 3=right
        public List<String> modifiers;  // "shift", "ctrl", "alt"
    }

    public static class ClickResponse {
        public String action;
        public Map<String, Object> data;

        public ClickResponse() {}

        public ClickResponse(String action, Map<String, Object> data) {
            this.action = action;
            this.data = data;
        }
    }

    public static class HoverRequest {
        public double x;
        public double y;
    }

    public static class HoverResponse {
        public String position;  // Lat/Lon formatted
        public String wind;
        public String tide;
        public String waves;
        public String nearestStation;

        public HoverResponse() {}
    }

    public static class ViewportInfo {
        public double north;
        public double south;
        public double east;
        public double west;
        public double scale;

        public ViewportInfo() {}

        public ViewportInfo(double north, double south, double east, double west, double scale) {
            this.north = north;
            this.south = south;
            this.east = east;
            this.west = west;
            this.scale = scale;
        }
    }

    public static class TimeInfo {
        public long timestamp;
        public String formatted;

        public TimeInfo() {}

        public TimeInfo(long timestamp, String formatted) {
            this.timestamp = timestamp;
            this.formatted = formatted;
        }
    }
}
