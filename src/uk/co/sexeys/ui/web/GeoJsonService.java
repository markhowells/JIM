package uk.co.sexeys.ui.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.sexeys.*;
import uk.co.sexeys.JIM.Agent;
import uk.co.sexeys.JIM.JIM;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.waypoint.InterimFix;
import uk.co.sexeys.waypoint.Waypoint;
import uk.co.sexeys.wind.Wind;
import uk.co.sexeys.CMap.CMap;

import java.util.List;

import java.awt.Polygon;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service that provides GeoJSON data for Leaflet-based frontend.
 * Converts existing WVS, route, and weather data to GeoJSON format.
 */
@Service
public class GeoJsonService {
    private static final Logger logger = LoggerFactory.getLogger(GeoJsonService.class);

    private final MapService mapService;
    private final SimpleDateFormat format = new SimpleDateFormat("EEE yyyy.MM.dd HH:mm:ss zzz");

    // Cached references (obtained from MapService via reflection or direct access)
    private WVS wvs;
    private Wind wind;
    private Water water;
    private Waves waves;
    private JIM jim;
    private Boat boat;
    private CMap cMap;
    private LastRoute lastRoute;

    public GeoJsonService(MapService mapService) {
        this.mapService = mapService;
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Ensure MapService is initialized and get references to data layers.
     */
    private void ensureInitialized() {
        // Force MapService to initialize if not already
        mapService.render(1, 1);

        // Get references via reflection (since MapService doesn't expose them directly)
        try {
            java.lang.reflect.Field wvsField = MapService.class.getDeclaredField("wvs");
            wvsField.setAccessible(true);
            wvs = (WVS) wvsField.get(mapService);

            java.lang.reflect.Field windField = MapService.class.getDeclaredField("wind");
            windField.setAccessible(true);
            wind = (Wind) windField.get(mapService);

            java.lang.reflect.Field waterField = MapService.class.getDeclaredField("water");
            waterField.setAccessible(true);
            water = (Water) waterField.get(mapService);

            java.lang.reflect.Field wavesField = MapService.class.getDeclaredField("waves");
            wavesField.setAccessible(true);
            waves = (Waves) wavesField.get(mapService);

            java.lang.reflect.Field jimField = MapService.class.getDeclaredField("jim");
            jimField.setAccessible(true);
            jim = (JIM) jimField.get(mapService);

            java.lang.reflect.Field boatField = MapService.class.getDeclaredField("boat");
            boatField.setAccessible(true);
            boat = (Boat) boatField.get(mapService);

            java.lang.reflect.Field cMapField = MapService.class.getDeclaredField("cMap");
            cMapField.setAccessible(true);
            cMap = (CMap) cMapField.get(mapService);

            java.lang.reflect.Field lastRouteField = MapService.class.getDeclaredField("lastRoute");
            lastRouteField.setAccessible(true);
            lastRoute = (LastRoute) lastRouteField.get(mapService);

        } catch (Exception e) {
            logger.error("Error accessing MapService fields", e);
        }
    }

    /**
     * Get initial data for the Leaflet frontend.
     * Centers the viewport on the departure waypoint.
     */
    public GeoJsonController.InitResponse getInitData() {
        ensureInitialized();

        MapController.TimeInfo timeInfo = mapService.getTimeInfo();

        // Create viewport centered on the departure waypoint
        MapController.ViewportInfo viewport;
        if (boat != null && boat.waypoints != null && boat.waypoints.length > 0) {
            // Get departure position (in radians, convert to degrees)
            double centerLat = Math.toDegrees(boat.waypoints[0].position.y);
            double centerLon = Math.toDegrees(boat.waypoints[0].position.x);

            // Create a reasonable initial viewport (approx 5 degrees span)
            double latSpan = 2.5;
            double lonSpan = 4.0;

            viewport = new MapController.ViewportInfo(
                (float) (centerLat + latSpan),   // north
                (float) (centerLat - latSpan),   // south
                (float) (centerLon + lonSpan),   // east
                (float) (centerLon - lonSpan),   // west
                0  // scale not used by Leaflet
            );
        } else {
            // Fallback to default viewport from MapService
            viewport = mapService.getViewport();
        }

        return new GeoJsonController.InitResponse(
            viewport,
            timeInfo.timestamp,
            timeInfo.formatted
        );
    }

    /**
     * Get shoreline as GeoJSON FeatureCollection.
     */
    public Map<String, Object> getShorelineGeoJson(double south, double west, double north, double east) {
        ensureInitialized();

        List<Map<String, Object>> features = new ArrayList<>();

        if (wvs == null) {
            return createFeatureCollection(features);
        }

        // Iterate through grid cells in the bounding box
        int startLat = (int) Math.floor(south);
        if (startLat < -90) startLat = -90;
        int endLat = (int) Math.ceil(north);
        if (endLat > 90) endLat = 90;
        int startLon = (int) Math.floor(west);
        if (startLon < -180) startLon = -180;
        int endLon = (int) Math.ceil(east);
        if (endLon > 180) endLon = 180;

        for (int lat = startLat; lat <= endLat; lat++) {
            for (int lon = startLon; lon < endLon; lon++) {
                try {
                    WVS.GridPoint gridPoint = wvs.GetGridPoint(lat, lon);
                    if (gridPoint == null || gridPoint.data.isEmpty()) continue;

                    // Convert each segment to a GeoJSON LineString feature
                    Iterator<Vector2> iter = gridPoint.data.iterator();
                    for (int nPoints : gridPoint.segray) {
                        if (nPoints < 2) {
                            // Skip segment with less than 2 points
                            for (int i = 0; i < nPoints && iter.hasNext(); i++) {
                                iter.next();
                            }
                            continue;
                        }

                        List<double[]> coordinates = new ArrayList<>();
                        for (int i = 0; i < nPoints && iter.hasNext(); i++) {
                            Vector2 point = iter.next();
                            // GeoJSON uses [longitude, latitude] order
                            coordinates.add(new double[]{
                                point.x + Main.ChartOffsetX,
                                point.y + Main.ChartOffsetY
                            });
                        }

                        if (coordinates.size() >= 2) {
                            features.add(createLineStringFeature(coordinates, Map.of("type", "shoreline")));
                        }
                    }
                } catch (Exception e) {
                    // Skip this grid cell on error
                }
            }
        }

        return createFeatureCollection(features);
    }

    /**
     * Get depth contours as GeoJSON (placeholder - would need Depth class access).
     */
    public Map<String, Object> getDepthGeoJson(double south, double west, double north, double east) {
        // TODO: Implement depth contour extraction from Depth class
        // For now, return empty feature collection
        return createFeatureCollection(new ArrayList<>());
    }

    /**
     * Get vector chart data (CMap) as GeoJSON.
     * Includes land, coastline, shallow water, etc.
     */
    public Map<String, Object> getChartsGeoJson(double south, double west, double north, double east) {
        ensureInitialized();

        List<Map<String, Object>> features = new ArrayList<>();

        if (cMap == null || cMap.scaleLevel < 0) {
            return createFeatureCollection(features);
        }

        try {
            // Access the instructions field via reflection
            java.lang.reflect.Field instructionsField = CMap.class.getDeclaredField("instructions");
            instructionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> instructions = (List<Object>) instructionsField.get(cMap);

            logger.info("CMap has {} instructions", instructions.size());

            for (Object instObj : instructions) {
                // Get DrawingInstructions fields via reflection
                Class<?> diClass = instObj.getClass();

                // Get CIB for coordinate transform
                java.lang.reflect.Field cibField = diClass.getField("cib");
                cibField.setAccessible(true);
                Object cib = cibField.get(instObj);
                Class<?> cibClass = cib.getClass();

                java.lang.reflect.Field latMinField = cibClass.getField("lat_min");
                java.lang.reflect.Field latMaxField = cibClass.getField("lat_max");
                java.lang.reflect.Field lonMinField = cibClass.getField("lon_min");
                java.lang.reflect.Field lonMaxField = cibClass.getField("lon_max");
                latMinField.setAccessible(true);
                latMaxField.setAccessible(true);
                lonMinField.setAccessible(true);
                lonMaxField.setAccessible(true);

                double latMin = latMinField.getDouble(cib);
                double latMax = latMaxField.getDouble(cib);
                double lonMin = lonMinField.getDouble(cib);
                double lonMax = lonMaxField.getDouble(cib);

                logger.info("CIB bounds: lat {}..{}, lon {}..{}", latMin, latMax, lonMin, lonMax);

                // Land polygons
                addPolygonFeatures(features, instObj, diClass, "land", latMin, latMax, lonMin, lonMax,
                    Map.of("type", "land", "color", "#F6C96E"));

                // Shallow water
                addPolygonFeatures(features, instObj, diClass, "shallow", latMin, latMax, lonMin, lonMax,
                    Map.of("type", "shallow", "color", "#A2E0EB"));

                // Depth 10m
                addPolygonFeatures(features, instObj, diClass, "depth10m", latMin, latMax, lonMin, lonMax,
                    Map.of("type", "depth10m", "color", "#D7FFFF"));

                // Intertidal
                addPolygonFeatures(features, instObj, diClass, "interTidal", latMin, latMax, lonMin, lonMax,
                    Map.of("type", "intertidal", "color", "#62AC71"));

                // Built-up areas
                addPolygonFeatures(features, instObj, diClass, "builtUp", latMin, latMax, lonMin, lonMax,
                    Map.of("type", "builtup", "color", "#C59C4D"));

                // Coastline paths
                addPathFeatures(features, instObj, diClass, "coastline", latMin, latMax, lonMin, lonMax,
                    Map.of("type", "coastline", "color", "#000000"));
            }
        } catch (Exception e) {
            logger.error("Error extracting chart GeoJSON", e);
        }

        return createFeatureCollection(features);
    }

    /**
     * Helper to add polygon features from DrawingInstructions.
     * CMap local coordinates are linear in Mercator projection space, not lat/lon.
     * We must interpolate in Mercator-y space then convert back to latitude.
     */
    @SuppressWarnings("unchecked")
    private void addPolygonFeatures(List<Map<String, Object>> features, Object instObj, Class<?> diClass,
                                    String fieldName, double latMin, double latMax, double lonMin, double lonMax,
                                    Map<String, Object> properties) {
        try {
            java.lang.reflect.Field field = diClass.getField(fieldName);
            field.setAccessible(true);
            List<Polygon> polygons = (List<Polygon>) field.get(instObj);

            // Convert lat bounds to Mercator-y for proper interpolation
            double yMercatorMin = latToMercatorY(latMin);
            double yMercatorMax = latToMercatorY(latMax);

            for (Polygon poly : polygons) {
                if (poly.npoints < 3) continue;

                List<double[]> coords = new ArrayList<>();
                for (int i = 0; i < poly.npoints; i++) {
                    // Longitude: linear interpolation (Mercator is linear in x)
                    double lng = lonMin + (poly.xpoints[i] / 65535.0) * (lonMax - lonMin);

                    // Latitude: interpolate in Mercator-y space, then convert back
                    double yMercator = yMercatorMin + (poly.ypoints[i] / 65535.0) * (yMercatorMax - yMercatorMin);
                    double lat = mercatorYToLat(yMercator);

                    coords.add(new double[]{lng, lat});
                }
                // Close the polygon
                if (coords.size() > 0) {
                    coords.add(coords.get(0));
                }

                features.add(createPolygonFeature(coords, properties));
            }
        } catch (Exception e) {
            // Field not found or access error - skip
        }
    }

    /**
     * Helper to add path (line) features from DrawingInstructions.
     * CMap local coordinates are linear in Mercator projection space, not lat/lon.
     */
    @SuppressWarnings("unchecked")
    private void addPathFeatures(List<Map<String, Object>> features, Object instObj, Class<?> diClass,
                                 String fieldName, double latMin, double latMax, double lonMin, double lonMax,
                                 Map<String, Object> properties) {
        try {
            java.lang.reflect.Field field = diClass.getField(fieldName);
            field.setAccessible(true);
            List<Path2D> paths = (List<Path2D>) field.get(instObj);

            // Convert lat bounds to Mercator-y for proper interpolation
            double yMercatorMin = latToMercatorY(latMin);
            double yMercatorMax = latToMercatorY(latMax);

            for (Path2D path : paths) {
                List<double[]> coords = new ArrayList<>();
                PathIterator pi = path.getPathIterator(null);
                float[] pathCoords = new float[6];

                while (!pi.isDone()) {
                    int type = pi.currentSegment(pathCoords);
                    if (type == PathIterator.SEG_MOVETO || type == PathIterator.SEG_LINETO) {
                        // Longitude: linear interpolation
                        double lng = lonMin + (pathCoords[0] / 65535.0) * (lonMax - lonMin);

                        // Latitude: interpolate in Mercator-y space, then convert back
                        double yMercator = yMercatorMin + (pathCoords[1] / 65535.0) * (yMercatorMax - yMercatorMin);
                        double lat = mercatorYToLat(yMercator);

                        coords.add(new double[]{lng, lat});
                    }
                    pi.next();
                }

                if (coords.size() >= 2) {
                    features.add(createLineStringFeature(coords, properties));
                }
            }
        } catch (Exception e) {
            // Field not found or access error - skip
        }
    }

    /**
     * Convert latitude (degrees) to Mercator Y coordinate.
     * Uses the standard Web Mercator formula.
     */
    private double latToMercatorY(double lat) {
        double latRad = Math.toRadians(lat);
        return Math.log(Math.tan(Math.PI / 4 + latRad / 2));
    }

    /**
     * Convert Mercator Y coordinate back to latitude (degrees).
     */
    private double mercatorYToLat(double y) {
        return Math.toDegrees(2 * Math.atan(Math.exp(y)) - Math.PI / 2);
    }

    /**
     * Get computed route as GeoJSON.
     */
    public Map<String, Object> getRouteGeoJson() {
        ensureInitialized();

        List<Map<String, Object>> features = new ArrayList<>();

        if (jim == null || jim.keyAgent == null) {
            return createFeatureCollection(features);
        }

        // Build route from agent chain
        List<double[]> routeCoords = new ArrayList<>();
        Agent agent = jim.keyAgent;

        while (agent != null) {
            // Convert radians to degrees
            double lng = Math.toDegrees(agent.position.x);
            double lat = Math.toDegrees(agent.position.y);
            routeCoords.add(0, new double[]{lng, lat}); // Prepend to get chronological order
            agent = agent.previousAgent;
        }

        if (routeCoords.size() >= 2) {
            features.add(createLineStringFeature(routeCoords, Map.of(
                "type", "route",
                "name", "JIM Route"
            )));
        }

        // Add individual route points as markers
        agent = jim.keyAgent;
        int pointIndex = 0;
        while (agent != null) {
            double lng = Math.toDegrees(agent.position.x);
            double lat = Math.toDegrees(agent.position.y);
            features.add(createPointFeature(lng, lat, Map.of(
                "type", "routePoint",
                "index", pointIndex++,
                "time", agent.time
            )));
            agent = agent.previousAgent;
        }

        return createFeatureCollection(features);
    }

    /**
     * Get waypoints as GeoJSON.
     */
    public Map<String, Object> getWaypointsGeoJson() {
        ensureInitialized();

        List<Map<String, Object>> features = new ArrayList<>();

        if (boat == null || boat.waypoints == null) {
            return createFeatureCollection(features);
        }

        for (int i = 0; i < boat.waypoints.length; i++) {
            Waypoint wp = boat.waypoints[i];
            double lng = Math.toDegrees(wp.position.x);
            double lat = Math.toDegrees(wp.position.y);

            String type;
            if (i == 0) {
                type = "depart";
            } else if (i == boat.waypoints.length - 1) {
                type = "destination";
            } else {
                type = "waypoint";
            }

            features.add(createPointFeature(lng, lat, Map.of(
                "type", type,
                "index", i,
                "name", wp.getClass().getSimpleName()
            )));
        }

        return createFeatureCollection(features);
    }

    /**
     * Get LastRoute (previous computed route) as GeoJSON.
     * Dark gray line with waypoint markers.
     */
    public Map<String, Object> getLastRouteGeoJson(long time) {
        ensureInitialized();

        List<Map<String, Object>> features = new ArrayList<>();

        if (lastRoute == null || lastRoute.waypoints.isEmpty()) {
            return createFeatureCollection(features);
        }

        // Build the route line
        List<double[]> routeCoords = new ArrayList<>();
        for (InterimFix w : lastRoute.waypoints) {
            double lng = Math.toDegrees(w.position.x);
            double lat = Math.toDegrees(w.position.y);
            routeCoords.add(new double[]{lng, lat});
        }

        if (routeCoords.size() >= 2) {
            features.add(createLineStringFeature(routeCoords, Map.of(
                "type", "lastRoute",
                "color", "#404040",  // Dark gray
                "weight", 1
            )));
        }

        // Add waypoint markers
        for (int i = 0; i < lastRoute.waypoints.size(); i++) {
            InterimFix w = lastRoute.waypoints.get(i);
            double lng = Math.toDegrees(w.position.x);
            double lat = Math.toDegrees(w.position.y);
            features.add(createPointFeature(lng, lat, Map.of(
                "type", "lastRouteWaypoint",
                "index", i,
                "time", w.time,
                "heading", w.heading,
                "speed", w.speed
            )));
        }

        return createFeatureCollection(features);
    }

    /**
     * Get JIM routes fan (all agents) as GeoJSON.
     * Black lines for all routes, thicker for optimal keyAgent route.
     */
    public Map<String, Object> getAgentsFanGeoJson() {
        ensureInitialized();

        List<Map<String, Object>> features = new ArrayList<>();

        if (jim == null || jim.keyAgent == null) {
            return createFeatureCollection(features);
        }

        // Access newAgents via reflection
        try {
            java.lang.reflect.Field newAgentsField = JIM.class.getDeclaredField("newAgents");
            newAgentsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.List<Agent> newAgents = (java.util.List<Agent>) newAgentsField.get(jim);

            if (newAgents != null) {
                for (Agent agent : newAgents) {
                    boolean isKeyAgent = (agent == jim.keyAgent);
                    List<double[]> agentCoords = new ArrayList<>();

                    // Walk back through agent chain
                    Agent a = agent;
                    while (a != null) {
                        double lng = Math.toDegrees(a.position.x);
                        double lat = Math.toDegrees(a.position.y);
                        agentCoords.add(0, new double[]{lng, lat});
                        a = a.previousAgent;
                    }

                    if (agentCoords.size() >= 2) {
                        Map<String, Object> props = new LinkedHashMap<>();
                        props.put("type", isKeyAgent ? "keyAgent" : "agent");
                        props.put("color", "#000000");  // Black
                        props.put("weight", isKeyAgent ? 3 : 1);
                        features.add(createLineStringFeature(agentCoords, props));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error accessing JIM agents", e);
        }

        return createFeatureCollection(features);
    }

    /**
     * Get obstructions as GeoJSON.
     * Red lines showing no-go zones.
     */
    public Map<String, Object> getObstructionsGeoJson() {
        ensureInitialized();

        List<Map<String, Object>> features = new ArrayList<>();

        // Get obstructions from waypoints
        if (boat != null && boat.waypoints != null) {
            for (Waypoint wp : boat.waypoints) {
                if (wp.obstructions != null && wp.obstructions.data != null) {
                    addObstructionFeatures(features, wp.obstructions);
                }
            }
        }

        // Also check Obstruction.active
        if (Obstruction.active != null && Obstruction.active.data != null) {
            addObstructionFeatures(features, Obstruction.active);
        }

        return createFeatureCollection(features);
    }

    /**
     * Helper to add obstruction line features using reflection to access Line fields.
     */
    private void addObstructionFeatures(List<Map<String, Object>> features, Obstruction obs) {
        try {
            // Access Line inner class fields via reflection
            Class<?> lineClass = obs.data[0].getClass();
            java.lang.reflect.Field startField = lineClass.getDeclaredField("start");
            java.lang.reflect.Field endField = lineClass.getDeclaredField("end");
            startField.setAccessible(true);
            endField.setAccessible(true);

            for (Object lineObj : obs.data) {
                Vector2 start = (Vector2) startField.get(lineObj);
                Vector2 end = (Vector2) endField.get(lineObj);

                List<double[]> coords = new ArrayList<>();
                // Obstruction lines are in degrees
                coords.add(new double[]{start.x, start.y});
                coords.add(new double[]{end.x, end.y});
                features.add(createLineStringFeature(coords, Map.of(
                    "type", "obstruction",
                    "color", "#FF0000",  // Red
                    "weight", 2
                )));
            }
        } catch (Exception e) {
            logger.error("Error accessing obstruction line fields", e);
        }
    }

    /**
     * Get time position indicator as GeoJSON.
     * Shows interpolated boat position at current time on both LastRoute and JIM route.
     */
    public Map<String, Object> getTimePositionGeoJson(long time) {
        ensureInitialized();

        List<Map<String, Object>> features = new ArrayList<>();

        if (time == 0) {
            time = mapService.getTimeInfo().timestamp;
        }

        // Time position on LastRoute
        if (lastRoute != null && !lastRoute.waypoints.isEmpty()) {
            InterimFix previous = lastRoute.waypoints.getFirst();
            InterimFix current = null;

            for (InterimFix w : lastRoute.waypoints) {
                if (w.time >= time) {
                    current = w;
                    break;
                }
                previous = w;
            }

            if (current != null && previous != null && current.time != previous.time) {
                float t = (float) (time - previous.time) / (float) (current.time - previous.time);
                double lng = Math.toDegrees(previous.position.x + t * (current.position.x - previous.position.x));
                double lat = Math.toDegrees(previous.position.y + t * (current.position.y - previous.position.y));

                features.add(createPointFeature(lng, lat, Map.of(
                    "type", "timePositionLastRoute",
                    "time", time,
                    "color", "#404040"  // Dark gray to match LastRoute
                )));
            }
        }

        // Time position on JIM keyAgent route
        if (jim != null && jim.keyAgent != null) {
            // Walk the agent chain to find the position at time
            Agent previous = null;
            Agent current = jim.keyAgent;

            // Build chain in chronological order
            List<Agent> chain = new ArrayList<>();
            Agent a = jim.keyAgent;
            while (a != null) {
                chain.add(0, a);
                a = a.previousAgent;
            }

            for (int i = 1; i < chain.size(); i++) {
                if (chain.get(i).time >= time) {
                    previous = chain.get(i - 1);
                    current = chain.get(i);
                    break;
                }
            }

            if (previous != null && current != null && current.time != previous.time) {
                float t = (float) (time - previous.time) / (float) (current.time - previous.time);
                double lng = Math.toDegrees(previous.position.x + t * (current.position.x - previous.position.x));
                double lat = Math.toDegrees(previous.position.y + t * (current.position.y - previous.position.y));

                features.add(createPointFeature(lng, lat, Map.of(
                    "type", "timePositionJIM",
                    "time", time,
                    "color", "#000000"  // Black
                )));
            }
        }

        return createFeatureCollection(features);
    }

    /**
     * Get great circle reference lines as GeoJSON.
     * Shows the corridor from departure to destination.
     */
    public Map<String, Object> getGreatCircleGeoJson() {
        ensureInitialized();

        List<Map<String, Object>> features = new ArrayList<>();

        if (boat == null || boat.waypoints == null || boat.waypoints.length < 2) {
            return createFeatureCollection(features);
        }

        // Get departure and destination waypoints
        Waypoint depart = boat.waypoints[0];
        Waypoint dest = boat.waypoints[boat.waypoints.length - 1];

        double departLng = Math.toDegrees(depart.position.x);
        double departLat = Math.toDegrees(depart.position.y);
        double destLng = Math.toDegrees(dest.position.x);
        double destLat = Math.toDegrees(dest.position.y);

        // Main great circle line
        List<double[]> gcCoords = new ArrayList<>();
        gcCoords.add(new double[]{departLng, departLat});
        gcCoords.add(new double[]{destLng, destLat});
        features.add(createLineStringFeature(gcCoords, Map.of(
            "type", "greatCircle",
            "color", "#000000",
            "weight", 1,
            "dashArray", "5,5"
        )));

        return createFeatureCollection(features);
    }

    /**
     * Get wind data as array of points with u/v components.
     */
    public java.util.List<GeoJsonController.WindPoint> getWindData(double south, double west, double north, double east, long time) {
        ensureInitialized();

        List<GeoJsonController.WindPoint> points = new ArrayList<>();

        if (wind == null) {
            return points;
        }

        if (time == 0) {
            time = mapService.getTimeInfo().timestamp;
        }

        // Create a grid of wind data points
        double latStep = (north - south) / 15;
        double lonStep = (east - west) / 20;

        for (double lat = south; lat <= north; lat += latStep) {
            for (double lon = west; lon <= east; lon += lonStep) {
                try {
                    Vector2 windValue = new Vector2();
                    wind.getValue(new Vector2((float) lon, (float) lat), time, windValue);

                    if (!Float.isNaN(windValue.x) && !Float.isNaN(windValue.y)) {
                        points.add(new GeoJsonController.WindPoint(lat, lon, windValue.x, windValue.y));
                    }
                } catch (Exception e) {
                    // Skip this point on error
                }
            }
        }

        return points;
    }

    /**
     * Get tide/current data as array of points with u/v components.
     */
    public java.util.List<GeoJsonController.TidePoint> getTideData(double south, double west, double north, double east, long time) {
        ensureInitialized();

        List<GeoJsonController.TidePoint> points = new ArrayList<>();

        if (water == null) {
            return points;
        }

        if (time == 0) {
            time = mapService.getTimeInfo().timestamp;
        }

        // Create a grid of tide data points
        double latStep = (north - south) / 15;
        double lonStep = (east - west) / 20;

        for (double lat = south; lat <= north; lat += latStep) {
            for (double lon = west; lon <= east; lon += lonStep) {
                try {
                    Vector2 tideValue = new Vector2();
                    water.getValue(new Vector2((float) lon, (float) lat), time, tideValue);

                    if (!Float.isNaN(tideValue.x) && !Float.isNaN(tideValue.y)) {
                        points.add(new GeoJsonController.TidePoint(lat, lon, tideValue.x, tideValue.y));
                    }
                } catch (Exception e) {
                    // Skip this point on error
                }
            }
        }

        return points;
    }

    // Conversion factor: 1 m/s = 1.94384 knots
    private static final double MS_TO_KNOTS = 1.94384;

    /**
     * Get hover info at a position.
     */
    public GeoJsonController.HoverResponse getHoverInfo(double lat, double lng, long time) {
        ensureInitialized();

        GeoJsonController.HoverResponse response = new GeoJsonController.HoverResponse();

        if (time == 0) {
            time = mapService.getTimeInfo().timestamp;
        }

        // Format position
        Fix fix = new Fix();
        fix.position = new Vector2((float) (lng * phys.radiansPerDegree), (float) (lat * phys.radiansPerDegree));
        response.position = fix.DMSLatitude() + " " + fix.DMSLongitude();

        // Get wind (display in knots)
        try {
            if (wind != null) {
                Vector2 w = new Vector2();
                wind.getValue(new Vector2((float) lng, (float) lat), time, w);
                if (!Float.isNaN(w.x) && !Float.isNaN(w.y)) {
                    double speedMs = Math.sqrt(w.x * w.x + w.y * w.y);
                    double speedKnots = speedMs * MS_TO_KNOTS;
                    double dir = Math.toDegrees(Math.atan2(w.x, w.y));
                    if (dir < 0) dir += 360;
                    response.wind = String.format("%.1f kts @ %.0f°", speedKnots, dir);
                } else {
                    response.wind = "N/A";
                }
            }
        } catch (Exception e) {
            response.wind = "N/A";
        }

        // Get tide/current (display in knots)
        try {
            if (water != null) {
                Vector2 t = new Vector2();
                water.getValue(new Vector2((float) lng, (float) lat), time, t);
                if (!Float.isNaN(t.x) && !Float.isNaN(t.y)) {
                    double speedMs = Math.sqrt(t.x * t.x + t.y * t.y);
                    double speedKnots = speedMs * MS_TO_KNOTS;
                    double dir = Math.toDegrees(Math.atan2(t.x, t.y));
                    if (dir < 0) dir += 360;
                    response.tide = String.format("%.2f kts @ %.0f°", speedKnots, dir);
                } else {
                    response.tide = "N/A";
                }
            }
        } catch (Exception e) {
            response.tide = "N/A";
        }

        // Get waves
        try {
            if (waves != null) {
                float height = waves.getValue(new Vector2((float) lng, (float) lat), time);
                if (height >= 0 && height < 1e10 && !Float.isNaN(height)) {
                    response.waves = String.format("%.1f m", height);
                } else {
                    response.waves = "N/A";
                }
            }
        } catch (Exception e) {
            response.waves = "N/A";
        }

        return response;
    }

    // GeoJSON helper methods

    private Map<String, Object> createFeatureCollection(List<Map<String, Object>> features) {
        Map<String, Object> fc = new LinkedHashMap<>();
        fc.put("type", "FeatureCollection");
        fc.put("features", features);
        return fc;
    }

    private Map<String, Object> createLineStringFeature(List<double[]> coordinates, Map<String, Object> properties) {
        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "LineString");
        geometry.put("coordinates", coordinates);

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("geometry", geometry);
        feature.put("properties", properties);
        return feature;
    }

    private Map<String, Object> createPolygonFeature(List<double[]> coordinates, Map<String, Object> properties) {
        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "Polygon");
        // GeoJSON Polygon requires array of rings (outer ring + holes)
        geometry.put("coordinates", List.of(coordinates));

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("geometry", geometry);
        feature.put("properties", properties);
        return feature;
    }

    private Map<String, Object> createPointFeature(double lng, double lat, Map<String, Object> properties) {
        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "Point");
        geometry.put("coordinates", new double[]{lng, lat});

        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");
        feature.put("geometry", geometry);
        feature.put("properties", properties);
        return feature;
    }
}