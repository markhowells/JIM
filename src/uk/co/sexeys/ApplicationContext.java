package uk.co.sexeys;

import uk.co.sexeys.water.Water;
import uk.co.sexeys.wind.Wind;

/**
 * Application context that manages services and provides a unified API
 * for the UI layer. This is the main entry point for business logic.
 */
public class ApplicationContext {

    private final WeatherService weatherService;
    private final ChartService chartService;
    private RoutingService routingService;
    private Boat boat;

    public ApplicationContext() {
        this.weatherService = new WeatherService();
        this.chartService = new ChartService();
    }

    /**
     * Initialize the boat with waypoints and polar data.
     */
    public void initializeBoat(Boat boat) {
        this.boat = boat;
    }

    /**
     * Initialize routing with current weather and boat settings.
     */
    public void initializeRouting() {
        if (boat == null) {
            throw new IllegalStateException("Boat must be initialized first");
        }
        Wind wind = weatherService.getWind();
        Water water = weatherService.getWater();
        if (wind == null || water == null) {
            throw new IllegalStateException("Weather data must be loaded first");
        }
        routingService = new RoutingService(boat, wind, water);
    }

    /**
     * Calculate a new route.
     */
    public void calculateRoute(long cutoffTime) throws Exception {
        if (routingService == null) {
            initializeRouting();
        }
        routingService.calculateRoute(cutoffTime);
    }

    /**
     * Run route optimization.
     */
    public void optimizeRoute(int legs, long searchPeriod, float tolerance, int iterations, float factor) {
        if (routingService == null) {
            throw new IllegalStateException("Routing must be initialized first");
        }
        if (routingService.getDifferentialEvolution() == null) {
            routingService.initializeOptimization(legs, searchPeriod, tolerance, boat.polar.raw);
        }
        routingService.optimize(iterations, factor);
    }

    /**
     * Change departure time and recalculate.
     */
    public void changeDepartureTime(long deltaMs) throws Exception {
        if (routingService != null) {
            routingService.changeDepartureTime(deltaMs);
            calculateRoute(Main.JIMCutoff);
        }
    }

    /**
     * Get route instructions.
     */
    public String getRouteInstructions() {
        if (routingService != null) {
            return routingService.getInstructions();
        }
        return "";
    }

    /**
     * Export route as GPX.
     */
    public String exportGPX(String title) {
        if (routingService != null) {
            return routingService.exportGPX(title);
        }
        return "";
    }

    /**
     * Get current route time.
     */
    public long getRouteTime() {
        if (routingService != null) {
            return routingService.getRouteTime();
        }
        return 0;
    }

    // Service accessors

    public WeatherService getWeatherService() {
        return weatherService;
    }

    public ChartService getChartService() {
        return chartService;
    }

    public RoutingService getRoutingService() {
        return routingService;
    }

    public Boat getBoat() {
        return boat;
    }

    /**
     * Set boat for testing or external initialization.
     */
    public void setBoat(Boat boat) {
        this.boat = boat;
    }
}
