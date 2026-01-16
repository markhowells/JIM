package uk.co.sexeys.JIM;

import uk.co.sexeys.Obstruction;
import uk.co.sexeys.waypoint.Waypoint;

import java.util.LinkedList;

public class Route {
    public Leg[] legs;
    public long timestep;
    public int currentLeg;
    public long currentTime;
    public long legElapsedTime;

    Route(Waypoint[] waypoints) {
        this.legs = new Leg[waypoints.length];
        for (int i = 0; i < waypoints.length; i++) {
            this.legs[i] = new Leg(waypoints[i]);
        }
    }
    public static class Leg{
        public LinkedList<Agent>[] greatCircleBins; // working memory
        float[] progress; // This is a cost. Starts high and goes down.
        public Waypoint waypoint;
        Leg(Waypoint w){
            waypoint = w;
            greatCircleBins = new LinkedList[w.numberOfBins];
            progress = new float[w.numberOfBins];
            for (int i = 0; i < w.numberOfBins; i++) {
                greatCircleBins[i] = new LinkedList<>();
                progress[i] = Float.MAX_VALUE;
            }
        }
    }
}
