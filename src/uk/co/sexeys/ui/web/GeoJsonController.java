package uk.co.sexeys.ui.web;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for GeoJSON-based map API.
 * Provides endpoints for Leaflet-based frontend to fetch map layers as GeoJSON.
 */
@RestController
@RequestMapping("/api/geojson")
@CrossOrigin(origins = "*")
public class GeoJsonController {

    private final GeoJsonService geoJsonService;

    public GeoJsonController(GeoJsonService geoJsonService) {
        this.geoJsonService = geoJsonService;
    }

    /**
     * Initialize endpoint - returns initial viewport and time.
     */
    @GetMapping("/init")
    public InitResponse init() {
        return geoJsonService.getInitData();
    }

    /**
     * Get shoreline as GeoJSON for a bounding box.
     * @param bbox Comma-separated: south,west,north,east
     */
    @GetMapping("/shoreline")
    public Map<String, Object> getShoreline(@RequestParam String bbox) {
        double[] bounds = parseBbox(bbox);
        return geoJsonService.getShorelineGeoJson(bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    /**
     * Get depth contours as GeoJSON for a bounding box.
     * @param bbox Comma-separated: south,west,north,east
     */
    @GetMapping("/depth")
    public Map<String, Object> getDepth(@RequestParam String bbox) {
        double[] bounds = parseBbox(bbox);
        return geoJsonService.getDepthGeoJson(bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    /**
     * Get vector chart data (CMap) as GeoJSON for a bounding box.
     * Includes land, coastline, shallow water, intertidal zones, etc.
     * @param bbox Comma-separated: south,west,north,east
     */
    @GetMapping("/charts")
    public Map<String, Object> getCharts(@RequestParam String bbox) {
        double[] bounds = parseBbox(bbox);
        return geoJsonService.getChartsGeoJson(bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    /**
     * Get computed route as GeoJSON.
     */
    @GetMapping("/route")
    public Map<String, Object> getRoute() {
        return geoJsonService.getRouteGeoJson();
    }

    /**
     * Get waypoints as GeoJSON.
     */
    @GetMapping("/waypoints")
    public Map<String, Object> getWaypoints() {
        return geoJsonService.getWaypointsGeoJson();
    }

    /**
     * Get wind data as JSON array for a bounding box.
     * @param bbox Comma-separated: south,west,north,east
     * @param time Unix timestamp in milliseconds
     */
    @GetMapping("/wind")
    public List<WindPoint> getWind(
            @RequestParam String bbox,
            @RequestParam(defaultValue = "0") long time) {
        double[] bounds = parseBbox(bbox);
        return geoJsonService.getWindData(bounds[0], bounds[1], bounds[2], bounds[3], time);
    }

    /**
     * Get tide/current data as JSON array for a bounding box.
     * @param bbox Comma-separated: south,west,north,east
     * @param time Unix timestamp in milliseconds
     */
    @GetMapping("/tide")
    public List<TidePoint> getTide(
            @RequestParam String bbox,
            @RequestParam(defaultValue = "0") long time) {
        double[] bounds = parseBbox(bbox);
        return geoJsonService.getTideData(bounds[0], bounds[1], bounds[2], bounds[3], time);
    }

    /**
     * Get hover info at a lat/lng position.
     */
    @PostMapping("/hover")
    public HoverResponse hover(@RequestBody HoverRequest request) {
        return geoJsonService.getHoverInfo(request.lat, request.lng, request.time);
    }

    /**
     * Get last route (previous computed route) as GeoJSON.
     * @param time Unix timestamp in milliseconds (for time position marker)
     */
    @GetMapping("/lastroute")
    public Map<String, Object> getLastRoute(@RequestParam(defaultValue = "0") long time) {
        return geoJsonService.getLastRouteGeoJson(time);
    }

    /**
     * Get all JIM agents (routes fan) as GeoJSON.
     */
    @GetMapping("/agents")
    public Map<String, Object> getAgentsFan() {
        return geoJsonService.getAgentsFanGeoJson();
    }

    /**
     * Get obstructions (no-go zones) as GeoJSON.
     */
    @GetMapping("/obstructions")
    public Map<String, Object> getObstructions() {
        return geoJsonService.getObstructionsGeoJson();
    }

    /**
     * Get time position indicator as GeoJSON.
     * @param time Unix timestamp in milliseconds
     */
    @GetMapping("/timeposition")
    public Map<String, Object> getTimePosition(@RequestParam(defaultValue = "0") long time) {
        return geoJsonService.getTimePositionGeoJson(time);
    }

    /**
     * Get great circle reference line as GeoJSON.
     */
    @GetMapping("/greatcircle")
    public Map<String, Object> getGreatCircle() {
        return geoJsonService.getGreatCircleGeoJson();
    }

    /**
     * Parse bounding box string "south,west,north,east" into double array.
     */
    private double[] parseBbox(String bbox) {
        String[] parts = bbox.split(",");
        return new double[] {
            Double.parseDouble(parts[0]), // south
            Double.parseDouble(parts[1]), // west
            Double.parseDouble(parts[2]), // north
            Double.parseDouble(parts[3])  // east
        };
    }

    // DTOs

    public static class InitResponse {
        public MapController.ViewportInfo viewport;
        public long time;
        public String timeFormatted;

        public InitResponse() {}

        public InitResponse(MapController.ViewportInfo viewport, long time, String timeFormatted) {
            this.viewport = viewport;
            this.time = time;
            this.timeFormatted = timeFormatted;
        }
    }

    public static class HoverRequest {
        public double lat;
        public double lng;
        public long time;
    }

    public static class HoverResponse {
        public String position;
        public String wind;
        public String tide;
        public String waves;

        public HoverResponse() {}
    }

    public static class WindPoint {
        public double lat;
        public double lng;
        public double u;  // East component (m/s)
        public double v;  // North component (m/s)

        public WindPoint() {}

        public WindPoint(double lat, double lng, double u, double v) {
            this.lat = lat;
            this.lng = lng;
            this.u = u;
            this.v = v;
        }
    }

    public static class TidePoint {
        public double lat;
        public double lng;
        public double u;  // East component (m/s)
        public double v;  // North component (m/s)

        public TidePoint() {}

        public TidePoint(double lat, double lng, double u, double v) {
            this.lat = lat;
            this.lng = lng;
            this.u = u;
            this.v = v;
        }
    }
}