package uk.co.sexeys.ui.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import uk.co.sexeys.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing configuration files and converting between
 * screen coordinates and geographic coordinates.
 */
@Service
public class ConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

    private final MapService mapService;

    public ConfigService(MapService mapService) {
        this.mapService = mapService;
    }

    /**
     * List available config files in the current directory.
     */
    public List<String> listConfigFiles() {
        List<String> configs = new ArrayList<>();
        try {
            Path dir = Paths.get(".");
            Files.list(dir)
                .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                .map(p -> p.getFileName().toString())
                .sorted()
                .forEach(configs::add);
        } catch (IOException e) {
            logger.error("Error listing config files", e);
        }
        return configs;
    }

    /**
     * List available GRIB files in the database/grib directory.
     */
    public List<String> listGribFiles() {
        List<String> files = new ArrayList<>();
        try {
            String root = Main.root != null ? Main.root : "./database/";
            Path gribDir = Paths.get(root, "grib");
            if (Files.exists(gribDir)) {
                Files.list(gribDir)
                    .filter(p -> {
                        String name = p.toString().toLowerCase();
                        return name.endsWith(".grb") || name.endsWith(".grib") ||
                               name.endsWith(".nc") || name.endsWith(".grb2");
                    })
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .forEach(files::add);
            }
        } catch (IOException e) {
            logger.error("Error listing GRIB files", e);
        }
        return files;
    }

    /**
     * List available polar directories.
     */
    public List<String> listPolars() {
        List<String> polars = new ArrayList<>();
        try {
            String root = Main.root != null ? Main.root : "./database/";
            Path polarDir = Paths.get(root);
            if (Files.exists(polarDir)) {
                Files.list(polarDir)
                    .filter(Files::isDirectory)
                    .filter(p -> {
                        // Check if directory contains polar files
                        try {
                            return Files.list(p).anyMatch(f ->
                                f.toString().toLowerCase().endsWith(".pol") ||
                                f.toString().toLowerCase().contains("polar"));
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .forEach(polars::add);
            }
            // Also add known polar names from the database
            Path dbDir = Paths.get(root);
            if (Files.exists(dbDir)) {
                Files.list(dbDir)
                    .filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .filter(name -> name.toUpperCase().equals(name)) // Convention: polar dirs are uppercase
                    .sorted()
                    .forEach(name -> {
                        if (!polars.contains(name)) polars.add(name);
                    });
            }
        } catch (IOException e) {
            logger.error("Error listing polars", e);
        }
        return polars;
    }

    /**
     * Get the current loaded configuration.
     */
    public ConfigController.ConfigData getCurrentConfig() {
        ConfigController.ConfigData data = new ConfigController.ConfigData();

        try {
            // Parse current route string to extract settings
            String route = Main.ROUTE;
            if (route != null) {
                parseRouteString(route, data);
            }
            data.screen = Main.SCREEN;
        } catch (Exception e) {
            logger.error("Error getting current config", e);
        }

        return data;
    }

    /**
     * Load a config file and return its data.
     */
    @SuppressWarnings("unchecked")
    public ConfigController.ConfigData loadConfig(String name) {
        try {
            Yaml yaml = new Yaml();
            InputStream is = new FileInputStream(name);
            Map<String, Object> config = yaml.load(is);
            is.close();

            return parseYamlConfig(config);
        } catch (Exception e) {
            logger.error("Error loading config: " + name, e);
            return null;
        }
    }

    /**
     * Save config data to a file.
     */
    public boolean saveConfig(String name, ConfigController.ConfigData config) {
        try {
            Map<String, Object> yamlMap = configToYaml(config);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Yaml yaml = new Yaml(options);
            FileWriter writer = new FileWriter(name);
            yaml.dump(yamlMap, writer);
            writer.close();

            logger.info("Saved config to: {}", name);
            return true;
        } catch (Exception e) {
            logger.error("Error saving config: " + name, e);
            return false;
        }
    }

    /**
     * Apply config and trigger route recalculation.
     */
    public void applyConfig(ConfigController.ConfigData config) throws Exception {
        // Build route string from config
        StringBuilder route = new StringBuilder();

        // Polar
        if (config.polar != null) {
            route.append("Using Polar: ").append(config.polar).append("\n");
        }

        // Weather files
        if (config.weatherFiles != null) {
            for (ConfigController.WeatherFile wf : config.weatherFiles) {
                if ("Wind".equals(wf.type)) {
                    route.append("Using Wind: ").append(wf.file).append("\n");
                } else if ("Tide".equals(wf.type)) {
                    route.append("Using Tide: ").append(wf.file).append("\n");
                } else if ("Waves".equals(wf.type)) {
                    route.append("Using Waves: ").append(wf.file).append("\n");
                } else if ("Current".equals(wf.type)) {
                    route.append("Using Current: ").append(wf.file).append("\n");
                }
            }
        }

        // Departure
        if (config.departure != null) {
            String pos = config.departure.latDMS != null ?
                config.departure.latDMS + " " + config.departure.lonDMS :
                formatDMS(config.departure.lat, true) + " " + formatDMS(config.departure.lon, false);
            String time = config.departure.time != null ? config.departure.time : "2025/01/01 00:00 UTC";
            route.append("Depart: ").append(pos).append(" ").append(time).append("\n");
        }

        // Expand - format: "Expand: 20 nm 360 bins of 1 nm 0.1 hour step"
        if (config.expand != null) {
            route.append("Expand: ")
                .append(config.expand.distanceNm).append(" nm ")
                .append(config.expand.bins).append(" bins of ")
                .append(config.expand.binSizeNm).append(" nm ")
                .append(config.expand.stepHours).append(" hour step\n");
        }

        // Obstructions
        if (config.obstructions != null) {
            for (ConfigController.ObstructionArea obs : config.obstructions) {
                route.append("Obstruction: ").append(obs.polygon).append("\n");
            }
        }

        // Destination - format: "Destination: LAT LON 0.2 nm 360 bins of 1 nm 0.1 hour step"
        if (config.destination != null) {
            String pos = config.destination.latDMS != null ?
                config.destination.latDMS + " " + config.destination.lonDMS :
                formatDMS(config.destination.lat, true) + " " + formatDMS(config.destination.lon, false);
            route.append("Destination: ").append(pos).append(" ")
                .append(config.destination.radiusNm).append(" nm ")
                .append(config.destination.bins).append(" bins of ")
                .append(config.destination.binSizeNm).append(" nm ")
                .append(config.destination.stepHours).append(" hour step\n");
        }

        // Update Main.ROUTE and reinitialize
        Main.ROUTE = route.toString();
        if (config.screen != null) {
            Main.SCREEN = config.screen;
        }

        logger.info("Applying new route config:\n{}", Main.ROUTE);

        // Reinitialize the map service with new route
        mapService.reinitialize();
    }

    /**
     * Convert screen coordinates to geographic coordinates.
     */
    public ConfigController.CoordsResponse screenToCoords(double x, double y) {
        try {
            Vector2 coords = mapService.screenToLatLon(x, y);
            if (coords != null) {
                String latDMS = formatDMS(coords.y, true);
                String lonDMS = formatDMS(coords.x, false);
                return new ConfigController.CoordsResponse(coords.y, coords.x, latDMS, lonDMS);
            }
        } catch (Exception e) {
            logger.error("Error converting screen coords", e);
        }
        return new ConfigController.CoordsResponse(0, 0, "", "");
    }

    // Helper methods

    @SuppressWarnings("unchecked")
    private ConfigController.ConfigData parseYamlConfig(Map<String, Object> config) {
        ConfigController.ConfigData data = new ConfigController.ConfigData();

        data.screen = (String) config.get("screen");

        Map<String, Object> routeMap = (Map<String, Object>) config.get("route");
        if (routeMap != null) {
            data.polar = (String) routeMap.get("polar");

            // Weather files
            List<Map<String, String>> using = (List<Map<String, String>>) routeMap.get("using");
            if (using != null) {
                data.weatherFiles = using.stream()
                    .map(m -> new ConfigController.WeatherFile(m.get("type"), m.get("file")))
                    .collect(Collectors.toList());
            }

            // Departure
            Map<String, Object> depart = (Map<String, Object>) routeMap.get("depart");
            if (depart != null) {
                data.departure = new ConfigController.DeparturePoint();
                String pos = (String) depart.get("position");
                if (pos != null) {
                    double[] coords = parseDMSPosition(pos);
                    data.departure.lat = coords[0];
                    data.departure.lon = coords[1];
                    data.departure.latDMS = formatDMS(coords[0], true);
                    data.departure.lonDMS = formatDMS(coords[1], false);
                }
                data.departure.time = (String) depart.get("time");
            }

            // Destination
            Map<String, Object> dest = (Map<String, Object>) routeMap.get("destination");
            if (dest != null) {
                data.destination = new ConfigController.DestinationPoint();
                String pos = (String) dest.get("position");
                if (pos != null) {
                    double[] coords = parseDMSPosition(pos);
                    data.destination.lat = coords[0];
                    data.destination.lon = coords[1];
                    data.destination.latDMS = formatDMS(coords[0], true);
                    data.destination.lonDMS = formatDMS(coords[1], false);
                }
                data.destination.radiusNm = parseDistance((String) dest.get("radius"));
                data.destination.bins = getInt(dest, "bins", 360);
                data.destination.binSizeNm = parseDistance((String) dest.get("binSize"));
                data.destination.stepHours = getDouble(dest, "stepHours", 0.1);
            }

            // Expand
            Map<String, Object> expand = (Map<String, Object>) routeMap.get("expand");
            if (expand != null) {
                data.expand = new ConfigController.ExpandSettings();
                data.expand.distanceNm = parseDistance((String) expand.get("distance"));
                data.expand.bins = getInt(expand, "bins", 360);
                data.expand.binSizeNm = parseDistance((String) expand.get("binSize"));
                data.expand.stepHours = getDouble(expand, "stepHours", 0.1);
            }

            // Obstructions
            List<Map<String, String>> obs = (List<Map<String, String>>) routeMap.get("obstructions");
            if (obs != null) {
                data.obstructions = obs.stream()
                    .map(m -> {
                        ConfigController.ObstructionArea area = new ConfigController.ObstructionArea();
                        area.name = m.get("name");
                        area.polygon = m.get("polygon");
                        return area;
                    })
                    .collect(Collectors.toList());
            }
        }

        return data;
    }

    private void parseRouteString(String route, ConfigController.ConfigData data) {
        data.weatherFiles = new ArrayList<>();
        data.obstructions = new ArrayList<>();

        for (String line : route.split("\n")) {
            String trimmed = line.trim();

            if (trimmed.startsWith("Using Polar:")) {
                data.polar = trimmed.substring("Using Polar:".length()).trim();
            } else if (trimmed.startsWith("Using Wind:")) {
                data.weatherFiles.add(new ConfigController.WeatherFile("Wind",
                    trimmed.substring("Using Wind:".length()).trim()));
            } else if (trimmed.startsWith("Using Tide:")) {
                data.weatherFiles.add(new ConfigController.WeatherFile("Tide",
                    trimmed.substring("Using Tide:".length()).trim()));
            } else if (trimmed.startsWith("Using Waves:")) {
                data.weatherFiles.add(new ConfigController.WeatherFile("Waves",
                    trimmed.substring("Using Waves:".length()).trim()));
            } else if (trimmed.startsWith("Using Current:")) {
                data.weatherFiles.add(new ConfigController.WeatherFile("Current",
                    trimmed.substring("Using Current:".length()).trim()));
            } else if (trimmed.startsWith("Depart:")) {
                data.departure = parseDepartLine(trimmed);
            } else if (trimmed.startsWith("Destination:")) {
                data.destination = parseDestinationLine(trimmed);
            } else if (trimmed.startsWith("Expand:")) {
                data.expand = parseExpandLine(trimmed);
            } else if (trimmed.startsWith("Obstruction:")) {
                ConfigController.ObstructionArea obs = new ConfigController.ObstructionArea();
                obs.polygon = trimmed.substring("Obstruction:".length()).trim();
                data.obstructions.add(obs);
            }
        }
    }

    private ConfigController.DeparturePoint parseDepartLine(String line) {
        ConfigController.DeparturePoint dp = new ConfigController.DeparturePoint();
        // Format: "Depart: 50*34'39"N 002*24'26"W 2025/11/25 15:00 UTC"
        String content = line.substring("Depart:".length()).trim();
        String[] parts = content.split(" ");
        if (parts.length >= 2) {
            double[] coords = parseDMSPosition(parts[0] + " " + parts[1]);
            dp.lat = coords[0];
            dp.lon = coords[1];
            dp.latDMS = parts[0];
            dp.lonDMS = parts[1];
        }
        if (parts.length >= 5) {
            dp.time = parts[2] + " " + parts[3] + " " + parts[4];
        }
        return dp;
    }

    private ConfigController.DestinationPoint parseDestinationLine(String line) {
        ConfigController.DestinationPoint dp = new ConfigController.DestinationPoint();
        String content = line.substring("Destination:".length()).trim();
        String[] parts = content.split(" ");
        if (parts.length >= 2) {
            double[] coords = parseDMSPosition(parts[0] + " " + parts[1]);
            dp.lat = coords[0];
            dp.lon = coords[1];
            dp.latDMS = parts[0];
            dp.lonDMS = parts[1];
        }
        // Parse remaining fields with defaults
        dp.radiusNm = 0.2;
        dp.bins = 360;
        dp.binSizeNm = 1.0;
        dp.stepHours = 0.1;
        return dp;
    }

    private ConfigController.ExpandSettings parseExpandLine(String line) {
        ConfigController.ExpandSettings es = new ConfigController.ExpandSettings();
        // Default values
        es.distanceNm = 20;
        es.bins = 360;
        es.binSizeNm = 1.0;
        es.stepHours = 0.1;
        return es;
    }

    private double[] parseDMSPosition(String pos) {
        // Format: "50*34'39"N 002*24'26"W" (DMS with seconds)
        double lat = 0, lon = 0;
        try {
            String[] parts = pos.split(" ");
            if (parts.length >= 2) {
                // Use DMS parsing methods that handle seconds format
                lat = Fix.parseLatitudeDMS(parts[0]);
                lon = Fix.parseLongitudeDMS(parts[1]);
            }
        } catch (Exception e) {
            logger.warn("Error parsing DMS position: {}", pos);
        }
        return new double[]{lat, lon};
    }

    private double parseDistance(String dist) {
        if (dist == null) return 1.0;
        try {
            // Remove units like "nm", "m", etc.
            String num = dist.replaceAll("[^0-9.]", "");
            return Double.parseDouble(num);
        } catch (Exception e) {
            return 1.0;
        }
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return defaultValue;
    }

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).doubleValue();
        return defaultValue;
    }

    private Map<String, Object> configToYaml(ConfigController.ConfigData config) {
        Map<String, Object> yaml = new LinkedHashMap<>();

        yaml.put("screen", config.screen != null ? config.screen : "53*00'N 11*30'W 37*00'N 0*0'W");
        yaml.put("dataRoot", "./database/");

        Map<String, Object> route = new LinkedHashMap<>();
        route.put("polar", config.polar);

        // Using
        if (config.weatherFiles != null && !config.weatherFiles.isEmpty()) {
            List<Map<String, String>> using = config.weatherFiles.stream()
                .map(wf -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("type", wf.type);
                    m.put("file", wf.file);
                    return m;
                })
                .collect(Collectors.toList());
            route.put("using", using);
        }

        // Depart
        if (config.departure != null) {
            Map<String, Object> depart = new LinkedHashMap<>();
            String pos = config.departure.latDMS != null && config.departure.lonDMS != null ?
                config.departure.latDMS + " " + config.departure.lonDMS :
                formatDMS(config.departure.lat, true) + " " + formatDMS(config.departure.lon, false);
            depart.put("position", pos.replace(" ", "\" ").replace("\"\"", "\""));
            depart.put("time", config.departure.time);
            route.put("depart", depart);
        }

        // Expand
        if (config.expand != null) {
            Map<String, Object> expand = new LinkedHashMap<>();
            expand.put("distance", config.expand.distanceNm + " nm");
            expand.put("bins", config.expand.bins);
            expand.put("binSize", config.expand.binSizeNm + " nm");
            expand.put("stepHours", config.expand.stepHours);
            route.put("expand", expand);
        }

        // Obstructions
        if (config.obstructions != null && !config.obstructions.isEmpty()) {
            List<Map<String, String>> obs = config.obstructions.stream()
                .map(o -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    if (o.name != null) m.put("name", o.name);
                    m.put("polygon", o.polygon);
                    return m;
                })
                .collect(Collectors.toList());
            route.put("obstructions", obs);
        }

        // Destination
        if (config.destination != null) {
            Map<String, Object> dest = new LinkedHashMap<>();
            String pos = config.destination.latDMS != null && config.destination.lonDMS != null ?
                config.destination.latDMS + " " + config.destination.lonDMS :
                formatDMS(config.destination.lat, true) + " " + formatDMS(config.destination.lon, false);
            dest.put("position", pos.replace(" ", "\" ").replace("\"\"", "\""));
            dest.put("radius", config.destination.radiusNm + " nm");
            dest.put("bins", config.destination.bins);
            dest.put("binSize", config.destination.binSizeNm + " nm");
            dest.put("stepHours", config.destination.stepHours);
            route.put("destination", dest);
        }

        yaml.put("route", route);
        return yaml;
    }

    /**
     * Format decimal degrees as DMS string.
     */
    public static String formatDMS(double decimal, boolean isLatitude) {
        char dir;
        if (isLatitude) {
            dir = decimal >= 0 ? 'N' : 'S';
        } else {
            dir = decimal >= 0 ? 'E' : 'W';
        }
        decimal = Math.abs(decimal);

        int degrees = (int) decimal;
        double minFloat = (decimal - degrees) * 60;
        int minutes = (int) minFloat;
        int seconds = (int) Math.round((minFloat - minutes) * 60);

        // Handle overflow
        if (seconds == 60) {
            seconds = 0;
            minutes++;
        }
        if (minutes == 60) {
            minutes = 0;
            degrees++;
        }

        return String.format("%02d*%02d'%02d\"%c", degrees, minutes, seconds, dir);
    }
}
