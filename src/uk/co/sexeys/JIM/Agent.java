package uk.co.sexeys.JIM;

import uk.co.sexeys.Fix;
import uk.co.sexeys.Stamina;
import uk.co.sexeys.Vector2;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.waypoint.Depart;
import uk.co.sexeys.waypoint.InterimFix;
import uk.co.sexeys.wind.Wind;

import java.util.LinkedList;
import java.util.Random;

public abstract class Agent {
    int bin;
    float cost;
    public float heading;
    public Vector2 position = new Vector2();
    Wind.SOURCE windSource;
    Water.SOURCE waterSource;
    public int sail;
    public long time;
    public Vector2 wind = new Vector2();
    public Vector2 tide = new Vector2();
    // Both of these are the velocity TO the waypoint. NOT the velocity AT the waypoint.
    public Vector2 logV = new Vector2(); // log velocity aka velocity through water
    public Vector2 VOG = new Vector2(); // velocity over ground

    public Agent previousAgent;
    public boolean portTack;
    boolean closeHauled;
    //        boolean applyPenalty = false;
    float cosLatitude;
    float sinLatitude;

    Stamina stamina;
    int currentLeg = 0;
    long nextTimestep;

    Agent() {}

    Agent(Agent agent) {  //TODO Not a lot copied...
        this.previousAgent  = agent;
        if (null != agent) {
            this.stamina = new Stamina(agent.stamina);
            this.currentLeg = agent.currentLeg;
        }
        else
            this.stamina = new Stamina();
    }
    void setSinCos() {
        cosLatitude = (float) Math.cos(position.y);
        sinLatitude = (float) Math.sin(position.y);
    }

    public Depart ToDepart() {
        Depart w = new Depart();
        w.position = new Vector2(position);
        w.time=time;
        return w;
    }

    public InterimFix ToInterimFix() {
        InterimFix w = new InterimFix();
        w.position = new Vector2(position);
        w.time=time;
        w.heading = heading;
        w.sail = sail;
        w.portTack = portTack;
        w.closeHauled = closeHauled;
        return w;
    }
     public Fix ToFix() {
        Fix f = new Fix();
        f.position = new Vector2(position);
        f.time = time;
        f.cosLatitude = cosLatitude;
        f.sinLatitude = sinLatitude;
        f.heading = new Vector2(heading);
        f.wind = new Vector2(wind);
        f.tide = new Vector2(tide);
        f.portTack = portTack;
        f.closeHauled = closeHauled;
        f.sail = sail;
        f.stamina = new Stamina(stamina);
        return f;
    }

    public Agent FirstAgent() {
        Agent Y = this;
        while(Y.previousAgent != null) {
            Y = Y.previousAgent;
        }
        return Y;
    }

    Fix FindNearestFix(long time) {
        Agent a = FindNearestAgent(time);
        return a.ToFix();
    }



    public Agent FindNearestAgent(long time) {
        Agent Y = this;
        while(true) {
            if(Y.time <= time)
                break;
            if(Y.previousAgent == null)
                break;
            Y = Y.previousAgent;
        }
        return Y;
    }

    public Agent findNearestAgent(Vector2 p) {
        Agent Y = this;
        Agent a = null;
        double minRange = Double.MAX_VALUE;
        while(true) {
            double r = Fix.range(Y.position,p);
            if(r < minRange) {
                minRange = r;
                a = Y;
            }
            if(Y.previousAgent == null)
                break;
            Y = Y.previousAgent;
        }
        return a;
    }


    public Vector2[] GenerateWayPoints(int N, long time, Random random) {
        Vector2[] waypoints = new Vector2[N+1];
        Agent first = FirstAgent();
        waypoints[0] = new Vector2(first.position);
        Agent last = FindNearestAgent(time);
        waypoints[N] = new Vector2(last.position);
        LinkedList<Agent> track = new LinkedList<>();
        Agent Y = last;
        while(Y!= null) {
            track.addFirst(Y);
            Y = Y.previousAgent;
        }
        // deliberately allow waypoints to overlap to increase the randomness...
        int[] res = Agent.sampleRandomNumbersWithoutRepetition(0,track.size() ,N-1, random );
        for(int i = 0; i < res.length; i++) {
            Agent t = track.get(res[i]);
            waypoints[i+1] = new Vector2(t.position);
        }
        return waypoints;
    }
    public static Vector2[] GenerateWayPoints(int N, Agent first, Agent last, Random random) {
        Vector2[] waypoints = new Vector2[N+1];
        waypoints[0] = new Vector2(first.position);
        waypoints[N] = new Vector2(last.position);
        LinkedList<Agent> track = new LinkedList<>();
        Agent Y = last;
        do {
            track.addFirst(Y);
            Y = Y.previousAgent;
        } while(Y!= first);

        // deliberately allow waypoints to overlap to increase the randomness...
        int[] res = sampleRandomNumbersWithoutRepetition(0,track.size() ,N-1 , random);
        for(int i = 0; i < res.length; i++) {
            Agent t = track.get(res[i]);
            waypoints[i+1] = new Vector2(t.position);
        }
        return waypoints;
    }

    public static int[] sampleRandomNumbersWithoutRepetition(int start, int end, int count, Random rng) {

        int[] result = new int[count];
        int cur = 0;
        int remaining = end - start;
        for (int i = start; i < end && count > 0; i++) {
            double probability = rng.nextDouble();
            if (probability < ((double) count) / (double) remaining) {
                count--;
                result[cur++] = i;
            }
            remaining--;
        }
        return result;
    }
}
