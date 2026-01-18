package uk.co.sexeys;

import uk.co.sexeys.JIM.CrossTrack;
import uk.co.sexeys.JIM.JIM;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.waypoint.Depart;
import uk.co.sexeys.waypoint.Waypoint;
import uk.co.sexeys.wind.Wind;

/**
 * Service that encapsulates route calculation and optimization.
 * This separates the routing business logic from the UI layer.
 */
public class RoutingService {

    private final Wind wind;
    private final Water water;
    private final Boat boat;
    private JIM jim;
    private DifferentialEvolution differentialEvolution;

    public RoutingService(Boat boat, Wind wind, Water water) {
        this.boat = boat;
        this.wind = wind;
        this.water = water;
    }

    /**
     * Calculate a new route using the JIM (Jim's Isochrone Method) algorithm.
     */
    public void calculateRoute(long cutoffTime) throws Exception {
        jim = new CrossTrack(boat, wind, water);
        jim.SearchInit();
        jim.Search(jim.route.currentTime + cutoffTime);

        int temp = boat.currentWaypoint;
        Obstruction.active = boat.waypoints[1].obstructions;
        boat.currentWaypoint = temp;
    }

    /**
     * Initialize differential evolution optimization for fine-tuning the route.
     */
    public void initializeOptimization(int legs, long searchPeriod, float tolerance, BiLinear polar) {
        if (jim == null) {
            throw new IllegalStateException("Must calculate route before optimization");
        }
        differentialEvolution = new DifferentialEvolution(
                legs,
                ((Depart) boat.waypoints[0]).getTime() + searchPeriod,
                jim, wind, water, boat, tolerance, 1234, polar
        );
    }

    /**
     * Run optimization iterations.
     *
     * @param iterations Number of iterations to run
     * @param factor     Evolution factor (0.0-1.0)
     */
    public void optimize(int iterations, float factor) {
        if (differentialEvolution == null) {
            throw new IllegalStateException("Must initialize optimization first");
        }
        differentialEvolution.search(iterations, factor, 0);
    }

    /**
     * Double the number of waypoints for finer route optimization.
     */
    public void doubleWaypoints() {
        if (differentialEvolution == null) {
            return;
        }
        DifferentialEvolution oldDE = differentialEvolution;
        differentialEvolution = new DifferentialEvolution(oldDE, boat);
        differentialEvolution.generateDoubleAgents(oldDE);
        differentialEvolution.recomputeErrors();
    }

    /**
     * Halve the route length to a given time.
     */
    public void halveRouteLength() {
        if (differentialEvolution == null) {
            return;
        }
        long t = differentialEvolution.getLastFix().time - differentialEvolution.getInitialFix().time;
        Fix f = differentialEvolution.findNearestFix(t / 2 + differentialEvolution.getInitialFix().time);
        // Additional implementation needed for full halve operation
    }

    /**
     * Get the current route time.
     */
    public long getRouteTime() {
        if (differentialEvolution != null) {
            return differentialEvolution.getTime();
        }
        if (jim != null) {
            return jim.GetTime();
        }
        return 0;
    }

    /**
     * Get the elapsed time from departure.
     */
    public long getElapsedTime() {
        if (differentialEvolution != null) {
            return differentialEvolution.getElapsedTime();
        }
        return 0;
    }

    /**
     * Get route instructions as text.
     */
    public String getInstructions() {
        if (differentialEvolution != null) {
            return differentialEvolution.PrintInstructions();
        }
        return "";
    }

    /**
     * Get short route instructions.
     */
    public String getShortInstructions() {
        if (differentialEvolution != null) {
            return differentialEvolution.PrintShortInstructions();
        }
        return "";
    }

    /**
     * Export route as GPX.
     */
    public String exportGPX(String title) {
        if (differentialEvolution != null) {
            return differentialEvolution.GPX(title);
        }
        if (jim != null) {
            return jim.GPX(title);
        }
        return "";
    }

    // Getters for rendering access

    public JIM getJim() {
        return jim;
    }

    public DifferentialEvolution getDifferentialEvolution() {
        return differentialEvolution;
    }

    public Wind getWind() {
        return wind;
    }

    public Water getWater() {
        return water;
    }

    public Boat getBoat() {
        return boat;
    }

    /**
     * Change the departure time.
     */
    public void changeDepartureTime(long deltaMs) {
        if (boat.waypoints[0] instanceof Depart) {
            ((Depart) boat.waypoints[0]).ChangeTime(deltaMs);
        }
    }

    /**
     * Recycle resources when done with current optimization.
     */
    public void recycle() {
        if (differentialEvolution != null) {
            differentialEvolution.Recycle();
        }
    }
}
