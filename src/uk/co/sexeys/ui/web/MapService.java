package uk.co.sexeys.ui.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.sexeys.*;
import uk.co.sexeys.CMap.CMap;
import uk.co.sexeys.JIM.Agent;
import uk.co.sexeys.JIM.CrossTrack;
import uk.co.sexeys.JIM.JIM;
import uk.co.sexeys.rendering.MercatorProjection;
import uk.co.sexeys.water.Current;
import uk.co.sexeys.water.PrevailingCurrent;
import uk.co.sexeys.water.Tide;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.waypoint.Depart;
import uk.co.sexeys.waypoint.Waypoint;
import uk.co.sexeys.wind.Prevailing;
import uk.co.sexeys.wind.SailDocs;
import uk.co.sexeys.wind.Wind;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service that manages the map state and rendering for web clients.
 * This is a singleton service that holds the shared map state.
 */
@Service
public class MapService {
    private static final Logger logger = LoggerFactory.getLogger(MapService.class);

    private Mercator screen;
    private MercatorProjection projection;
    private Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    private final SimpleDateFormat format = new SimpleDateFormat("EEE yyyy.MM.dd HH:mm:ss zzz");

    // Data layers
    private WVS wvs;
    private CMap cMap;
    private Wind wind;
    private Water water;
    private Waves waves;
    private Boat boat;
    private JIM jim;
    private IDX idx;
    private Harmonics harmonics;
    private Depth depth;
    private LastRoute lastRoute;

    // Visibility toggles
    private boolean showWind = true;
    private boolean showWater = true;
    private boolean showWaves = true;
    private boolean showCharts = true;
    private boolean showShoreline = true;
    private boolean showRoute = true;
    private boolean showFan = true;
    private boolean showLastRoute = true;
    private boolean showObstructions = true;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Initialize the map service with configuration.
     * Called lazily on first render request.
     */
    public synchronized void initialize() {
        if (initialized) return;

        try {
            // Load configuration
            Config.load();
            initFromConfig();

            // Initialize shoreline
            Main.shoreline = new Shoreline(Main.WVSResolution);

            // Create boat and parse route
            boat = new Boat();
            boat.waypoints = parseRoute(Main.ROUTE);

            // Load polars
            loadPolars();

            // Initialize screen
            String[] temp = Main.SCREEN.split(" ");
            float screenTop = Fix.parseLatitude(temp[0]);
            float screenLeft = Fix.parseLongitude(temp[1]);
            float screenRight = Fix.parseLongitude(temp[3]);
            screen = new Mercator(screenTop, screenLeft, screenRight);
            screen.width = 1200;
            screen.height = 800;
            // Set x2/y2 to screen dimensions - computeParameters uses these
            screen.x2 = new int[]{screen.width};
            screen.y2 = new int[]{screen.height};
            // Calculate lat2 from the aspect ratio (lon range / width * height)
            float lonRange = screenRight - screenLeft;
            float latRange = lonRange * screen.height / screen.width;
            screen.lat2 = screenTop - latRange;
            screen.computeParameters(0);
            screen.enabled = true;

            projection = new MercatorProjection(screen);

            // Initialize data layers
            wvs = new WVS(Main.WVSResolution);
            cMap = new CMap(0);
            idx = new IDX(Main.root + File.separator + "Charts/Tides/HARMONIC");
            harmonics = new Harmonics(Main.root + File.separator + "Charts/Tides/HARMONIC");
            depth = new Depth(Main.root + File.separator + "Charts/Bathymetry/GEBCO_2020.dat", 4);

            try {
                idx.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Compute initial route
            computeRoute();

            // Center viewport on departure point, keeping the original viewport range
            if (boat.waypoints.length > 0 && boat.waypoints[0] instanceof Depart) {
                Depart depart = (Depart) boat.waypoints[0];
                centerOnPosition(depart.position);
                logger.info("Centered viewport on departure: ({}, {})",
                    Math.toDegrees(depart.position.x), Math.toDegrees(depart.position.y));
            }

            // Load last route if available
            lastRoute = new LastRoute();
            logger.info("LastRoute loaded with {} waypoints", lastRoute.waypoints.size());

            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize MapService", e);
        }
    }

    private void initFromConfig() {
        Config cfg = Config.get();
        Main.SCREEN = cfg.getScreen();
        Main.ROUTE = cfg.getRoute();
        Main.root = cfg.getRoot();
        logger.info("MapService config loaded:");
        logger.info("  Main.root = {}", Main.root);
        logger.info("  Prevailing path would be: {}", Main.root + "Prevailing" + File.separator + "prevailingCurrents.nc");
        java.io.File prevFile = new java.io.File(Main.root + "Prevailing" + File.separator + "prevailingCurrents.nc");
        logger.info("  File exists: {}, absolute path: {}", prevFile.exists(), prevFile.getAbsolutePath());
        Main.WindSource = cfg.getWindSource();
        Main.WindResolution = cfg.getWindResolution();
        Main.prevailingTransitionPeriod = cfg.getPrevailingTransitionPeriod();
        Main.sparsePolar = cfg.isSparsePolar();
        Main.polarHighWindOnly = cfg.isPolarHighWindOnly();
        Main.useWater = cfg.isUseWater();
        Main.crossDateLine = cfg.isCrossDateLine();
        Main.useIceZone = cfg.isUseIceZone();
        Main.WVSResolution = cfg.getWvsResolution();
        Main.ChartOffsetY = cfg.getChartOffsetY();
        Main.ChartOffsetX = cfg.getChartOffsetX();
        Main.REPLAY = cfg.getReplay();
        Main.ExpandingTimeFactor = cfg.getExpandingTimeFactor();
        Main.minLon = cfg.getMinLon();
        Main.maxLon = cfg.getMaxLon();
        Main.numberOfFixes = cfg.getNumberOfFixes();
        Main.searchTolerance = cfg.getSearchTolerance();
        Main.C2SSearchPeriod = cfg.getC2sSearchPeriod();
        Main.C2SLegs = cfg.getC2sLegs();
        Main.C2SAgents = cfg.getC2sAgents();
        Main.C2SCR = cfg.getC2sCR();
        Main.routeAspectRatio = cfg.getRouteAspectRatio();
        Main.continuousFactor = cfg.getContinuousFactor();
        Main.ShowRouteResolution = cfg.isShowRouteResolution();
        Main.useDifferentialEvolution = cfg.isUseDifferentialEvolution();
        Main.JIMCutoff = cfg.getJimCutoff();
        Main.waveWarning = cfg.getWaveWarning();
        Main.fontSize = cfg.getFontSize();
    }

    private Waypoint[] parseRoute(String routeString) {
        // Route parsing - matches StreamFrame.ParseRoute()
        String[] lines = routeString.split("\n");
        LinkedList<Waypoint> waypoints = new LinkedList<>();
        LinkedList<Obstruction> obstructions = null;
        List<String> windList = new LinkedList<>();
        List<String> tideList = new LinkedList<>();
        List<String> currentList = new LinkedList<>();
        List<String> wavesList = new LinkedList<>();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.startsWith("#")) continue;

            if (trimmedLine.startsWith("Using Polar:")) {
                // Polar handled separately in loadPolars()
            } else if (trimmedLine.startsWith("Using Wind:")) {
                String[] temp = trimmedLine.split("Using Wind: ");
                if (temp.length > 1) {
                    windList.add(Main.root + "grib" + File.separator + temp[1]);
                    logger.info("Added wind file: {}", Main.root + "grib" + File.separator + temp[1]);
                }
            } else if (trimmedLine.startsWith("Using Tide:")) {
                String[] temp = trimmedLine.split("Using Tide: ");
                if (temp.length > 1) {
                    tideList.add(Main.root + "grib" + File.separator + temp[1]);
                    logger.info("Added tide file: {}", Main.root + "grib" + File.separator + temp[1]);
                }
            } else if (trimmedLine.startsWith("Using Current:")) {
                String[] temp = trimmedLine.split("Using Current: ");
                if (temp.length > 1) {
                    currentList.add(Main.root + "grib" + File.separator + temp[1]);
                    logger.info("Added current file: {}", Main.root + "grib" + File.separator + temp[1]);
                }
            } else if (trimmedLine.startsWith("Using Waves:")) {
                String[] temp = trimmedLine.split("Using Waves: ");
                if (temp.length > 1) {
                    wavesList.add(Main.root + "grib" + File.separator + temp[1]);
                    logger.info("Added waves file: {}", Main.root + "grib" + File.separator + temp[1]);
                }
            } else if (trimmedLine.startsWith("Depart")) {
                waypoints.add(new Depart(trimmedLine));
                obstructions = new LinkedList<>();
            } else if (trimmedLine.startsWith("Expand")) {
                uk.co.sexeys.waypoint.Waypoint previous = waypoints.getLast();
                waypoints.add(new uk.co.sexeys.waypoint.Expand(trimmedLine, obstructions, previous));
                obstructions = new LinkedList<>();
            } else if (trimmedLine.startsWith("Destination")) {
                uk.co.sexeys.waypoint.Waypoint previous = waypoints.getLast();
                waypoints.add(new uk.co.sexeys.waypoint.Destination(trimmedLine, obstructions, previous));
                obstructions = new LinkedList<>();
            } else if (trimmedLine.startsWith("Obstruction")) {
                obstructions.add(new Obstruction(trimmedLine));
            }
        }

        if (!waypoints.isEmpty() && waypoints.getFirst() instanceof Depart) {
            long departTime = ((Depart) waypoints.getFirst()).time;
            UTC.setTimeInMillis(departTime);

            // Initialize wind - start with Prevailing, then layer GRIB data on top
            wind = new Prevailing(departTime);
            if (!windList.isEmpty()) {
                logger.info("Loading {} wind GRIB files", windList.size());
                wind = new SailDocs(windList, wind);
            }

            // Initialize water/current - start with Prevailing, then layer GRIB data
            water = new PrevailingCurrent(departTime);
            if (!currentList.isEmpty()) {
                logger.info("Loading {} current GRIB files", currentList.size());
                water = new Current(currentList, water);
            }
            if (!tideList.isEmpty()) {
                logger.info("Loading {} tide files", tideList.size());
                water = new Tide(tideList, water);
            }

            // Initialize waves
            if (!wavesList.isEmpty()) {
                logger.info("Loading {} wave GRIB files", wavesList.size());
                waves = new Waves(wavesList);
            } else {
                waves = new Waves(departTime);
            }
        }

        return waypoints.toArray(new Waypoint[0]);
    }

    private void loadPolars() {
        // Find polar directory from route config
        String polarPath = Main.root;  // Default
        String[] lines = Main.ROUTE.split("\n");
        for (String line : lines) {
            if (line.trim().startsWith("Using Polar:")) {
                polarPath = Main.root + line.split("Using Polar:")[1].trim();
                break;
            }
        }

        File polarDir = new File(polarPath);
        if (!polarDir.exists() || !polarDir.isDirectory()) {
            System.err.println("Polar directory not found: " + polarPath);
            boat.polar = new Polar();
            return;
        }

        boat.polar = new Polar();
        File[] files = polarDir.listFiles();
        if (files == null) return;

        StringBuilder sb = new StringBuilder();
        for (File file : files) {
            try {
                sb.setLength(0);
                FileInputStream is = new FileInputStream(file);
                byte[] b = new byte[is.available()];
                is.read(b);
                is.close();
                sb.append(new String(b));
                sb.append("\n");
                boat.polar.ScanVirtualRegattaPolar(sb.toString(), file.getName());
            } catch (IOException e) {
                System.err.println("Error reading polar: " + file.getName());
            }
        }
        boat.polar.combinePolars();
        boat.polar.computeVMGPolar();
    }

    private void computeRoute() {
        try {
            logger.info("Computing route...");
            logger.info("  boat.waypoints.length = {}", boat.waypoints.length);
            for (int i = 0; i < boat.waypoints.length; i++) {
                logger.info("  waypoint[{}]: {} at ({}, {}) binWidth={} numberOfBins={} timeStep={}", i,
                    boat.waypoints[i].getClass().getSimpleName(),
                    Math.toDegrees(boat.waypoints[i].position.x),
                    Math.toDegrees(boat.waypoints[i].position.y),
                    boat.waypoints[i].binWidth,
                    boat.waypoints[i].numberOfBins,
                    boat.waypoints[i].timeStep);
            }
            logger.info("  boat.polar = {}", boat.polar);
            logger.info("  boat.polar.raw = {}", boat.polar != null ? boat.polar.raw : "null");
            logger.info("  wind = {}", wind);
            logger.info("  water = {}", water);
            jim = new CrossTrack(boat, wind, water);
            jim.SearchInit();
            logger.info("  SearchInit complete, route.currentTime = {}", jim.route.currentTime);
            logger.info("  Main.JIMCutoff = {} ms ({} hours)", Main.JIMCutoff, Main.JIMCutoff / 3600000.0);
            jim.Search(jim.route.currentTime + Main.JIMCutoff);
            logger.info("  Search complete, keyAgent = {}", jim.keyAgent);
            if (jim.keyAgent != null) {
                logger.info("  keyAgent.position = ({}, {})",
                    Math.toDegrees(jim.keyAgent.position.x),
                    Math.toDegrees(jim.keyAgent.position.y));
                // Count agent chain length
                int chainLength = 0;
                Agent a = jim.keyAgent;
                while (a != null) {
                    chainLength++;
                    a = a.previousAgent;
                }
                logger.info("  keyAgent chain length = {}", chainLength);
            }
            Obstruction.active = boat.waypoints[1].obstructions;
        } catch (Exception e) {
            logger.error("Error computing route", e);
            e.printStackTrace();
        }
    }

    /**
     * Render the current map state to drawing commands.
     */
    public MapController.RenderResponse render(int width, int height) {
        if (!initialized) {
            initialize();
        }

        // Update screen size if changed
        if (screen.width != width || screen.height != height) {
            // Preserve the center in Mercator space when resizing
            float lonRange = screen.lon2 - screen.lon1;
            double y1 = latToMercatorY(screen.lat1);
            double y2 = latToMercatorY(screen.lat2);
            double yCenter = (y1 + y2) / 2;  // Center in Mercator space

            screen.width = width;
            screen.height = height;
            screen.x2 = new int[]{width};
            screen.y2 = new int[]{height};

            // Calculate new Mercator y range based on aspect ratio
            // Mercator y is in radians, lonRange is in degrees - need conversion
            double yRange = Math.toRadians(lonRange) * height / width;
            double newY1 = yCenter + yRange / 2;
            double newY2 = yCenter - yRange / 2;

            // Convert back to geographic latitudes
            screen.lat1 = (float) mercatorYToLat(newY1);
            screen.lat2 = (float) mercatorYToLat(newY2);

            screen.computeParameters(0);
            projection = new MercatorProjection(screen);
            cMap.update(screen);
        }

        WebRenderer renderer = new WebRenderer(width, height);
        renderer.clear();

        long time = UTC.getTimeInMillis();
        int cmdCount;

        // Render layers (with command counting for diagnostics)
        // Layer tags for client-side caching: static, dynamic, route, ui

        // Static layers (charts, shoreline) - rarely change
        renderer.setCurrentLayer("static");
        if (depth != null && cMap.scaleLevel < 0) {
            cmdCount = renderer.getCommands().size();
            depth.render(renderer, projection, time);
            logger.debug("Depth: {} commands", renderer.getCommands().size() - cmdCount);
        }

        if (showCharts) {
            cmdCount = renderer.getCommands().size();
            cMap.render(renderer, projection, time);
            logger.debug("Charts: {} commands", renderer.getCommands().size() - cmdCount);
        }

        if (showShoreline) {
            cmdCount = renderer.getCommands().size();
            wvs.render(renderer, projection, time);
            logger.debug("Shoreline: {} commands", renderer.getCommands().size() - cmdCount);
        }

        // Dynamic layers (weather) - change with time
        renderer.setCurrentLayer("dynamic");
        if (showWaves && waves != null) {
            cmdCount = renderer.getCommands().size();
            waves.render(renderer, projection, time);
            logger.debug("Waves: {} commands", renderer.getCommands().size() - cmdCount);
        }

        if (showWind && wind != null) {
            cmdCount = renderer.getCommands().size();
            wind.render(renderer, projection, time);
            logger.debug("Wind: {} commands", renderer.getCommands().size() - cmdCount);
        }

        if (Main.useWater && showWater && water != null) {
            cmdCount = renderer.getCommands().size();
            water.render(renderer, projection, time);
            logger.debug("Water/Tide: {} commands", renderer.getCommands().size() - cmdCount);
        }

        // Route layers (JIM, last route) - change on route recalculation
        renderer.setCurrentLayer("route");
        if (jim != null && (showRoute || showFan)) {
            cmdCount = renderer.getCommands().size();
            jim.showRoute = showRoute;
            jim.showFan = showFan;
            jim.render(renderer, projection, time);
            logger.debug("JIM (route={}, fan={}): {} commands", showRoute, showFan, renderer.getCommands().size() - cmdCount);
        }

        if (showLastRoute && lastRoute != null) {
            cmdCount = renderer.getCommands().size();
            lastRoute.render(renderer, projection, time);
            logger.debug("LastRoute: {} commands", renderer.getCommands().size() - cmdCount);
        }

        // UI layers (obstructions, waypoints, boat) - always visible
        renderer.setCurrentLayer("ui");
        if (showObstructions) {
            cmdCount = renderer.getCommands().size();
            for (Waypoint w : boat.waypoints) {
                w.render(renderer, projection, time);
            }
            logger.debug("Obstructions: {} commands", renderer.getCommands().size() - cmdCount);
        }

        // Render boat
        cmdCount = renderer.getCommands().size();
        boat.render(renderer, projection, time);
        logger.debug("Boat: {} commands", renderer.getCommands().size() - cmdCount);

        logger.debug("Total render: {} commands", renderer.getCommands().size());

        MapController.ViewportInfo viewport = getViewport();
        return new MapController.RenderResponse(renderer.getCommands(), time, viewport);
    }

    /**
     * Pan the map by screen coordinate deltas.
     */
    public void pan(double deltaX, double deltaY) {
        if (!initialized) return;

        // Convert screen delta to geographic delta
        Vector2 p1 = screen.fromPointToLatLng(0, 0).scale(phys.radiansPerDegree);
        Vector2 p2 = screen.fromPointToLatLng((float) deltaX, (float) deltaY).scale(phys.radiansPerDegree);
        Vector2 diff = p2.minus(p1);
        diff.scaleIP((float) (180 / Math.PI));

        screen.lon1 -= diff.x;
        screen.lat1 -= diff.y;
        screen.lon2 -= diff.x;
        screen.lat2 -= diff.y;
        screen.computeParameters(0);
        cMap.update(screen);
        projection = new MercatorProjection(screen);
    }

    /**
     * Zoom the map by a factor centered on a screen point.
     */
    public void zoom(double factor, double centerX, double centerY) {
        if (!initialized) return;

        Vector2 p = screen.fromPointToLatLng((float) centerX, (float) centerY);

        float dlon = screen.lon2 - screen.lon1;
        float dlat = screen.lat2 - screen.lat1;
        screen.lon1 -= (float) ((factor - 1) * (p.x - screen.lon1));
        screen.lat1 -= (float) ((factor - 1) * (p.y - screen.lat1));
        screen.lon2 = screen.lon1 + dlon * (float) factor;
        screen.lat2 = screen.lat1 + dlat * (float) factor;
        screen.computeParameters(0);
        cMap.update(screen);
        projection = new MercatorProjection(screen);
    }

    /**
     * Set the display time.
     */
    public void setTime(long timestamp) {
        UTC.setTimeInMillis(timestamp);
    }

    /**
     * Adjust the display time by minutes.
     */
    public void adjustTime(int deltaMinutes) {
        UTC.add(Calendar.MINUTE, deltaMinutes);
    }

    /**
     * Handle a click on the map.
     */
    public MapController.ClickResponse handleClick(double x, double y, int button, List<String> modifiers) {
        if (!initialized) return new MapController.ClickResponse("none", Map.of());

        Vector2 p = screen.fromPointToLatLng((float) x, (float) y);

        if (button == 3) {
            // Right click - could toggle charts
            return new MapController.ClickResponse("context_menu", Map.of(
                "lat", p.y,
                "lon", p.x
            ));
        }

        return new MapController.ClickResponse("click", Map.of(
            "lat", p.y,
            "lon", p.x
        ));
    }

    /**
     * Handle hover for info display.
     */
    public MapController.HoverResponse handleHover(double x, double y) {
        MapController.HoverResponse response = new MapController.HoverResponse();

        if (!initialized) return response;

        Vector2 screenPos = screen.fromPointToLatLng((float) x, (float) y);
        Vector2 pos = new Vector2(screenPos).scale(phys.radiansPerDegree);

        Fix fix = new Fix();
        fix.position = pos;
        response.position = fix.DMSLatitude() + " " + fix.DMSLongitude();

        try {
            if (wind != null) {
                Vector2 w = new Vector2();
                wind.getValue(pos.scale(phys.degrees), UTC.getTimeInMillis(), w);
                // Convert from m/s components to knots and degrees (true)
                // w.x = East component, w.y = North component
                double speedMs = Math.sqrt(w.x * w.x + w.y * w.y);
                double speedKts = speedMs * 1.94384; // m/s to knots
                // Wind direction is FROM which it blows, so add 180 degrees
                double dirRad = Math.atan2(w.x, w.y);
                double dirDeg = Math.toDegrees(dirRad);
                dirDeg = (dirDeg + 180 + 360) % 360; // Wind blows FROM this direction
                response.wind = String.format("%.1f kts / %03.0f°T", speedKts, dirDeg);
            }
        } catch (Exception e) {
            response.wind = "N/A";
        }

        try {
            if (water != null) {
                Vector2 t = new Vector2();
                Vector2 queryPos = pos.scale(phys.degrees);
                water.getValue(queryPos, UTC.getTimeInMillis(), t);
                // Convert from m/s components to knots and degrees (true)
                // t.x = East component, t.y = North component
                double speedMs = Math.sqrt(t.x * t.x + t.y * t.y);
                double speedKts = speedMs * 1.94384; // m/s to knots
                // Current direction is the direction it FLOWS TO
                double dirRad = Math.atan2(t.x, t.y);
                double dirDeg = Math.toDegrees(dirRad);
                dirDeg = (dirDeg + 360) % 360; // Current flows TO this direction
                response.tide = String.format("%.2f kts / %03.0f°T", speedKts, dirDeg);
            } else {
                response.tide = "N/A";
            }
        } catch (Exception e) {
            response.tide = "N/A";
        }

        try {
            if (waves != null) {
                float height = waves.getValue(pos.scale(phys.degrees), UTC.getTimeInMillis());
                // Check for missing/undefined values (GRIB uses 99999e20 as undefined, -1 means out of bounds)
                if (height < 0 || height > 1e10 || Float.isNaN(height) || Float.isInfinite(height)) {
                    response.waves = "N/A";
                } else {
                    response.waves = String.format("%.1f m", height);
                }
            }
        } catch (Exception e) {
            response.waves = "N/A";
        }

        return response;
    }

    /**
     * Get current viewport information.
     */
    public MapController.ViewportInfo getViewport() {
        if (!initialized || screen == null) {
            return new MapController.ViewportInfo(0, 0, 0, 0, 0);
        }
        return new MapController.ViewportInfo(
            screen.lat1,
            screen.lat2,
            screen.lon2,
            screen.lon1,
            screen.scale
        );
    }

    /**
     * Toggle layer visibility.
     */
    public void toggleLayer(String layer) {
        switch (layer.toLowerCase()) {
            case "wind" -> showWind = !showWind;
            case "water", "tide" -> showWater = !showWater;
            case "waves" -> showWaves = !showWaves;
            case "charts" -> showCharts = !showCharts;
            case "shoreline" -> showShoreline = !showShoreline;
            case "route" -> showRoute = !showRoute;
            case "fan" -> showFan = !showFan;
            case "lastroute" -> showLastRoute = !showLastRoute;
            case "obstructions" -> showObstructions = !showObstructions;
        }
    }

    /**
     * Get current time info.
     */
    public MapController.TimeInfo getTimeInfo() {
        return new MapController.TimeInfo(
            UTC.getTimeInMillis(),
            format.format(UTC.getTime())
        );
    }

    /**
     * Reinitialize the map service (e.g., after config change).
     * Unlike initialize(), this does NOT reload from config file - it uses
     * the Main.ROUTE and Main.SCREEN values that were already set by the caller.
     */
    public synchronized void reinitialize() {
        logger.info("Reinitializing MapService with new route...");

        // Clear existing data to free memory BEFORE loading new data
        jim = null;
        lastRoute = null;
        wind = null;
        water = null;
        waves = null;
        boat = null;

        // Request garbage collection to free memory from old GRIB data
        System.gc();

        try {
            // Re-parse route with new settings (don't reload config file)
            boat = new Boat();
            boat.waypoints = parseRoute(Main.ROUTE);

            // Reload polars for the new route
            loadPolars();

            // Update screen viewport if SCREEN was changed
            if (Main.SCREEN != null) {
                String[] temp = Main.SCREEN.split(" ");
                float screenTop = Fix.parseLatitude(temp[0]);
                float screenLeft = Fix.parseLongitude(temp[1]);
                float screenRight = Fix.parseLongitude(temp[3]);
                screen.lat1 = screenTop;
                screen.lon1 = screenLeft;
                screen.lon2 = screenRight;
                float lonRange = screenRight - screenLeft;
                float latRange = lonRange * screen.height / screen.width;
                screen.lat2 = screenTop - latRange;
                screen.computeParameters(0);
                cMap.update(screen);
                projection = new MercatorProjection(screen);
            }

            // Center viewport on new departure point
            if (boat.waypoints.length > 0 && boat.waypoints[0] instanceof Depart) {
                Depart depart = (Depart) boat.waypoints[0];
                centerOnPosition(depart.position);
                logger.info("Centered viewport on departure: ({}, {})",
                    Math.toDegrees(depart.position.x), Math.toDegrees(depart.position.y));
            }

            // Compute the new route
            computeRoute();

            // Reset lastRoute
            lastRoute = new LastRoute();

            logger.info("Reinitialize complete");
        } catch (Exception e) {
            logger.error("Error during reinitialize", e);
            e.printStackTrace();
        }
    }

    /**
     * Center the viewport on a given position (in radians).
     * Uses Mercator y-coordinates to ensure proper visual centering.
     */
    private void centerOnPosition(Vector2 posRadians) {
        if (screen == null) return;

        // Convert to degrees
        float centerLat = (float) Math.toDegrees(posRadians.y);
        float centerLon = (float) Math.toDegrees(posRadians.x);

        // Calculate current viewport size in longitude (linear in Mercator)
        float lonRange = screen.lon2 - screen.lon1;

        // Convert latitudes to Mercator y-coordinates for proper centering
        double y1 = latToMercatorY(screen.lat1);
        double y2 = latToMercatorY(screen.lat2);
        double yCenter = latToMercatorY(centerLat);
        double yRange = y1 - y2;

        // Calculate new Mercator y bounds centered on the target
        double newY1 = yCenter + yRange / 2;
        double newY2 = yCenter - yRange / 2;

        // Convert back to geographic latitudes
        screen.lat1 = (float) mercatorYToLat(newY1);
        screen.lat2 = (float) mercatorYToLat(newY2);
        screen.lon1 = centerLon - lonRange / 2;
        screen.lon2 = centerLon + lonRange / 2;

        screen.computeParameters(0);
        cMap.update(screen);
        projection = new MercatorProjection(screen);
    }

    /** Convert latitude (degrees) to Mercator y-coordinate */
    private double latToMercatorY(double lat) {
        double latRad = Math.toRadians(lat);
        return Math.log(Math.tan(Math.PI / 4 + latRad / 2));
    }

    /** Convert Mercator y-coordinate to latitude (degrees) */
    private double mercatorYToLat(double y) {
        return Math.toDegrees(2 * Math.atan(Math.exp(y)) - Math.PI / 2);
    }

    /**
     * Convert screen coordinates to lat/lon.
     * Returns null if not initialized.
     */
    public Vector2 screenToLatLon(double x, double y) {
        if (!initialized || screen == null) {
            return null;
        }
        return screen.fromPointToLatLng((float) x, (float) y);
    }
}
