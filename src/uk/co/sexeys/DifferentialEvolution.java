package uk.co.sexeys;

import uk.co.sexeys.JIM.*;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.waypoint.Depart;
import uk.co.sexeys.waypoint.InterimFix;
import uk.co.sexeys.waypoint.Waypoint;
import uk.co.sexeys.wind.Wind;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Jim on 12/01/2019.
 *
 */
class DifferentialEvolution {
    class Agent {
        Vector2[] waypoint; // Only position is part of search
        final LinkedList<Fix> track = new LinkedList<>();
        final Fix stub; // first part of track
        long error; // time

        Agent(){
            stub = null;
            error = Long.MAX_VALUE;
        }

        Agent(Agent agent) {
            waypoint = new Vector2[agent.waypoint.length];
            System.arraycopy(agent.waypoint, 0, waypoint, 0, waypoint.length);
            for(Fix f: agent.track)
                track.add(new Fix(f));
            stub = CopyStub(track);
            error = agent.error;
        }

        Agent(long time, JIM jim, Random generator) {
            int count = 0;
            stub = CopyStub(track);
            do {
                waypoint = jim.keyAgent.GenerateWayPoints(legs,time,generator);
                try {
                    ComputeError();
                } catch (Exception e) {
                    error = Long.MAX_VALUE;
                }
                if(count > 1000000)
                    throw new RuntimeException("Could not assign C2S waypoints from JIM track.");
                count++;
            } while (error == Long.MAX_VALUE);
            System.out.print("\r"+Main.spinner[(Main.spinnerCounter++)%4]);
        }

        Agent(uk.co.sexeys.JIM.Agent first, uk.co.sexeys.JIM.Agent last, Random generator) {
            int count = 0;
            stub = CopyStub(track);
            do {
                waypoint = uk.co.sexeys.JIM.Agent.GenerateWayPoints(legs,first,last,generator);
                try {
                    ComputeError();
                } catch (Exception e) {
                    error = Long.MAX_VALUE;
                }
                if(count > 1000)
                    throw new RuntimeException("Could not assign C2S waypoints from MIM track.");
                count++;
            } while (error == Long.MAX_VALUE);
            System.out.print("\r"+Main.spinner[(Main.spinnerCounter++)%4]);
        }


        Agent(int N, Vector2 A, Vector2 B) {
            waypoint = new Vector2[N+1];
            waypoint[0] = A;
            for (int i = 1; i < N; i++) {
                waypoint[i] = new Vector2();
            }
            waypoint[N] = B;
            stub = CopyStub(track);
            error =  Long.MAX_VALUE;
        }
        Agent(int maxTries) {
            waypoint = new Vector2[legs+1];
            waypoint[0] = depart.position;
            waypoint[legs] = destination.position;
            stub = CopyStub(track);
            Vector2 dx = destination.position.minus(depart.position);
            Vector2 dy = dx.Perpendicular();
            Vector2 trialWaypoint;
            int j;
            int tries;
            do {
                tries = 0;
                Fix.recycle(track,stub);
                error = Long.MAX_VALUE;
                for (j = 1; j < legs && tries < maxTries; j++) {
                    while(tries < maxTries) {
                        trialWaypoint = depart.position.plus(dx.scale((double)(j-1)/(legs-2) + (1*generator.nextDouble()-0.5))
                                .plus(dy.scale(Main.routeAspectRatio*generator.nextDouble()-Main.routeAspectRatio/2)));
                        try {
                            boat.courseToSteer(wind,water,Main.shoreline, trialWaypoint,track,resolution,tolerance,10);
                            waypoint[j] = new Vector2(track.getLast().position);
                            if(j == legs -1) {
                                boat.courseToSteer(wind,water,Main.shoreline,destination.position,track,resolution,tolerance,10);
                            }
                            break;
                        } catch (Exception e) {
                            tries++;
                        }
                    }
                }
                if(tries >= maxTries)
                    continue;
                error = track.getLast().time - stub.time;
            } while (error == Long.MAX_VALUE);
            if(error < bestAgent.error)
                bestAgent = this;
        }

        void ComputeError() {
            Fix.recycle(track,stub);
            try {
                for (int j = 1; j < waypoint.length; j++) {
                    boat.courseToSteer(wind, water, Main.shoreline, waypoint[j], track, resolution, tolerance, 10);
                }
                error = track.getLast().time - stub.time;
            } catch (Exception e) {
                error =  Long.MAX_VALUE;
            }
        }


//        boolean CheckFirstWaypoint() {
//            final ArrayList<Fix> with = new ArrayList<>();
//            final ArrayList<Fix> without = new ArrayList<>();
//            try {
//                boat.courseToSteer(wind, tide, Main.shoreline, waypoint[0], waypoint[2].position, without, resolution, tolerance, 100);
//                boat.courseToSteer(wind, tide, Main.shoreline, waypoint[0], waypoint[1].position, with, resolution, tolerance, 100);
//                boat.courseToSteer(wind, tide, Main.shoreline, with.get(with.size()-1), waypoint[2].position, with, resolution, tolerance, 100);
//                return(with.get(with.size()-1).time > without.get(without.size()-1).time);
//            } catch (Exception e) {
//                return false;
//            }
//        }
    }

    Agent[] population;
    Agent spare;
    public final Waypoint depart, destination;
    private final LinkedList<Fix> stub;
    private final float tolerance;
    private int legs;
    public final Boat boat;
    private final Wind wind;
    private final Water water;
    private long resolution;
    private final Random generator;
    Agent bestAgent;
    private BiLinear polar;
    private Vector2 BmA;

    DifferentialEvolution(int legs, final Wind wind, final Water water, final Boat boat, Waypoint depart, Waypoint destination,
                          long resolution, float tolerance, long seed, BiLinear polar) {
        this.legs = legs;
        this.wind = wind;
        this.water = water;
        this.boat = boat;
        this.depart = depart;
        this.destination = destination;
        stub = CreateStub();
        this.resolution = resolution;
        this.tolerance = tolerance *  phys.rReciprocal;
        generator = new Random(seed);
        this.polar = polar;
    }
    DifferentialEvolution(int legs, long time, JIM modifiedIsochrone, final Wind wind, final Water water, final Boat boat,
                          float tolerance, long seed, BiLinear polar) {
        this.legs = legs;
        this.wind = wind;
        this.water = water;
        this.boat = boat;
        this.depart = modifiedIsochrone.boat.waypoints[0];
        this.destination = modifiedIsochrone.keyAgent.FindNearestAgent(time).ToDepart();
        stub = CreateStub();
        resolution = (((Depart)destination).time - ((Depart)depart).time)/ Main.numberOfFixes;

        this.tolerance = tolerance *  phys.rReciprocal;
        generator = new Random(seed);
        this.polar = polar;
        Obstruction.active = boat.waypoints[1].obstructions;

        population = new Agent[Main.C2SAgents];
        bestAgent = new Agent();

        for (int i = 0; i < population.length; i++) {
            System.out.print("\r"+i);
            population[i] = new Agent(time, modifiedIsochrone, generator);
            if(population[i].error < bestAgent.error)
            {
                bestAgent = population[i];
            }
        }
        spare = new Agent(legs,depart.position,destination.position);
    }

    DifferentialEvolution(int legs, uk.co.sexeys.JIM.Agent first, uk.co.sexeys.JIM.Agent last,final Wind wind, final Water water, final Boat boat,
                          float tolerance, long seed, BiLinear polar) {
        this.legs = legs;
        this.wind = wind;
        this.water = water;
        this.boat = boat;
        this.depart = first.ToInterimFix();
        this.destination = last.ToDepart();
        stub = CreateStub();
        resolution = (last.time - first.time)/ Main.numberOfFixes;

        this.tolerance = tolerance *  phys.rReciprocal;
        generator = new Random(seed);
        this.polar = polar;
        population = new Agent[Main.C2SAgents];
        bestAgent = new Agent();
        for (int i = 0; i < population.length; i++) {
            population[i] = new Agent(first, last, generator);
            if(population[i].error < bestAgent.error)
                bestAgent = population[i];
        }
        spare = new Agent(legs,depart.position,destination.position);
    }


    DifferentialEvolution(final DifferentialEvolution DE, Boat boat) {
        legs = DE.legs;
        wind = DE.wind;
        water = DE.water;
        this.boat = boat;
        depart = DE.depart;
        destination = DE.destination;
        stub = DE.stub;
        resolution = DE.resolution;
        tolerance = DE.tolerance;
        generator = DE.generator;
        bestAgent = DE.bestAgent;
        population = new Agent[DE.population.length];
        for (int i = 0; i < population.length; i++) {
            population[i] = new Agent(DE.population[i]);
        }
        spare = new Agent(legs,depart.position,destination.position);
        polar = DE.polar;
        BmA = DE.BmA;
    }

    void Recycle() {
        for (Agent a: population) {
            Fix.recycle(a.track);
        }
    }

    LinkedList<Fix> CreateStub() {
        LinkedList<Fix> track = new LinkedList<>();
        if(depart instanceof InterimFix) {  // TODO why not copy evertything?
            Fix f = new Fix();
            f.position.x = Float.NaN;
            f.sail = ((InterimFix) depart).sail;
            f.portTack = ((InterimFix) depart).portTack;
            f.stamina = new Stamina(((InterimFix) depart).stamina);
            track.add(f);
        }
        Fix f = new Fix();
        f.position.x = depart.position.x;
        f.position.y = depart.position.y;
        f.setSinCos();
        f.time = ((Depart)depart).time;
        if(depart instanceof InterimFix)
            f.stamina = new Stamina(((InterimFix) depart).stamina);
        else
            f.stamina = new Stamina();
        Vector2 positionDegrees = f.position.scale(phys.degrees);
        f.windSource = wind.getValue(positionDegrees, f.time, f.wind);
        if( Main.useWater)
            f.waterSource = water.getValue(positionDegrees, f.time, f.tide);
        track.add(f);
        return track;
    }

    Fix CopyStub(LinkedList<Fix> track) {
        track.clear();
        for (Fix f: stub) {
            track.add(new Fix(f));
        }
        return track.getLast();
    }



//    void copy24HoursAhead(DifferentialEvolution DE) {
//        long test = depart.time + 12* phys.msPerHour;
//        for(Fix f: DE.bestAgent.track) {
//            if(f.time > test) {
//                destination = new Fix(f);
//                if(population != null){
//                    int N = population[0].waypoint.length-1;
//                    for(Agent a:population)
//                        a.waypoint[N] = f;
//                }
//                return;
//            }
//        }
//        destination = new Fix(DE.destination);
//        if(population != null){
//            int N = population[0].waypoint.length-1;
//            for(Agent a:population)
//                a.waypoint[N] = destination;
//        }
//    }

    void generateAgents() {
        boat.polarToUse = polar;
        final ArrayList<Agent> agents = new ArrayList<>();
        int agentNumber = 0;
        bestAgent = new Agent();
        while (agentNumber < Main.C2SAgents) {
            System.out.print(".");
            Agent a = new Agent(10);
            agents.add(a);
            agentNumber++;
        }
        System.out.println();
        population = agents.toArray(new Agent[0]);
        spare = new Agent(legs,depart.position,destination.position);
    }

    // Generate agents using previous solution as a guide
    void generateAgents(DifferentialEvolution DE) {
        boat.polarToUse = polar;
        population = new Agent[DE.population.length];
        for (int i = 0; i < DE.population.length; i++) {
            System.out.print(".");
            LinkedList<Fix> DETrack = DE.population[i].track;
            int start = findNearestFix(DETrack, depart.position);
            int end = findNearestFix(DETrack, destination.position);
            population[i] = new Agent(legs, depart.position, destination.position);
//            int nextWaypoint = findNearestFix(DE.population[i].track,depart.position)+1; // NOte track used to be waypoint
            for (int j = 1; j < legs; j++) {
                InterimFix test = new InterimFix(DETrack.get(start + (j*(end- start))/legs));
//                if(DE.population[i].waypoint[nextWaypoint].time < test.time) { //Not sure what this did
//                    population[i].waypoint[j] = DE.population[i].waypoint[nextWaypoint];
//                    nextWaypoint++;
//                }
//                else
                    population[i].waypoint[j] = test.position;
            }
        }
        spare = new Agent(legs,depart.position,destination.position);
        System.out.println();
    }


    void generateDoubleAgents(DifferentialEvolution DE) {
        boat.polarToUse = polar;
        population = new Agent[DE.population.length];
        legs = DE.legs * 2;
        Vector2 p;

        double minRange;
        LinkedList<Fix> DETrack;
        for (int i = 0; i < DE.population.length; i++) {
            System.out.print(".");
            population[i] = new Agent(legs, depart.position, destination.position);

            for (int j = 1; j < DE.legs; j++) {
                population[i].waypoint[j*2] = new Vector2(DE.population[i].waypoint[j]);
            }
            DETrack = DE.population[i].track;
            for (int j = 0; j < legs; j+=2) {
                p = population[i].waypoint[j].plus(population[i].waypoint[j+2]);
                p.scaleIP(0.5f);

                minRange = Double.MAX_VALUE;
                for (int k = 0; k < DETrack.size(); k++) {
                    double r = Fix.range(DETrack.get(k).position,p);
                    if(r < minRange) {
                        minRange = r;

                    }
                }
//                population[i].waypoint[j+1] = new Vector2(DETrack.get(closest).position);
                population[i].waypoint[j+1] = p; // TODO this could be a mistake when tides are added back in.
//                if(((Depart)population[i].waypoint[j+1]).time == ((Depart)population[i].waypoint[j]).time) {
//                    population[i].waypoint[j+1] = p;
//                }
            }
        }
        spare = new Agent(legs,depart.position,destination.position);
    }

    void search(int numberOfIterations, float factor, int sleepHours) {
        Agent[] newPopulation;
        Agent Ac,  best = new Agent();
        int i;
        boat.polarToUse = polar;
        for(int iteration = 0; iteration < numberOfIterations; iteration++ ) {
            System.out.print(".");
            newPopulation = new Agent[population.length];
            i = 0;
            int a,b,c,R;
            float r;
            for (Agent X: population) {
                do
                    a = generator.nextInt(population.length);
                while (population[a] == X);
                do
                    b = generator.nextInt(population.length);
                while (population[b] == X || a == b);
                do
                    c = generator.nextInt(population.length);
                while (population[c] == X || b == c || a == c);

                R = generator.nextInt(legs*2);
                Ac = spare;
                for (int j = 1; j < legs; j++) {
                    r = generator.nextFloat();
                    if (r < Main.C2SCR || (j*2) == R) {
                        Ac.waypoint[j].x = population[a].waypoint[j].x  +
                                factor * (population[b].waypoint[j].x - population[c].waypoint[j].x);
                    }
                    else {
                        Ac.waypoint[j].x = X.waypoint[j].x ;
                    }
                    r = generator.nextFloat();
                    if (r < Main.C2SCR || (j*2+1) == R) {
                        Ac.waypoint[j].y = population[a].waypoint[j].y  +
                                factor * (population[b].waypoint[j].y - population[c].waypoint[j].y);
                    }
                    else {
                        Ac.waypoint[j].y = X.waypoint[j].y ;
                    }
                }

                Ac.ComputeError();

                if ( Ac.error < X.error) {
                    newPopulation[i] = Ac; // This is the evolution
                    spare = X;
                } else {
                    newPopulation[i] = X;
                }
                if(newPopulation[i].error < best.error) {
                    best = newPopulation[i];
                }
                i++;
            }
            population = newPopulation;
        }
        bestAgent = best;
    }

//    void converge(int numberOfIterations, float factor, int sleepHours) {
//        long change = Long.MAX_VALUE;
//        long lastError = bestAgent.error;
//        while (change > phys.minute) {
//            search(numberOfIterations, factor, sleepHours);
//            change = lastError - bestAgent.error;
//            System.out.println("Change (mins): "+change/phys.minute);
//            lastError = bestAgent.error;
//        }
//    }
//    void converge( float factor, int sleepHours) {
//        double rate = Double.MAX_VALUE;
//        long lastError = bestAgent.error;
//        int iterationsSinceLastChange = 0;
//        int totalIterationsPerformed = 0;
//
//        int numberOfIterations = 10;
//
//        while (rate > 0.2 * phys.msPerSecond) {
//            totalIterationsPerformed += numberOfIterations;
//            if(totalIterationsPerformed > Main.numberOfIterations){
//                System.out.println("\n"+Main.numberOfIterations+" iterations completed.");
//                break;
//            }
//            search(numberOfIterations, factor, sleepHours);
//            long change = lastError - bestAgent.error;
//            if( change == 0) {
//                iterationsSinceLastChange += numberOfIterations;
//                continue;
//            }
//            rate = (double) change/ (iterationsSinceLastChange+numberOfIterations);
//            System.out.print("\nRate : "+(int) rate);
//            iterationsSinceLastChange = 0;
//            lastError = bestAgent.error;
//        }
//    }
//
//    void converge(float factor, int sleepHours, ScatterPlot progressPlot, List<Vector2> computePoints) {
//        double rate = Double.MAX_VALUE;
//        long lastError = bestAgent.error;
//        int iterationsSinceLastChange = 0;
//        int totalIterationsPerformed = 0;
//
//        int numberOfIterations = 10;
//
//        while (rate > 0.2 * phys.msPerSecond) {
//            totalIterationsPerformed += numberOfIterations;
//            if(totalIterationsPerformed > Main.numberOfIterations){
//                System.out.println("\n"+Main.numberOfIterations+" iterations completed.");
//                break;
//            }
//            search(numberOfIterations, factor, sleepHours);
//
//            long change = lastError - bestAgent.error;
//            if( change == 0) {
//                iterationsSinceLastChange += numberOfIterations;
//                continue;
//            }
//            computePoints.add(new Vector2(
//                    computePoints.get(computePoints.size()-1).x+iterationsSinceLastChange,
//                    boat.DE.getElapsedHours()));
//            progressPlot.scatterPanel.setLimits();
//            progressPlot.Update(computePoints);
//            rate = (double) change/ (iterationsSinceLastChange+numberOfIterations);
//            System.out.print("\nRate : "+(int) rate);
//            iterationsSinceLastChange = 0;
//            lastError = bestAgent.error;
//        }
//    }
//    void setStart (Fix start){
//        Agent[] newPopulation = new Agent[population.length];
//        int i=0;
//        Agent best = new Agent();
//        boat.polarToUse = polar;
//        depart = new Fix(start);
//
//        for (Agent X: population) {
//            X.waypoint[0] = new Fix(depart);
//            synchronized (X.track) {
//                X.ComputeError();
//                if(X.error  < best.error) {
//                    best = X;
//                }
//                newPopulation[i++] = X;
//            }
//        }
//        population = newPopulation;
//        bestAgent = best;
//    }

    void draw(Graphics2D g, Mercator screen, long time, boolean showCandidates) {
        Vector2 p,p0;

        float cutoff = 0.99F;

        if(showCandidates) {
            long bestError = bestAgent.error;
            g.setColor(Color.gray);

            LinkedList<Agent> sorted = new LinkedList<> (Arrays.asList(population));
            Collections.sort(sorted,new Comparator<Agent>() {
                @Override
                public int compare(Agent o1, Agent o2) {
                    if (o1.error > o2.error) return -1;
                    else if (o1.error < o2.error) return 1;
                    return 0;
                }
            });

            for(Agent X:sorted) {
                float dError = (float) (bestError)/X.error ;
                dError -= cutoff;
                if(dError < 0) {
                    g.setColor(Color.lightGray);
                }
                else {
                    dError/= (1-cutoff);
                    g.setColor(new Color(Color.HSBtoRGB(1.f-dError,0.3f,1)));
                    Color c = g.getColor();
                }

                for (int i = 1; i < X.track.size(); i++) {
                    if(Float.isNaN(X.track.get(i - 1).position.x))
                        continue; // TODO This is a interimfix waypoint. Would like to show somehow.
                    p0 = screen.fromRadiansToPoint(X.track.get(i-1).position);
                    p = screen.fromRadiansToPoint(X.track.get(i).position);
                    g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
                }
            }
            g.setColor(Color.black);

            for(Fix f: bestAgent.track) {
                if(Float.isNaN(f.position.x))
                    continue; // TODO This is a interimfix waypoint. Would like to show somehow.
                if(f.time > time)
                    break;
                f.draw(g, screen);
            }
//            Graphics2D g1 = (Graphics2D) g.create();
//            g1.setStroke(new BasicStroke(3));
//            lastFix.draw(g1, screen);
        }

        g.setColor(Color.BLACK);
        if(bestAgent.waypoint != null) {
            for (Vector2 w : bestAgent.waypoint) {
                int f = findNearestFix(bestAgent.track,w);
                bestAgent.track.get(f).draw(g, screen, ((Depart) depart).time);

                p = screen.fromRadiansToPoint(w);
                g.drawRect((int) p.x - 5, (int) p.y - 5, 10, 10);
            }
        }
        Graphics2D g2d = (Graphics2D) g.create();
        Stroke dashed = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        Stroke solid = new BasicStroke(3);
        long riseTime, setTime;
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        for (int i = 1; i < bestAgent.track.size(); i++) {
            Fix f = bestAgent.track.get(i-1);
            if(Float.isNaN(f.position.x))
                continue; // TODO This is a interimfix waypoint. Would like to should somehow.
            c.setTimeInMillis(f.time+phys.msPerDay);
            riseTime = Sun.riseTime(c,f.position);
            setTime = Sun.setTime(c,f.position);
            while(riseTime > f.time) {
                c.add(Calendar.DAY_OF_WEEK,-1);
                riseTime = Sun.riseTime(c,f.position);
                setTime = Sun.setTime(c,f.position);
            }
            boolean daylight = f.time <= setTime;
            if(daylight)
                g2d.setStroke(solid);
            else
                g2d.setStroke(dashed);
            Vector2 tidalWind = f.wind.minus(f.tide);
            double trueWIndSpeed = tidalWind.mag();
            if (trueWIndSpeed == 0) {
                continue;
            }
            double cosAngle = -tidalWind.dot(f.heading) / trueWIndSpeed;
            g2d.setColor(Color.black);
            if ((boat.polar.gibeAngle.interpolate(trueWIndSpeed) > cosAngle) ||  (boat.polar.tackAngle.interpolate(trueWIndSpeed) < cosAngle))
                g2d.setColor(Color.magenta);
            if(trueWIndSpeed < 5.0f / phys.knots)
                g2d.setColor(Color.red);

            p0 = screen.fromRadiansToPoint(bestAgent.track.get(i-1).position);
            p = screen.fromRadiansToPoint(bestAgent.track.get(i).position);
            g2d.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
        }

        Fix Y = null, previous = bestAgent.track.getFirst();
        for (Fix f:bestAgent.track) {
            if(f.time >= time) {
                Y = f;
                break;
            }
            previous = f;
        }
        if (null != Y) {
            g2d.setColor(Color.red);
            p0 = screen.fromRadiansToPoint(Y.position);
            p = screen.fromRadiansToPoint(previous.position);
            Vector2 dp = p0.minus(p);
            float t = (float) (time-previous.time)/(float)(Y.time - previous.time);
            p0 = p.plus(dp.scale(t));
            g2d.setStroke(solid);
            g2d.drawLine((int)p0.x, (int) p0.y-10, (int) p0.x, (int) p0.y+10);
            g2d.drawLine((int)p0.x-10, (int) p0.y, (int) p0.x+10, (int) p0.y);
        }
        g2d.dispose();
    }

    void drawTWA(Graphics2D g) {
        int[] data = new int[19];
        for(Fix f: bestAgent.track) {
            Vector2 tidalWind = f.wind.minus(f.tide);
            double trueWIndSpeed = tidalWind.mag();
            if (trueWIndSpeed == 0) {
                continue;
            }
            double cosAngle = -tidalWind.dot(f.heading) / trueWIndSpeed;
            double TWA = Math.acos(cosAngle);
            data[(int)(Math.toDegrees(TWA)/10)]++;
        }
        g.setColor(Color.BLACK);

        for(int i = 0; i < data.length; i++)
            g.drawLine(i*10,200,i*10,200+data[i]*10);
        AffineTransform orig = g.getTransform();
        g.rotate(-Math.PI/2);
        for(int i = 30; i <= 180; i+=30) {
            g.drawString(i+"", -200, i);
        }
        g.setTransform(orig);
    }

//    BiLinear getTWApolar() {
//        int[] data = new int[18];
//        for(Fix f: bestAgent.track) {
//            Vector2 tidalWind = f.wind.minus(f.tide);
//            double trueWIndSpeed = tidalWind.mag();
//            if (trueWIndSpeed == 0) {
//                continue;
//            }
//            double cosAngle = -tidalWind.dot(f.heading) / trueWIndSpeed;
//            double TWA = Math.acos(cosAngle);
//            data[(int)(Math.toDegrees(TWA)/10)]++;
//        }
//        Vector2[][] v = new Vector2[1][data.length];
//        float[] x = {0.0f};
//        for (int i = 0; i < data.length; i++) {
//            v[0][i] = new Vector2(
//                    data[i] *Math.cos(Math.toRadians(i*10+5)),
//                    data[i] *Math.sin(Math.toRadians(i*10+5)));
//        }
//        return new BiLinear(v,x,x);
//    }

    String PrintInstructions() {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.UK);
        SimpleDateFormat format = new SimpleDateFormat("EEE yyyy.MM.dd HH:mm:ss zzz");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        Vector2 t = new Vector2();
        Vector2 w = new Vector2();
        Vector2 positionDegrees = new Vector2();

        System.out.println();
        Fix firstFix = bestAgent.stub;
        Fix lastFix = bestAgent.track.getLast();
        Fix previousFix = firstFix;
        boolean penaltyTimeLatch = false;

        for (Fix f: bestAgent.track) {
            if(f.time == 0)
                continue;
            if (    ( f != firstFix) &&
                    ( f != lastFix) &&
                    (Math.round(f.heading.toBearing().y) == Math.round(previousFix.heading.toBearing().y)) &&
                    (( f.stamina.penaltyEndTime - f.time <= 0) || penaltyTimeLatch ) ) {
                if ( f.stamina.penaltyEndTime - f.time <= 0)
                    penaltyTimeLatch = false;
                previousFix = f;
                continue;
            }
            penaltyTimeLatch = true;
            previousFix = f;
            positionDegrees.x = f.position.x * phys.degrees;
            positionDegrees.y = f.position.y * phys.degrees;
            if(Main.useWater)
                water.getValue(positionDegrees,f.time,t);
            wind.getValue(positionDegrees,f.time,w);
            Vector2 tidalWind = w.minus(t);
            float trueWIndSpeed = tidalWind.mag();
            double CTS = f.heading.toBearing().y; // TODO need to add in leeway
            Vector2 heading = new Vector2(CTS);
            double TWA;

            if (trueWIndSpeed == 0) {
                TWA = 0;
            }
            else {
                float cosAngle = -tidalWind.dot(heading) / trueWIndSpeed;
                TWA = Math.acos(cosAngle);
            }
            formatter.format("%s %s %s ** %03.0fT **  %4.1fkts TWA %3.0f TWS %4.1f %s %s %s\n",format.format(f.time),f.DMSLatitude(), f.DMSLongitude(),
                    (CTS + 360) % 360, f.velocity.mag() * phys.knots, Math.toDegrees(TWA), trueWIndSpeed * phys.knots,
                    f.Tack(), f.Penalty(), boat.polar.polars.get(f.sail).name);

//            formatter.format("%s %s %s ** %03.0fT **  %4.1fkts TWA %3.0f TWS %4.1f %s %s %s\n",format.format(f.time),f.DMSLatitude(), f.DMSLongitude(),
//                    (CTS + 360) % 360, waterTrack.mag() * phys.knots, Math.toDegrees(TWA), trueWIndSpeed * phys.knots,
//                    f.Tack(), format.format(f.stamina.penaltyEndTime), boat.polar.polars.get(f.sail).name);
        }
//        sb.append("\ntime: " + (boat.DE.getTime()-boat.DE.getTime(0)) / phys.msPerHour);

        System.out.print(sb);
        return sb.toString();
    }

    String PrintShortInstructions() {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.UK);
        SimpleDateFormat format = new SimpleDateFormat("EEE HHmm'Z'dd");
        Vector2 t = new Vector2();
        Vector2 w = new Vector2();
        Vector2 positionDegrees = new Vector2();
        long[] times = new long[8];
        times[0] = getInitialFix().time;
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(getInitialFix().time );
        int startHour = calendar.get(Calendar.HOUR_OF_DAY);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        if (startHour < 12) {
            calendar.set(Calendar.HOUR_OF_DAY,12);
        }
        else {
            calendar.set(Calendar.HOUR_OF_DAY,0);
            calendar.add(Calendar.DAY_OF_YEAR,1);
        }
        for (int i = 1; i < times.length; i++) {
            times[i] = calendar.getTimeInMillis();
            calendar.add(Calendar.HOUR,12);
        }

        for (long time : times) {
            if (time > getLastFix().time)
                break;
            Fix f = findNearestFix(time);
            positionDegrees.x = f.position.x * phys.degrees;
            positionDegrees.y = f.position.y * phys.degrees;
            if(Main.useWater)
                water.getValue(positionDegrees,f.time,t);
            wind.getValue(positionDegrees,f.time,w);
            Vector2 tidalWind = w.minus(t);
            float trueWIndSpeed = tidalWind.mag();
            double CTS = f.heading.toBearing().y; // TODO need to add in leeway

            formatter.format("%s %s %s %03.0fT %3.0fTWS %s\n",format.format(time),f.DMLatitude(), f.DMLongitude(),
                    (CTS + 360) % 360, trueWIndSpeed * phys.knots, f.Tack());
        }
        sb.append("\nETA: ").append(format.format(getLastFix().time));
//        sb.append("\ntime: ").append((boat.DE.getTime()-boat.DE.getTime(0)) / phys.msPerHour);

        System.out.print(sb);
        return sb.toString();
    }

    String GPX(String title) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.UK);
        Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Vector2 t;
        Vector2 w;
        Vector2 waterTrack = new Vector2();
        int sailConfiguration;
        sb.append("\n\n<gpx>\n" +
                "<metadata><author>Course2Steer</author></metadata>\n" +
                "<trk>\n" +
                "<name>").append(title).append("</name>\n").append("<trkseg>\n");
        for (Fix f:bestAgent.track) {
            t = f.tide;
            w = f.wind;
            Vector2 tidalWind = w.minus(t);
            float trueWIndSpeed = tidalWind.mag();
            double CTS = f.heading.toBearing().y; // TODO need to add in leeway
            Vector2 heading = f.heading;
            if (trueWIndSpeed == 0) {
                continue;
            }
            float cosAngle = -tidalWind.dot(heading) / trueWIndSpeed;
            double TWA = Math.acos(cosAngle);
            double latd = Math.toDegrees(f.position.y);
            double lond = Math.toDegrees(f.position.x);

            try {
                boat.polarToUse.interpolate(trueWIndSpeed, cosAngle, waterTrack);
                sailConfiguration = boat.polar.sail.get(trueWIndSpeed, cosAngle);
            } catch (Exception e) {
                continue;
            }
            String tack = "Stbd";
            if(f.portTack)
                tack = "Port";
            UTC.setTimeInMillis(f.time);
            formatter.format("<trkpt lat=\"%.3f\" lon=\"%.3f\">\n" +
                            "<time>%s</time>\n" +
                            "<desc>%03.0fT %4.1fkts TWA %3.0f TWS %4.1f %s %s</desc>\n" +
                            "</trkpt>\n",
                    latd,  lond, format.format(UTC.getTime()),
                    (CTS + 360) % 360, waterTrack.mag() * phys.knots, Math.toDegrees(TWA), trueWIndSpeed * phys.knots,
                    tack, boat.polar.polars.get(sailConfiguration).name);
        }
        sb.append("</trkseg>\n" +
                "</trk>\n" +
                "</gpx>\n");
        return sb.toString();
    }



//    void reset(Fix depart) {
//        this.depart = new Fix (depart);
//        generateAgents();
//    }

    Fix getInitialFix() {
            return new Fix(depart);
    }

    Fix getLastFix() {
            return bestAgent.track.getLast();
    }

    void setBmA() {
        BmA = bestAgent.waypoint[1].minus(bestAgent.waypoint[0]);
        double cosTestLat = Math.cos(bestAgent.waypoint[1].y); // TODO why does this have to be computed?
        BmA.x *= (float) cosTestLat;
    }


//    boolean passedFirstWaypoint(Fix fix) {
//
//        Vector2 BmF = bestAgent.waypoint[1].position.minus(fix.position);
//        double cosTestLat = Math.cos(bestAgent.waypoint[1].position.y); // TODO Why does this have to be computed
//        BmF.x *= cosTestLat;
//        return BmF.dot(BmA) < 0;
//    }

//    Vector2 getHeading() {
//            return new Vector2(bestAgent.track.get(1).heading);
//    }
//    Vector2 getHeading(int i) {
//            return new Vector2(bestAgent.waypoint[i].heading);
//    }

//    Vector2 getNextHeading() {
//            return new Vector2(bestAgent.track.get(1).heading);
//    }

    long getTime() {
        return bestAgent.track.getLast().time;}
    long getElapsedTime() { return (getTime() - ((Depart)depart).time);}

//    long getTime(int i) { return bestAgent.waypoint[i].time;}
//    BiLinear getPolar() {return polar;};

    Fix  findNearestFix(long time) {
        Fix f = null;
        for(Fix tp:bestAgent.track) { // find nearest track point to cursor
            f = tp;
            if(f.time >= time)
                break;
        }
        return f;
    }
    Fix findNearestFix(Vector2 p) {
        Fix f = null;
        double minRange = Double.MAX_VALUE;
        for(Fix tp:bestAgent.track) { // find nearest track point to cursor
            double r = Fix.range(tp.position,p);
            if(r < minRange) {
                minRange = r;
                f = tp;
            }
        }
        return f;
    }

    static int findNearestFix(LinkedList<Fix> track, Vector2 position) {
        double minRange = Double.MAX_VALUE;
        int closest = 0;
        for (int j = 0; j < track.size(); j++) {
            if(Float.isNaN(track.get(j).position.x) )
                continue;
            double r = Fix.range(track.get(j).position, position);
            if(r < minRange) {
                minRange = r;
                closest = j;
            }
        }
        return closest;
    }

    static int findNearestFix(Fix[] track, Vector2 position) {
        double minRange = Double.MAX_VALUE;
        int closest = 0;
        for (int j = 0; j < track.length; j++) {
            double r = Fix.range(track[j].position, position);
            if(r < minRange) {
                minRange = r;
                closest = j;
            }
        }
        return closest;
    }


    void setPolar (BiLinear polar) {
        this.polar = polar;
    }
    void setResolution (long resolution) {
        this.resolution = resolution;
    }
//    void setDestination (Fix destination) {
//        this.destination = new Fix(destination);
//    }

//    void CheckBest() {
//        boat.polarToUse = polar;
//        bestAgent = new Agent();
//        for (int i = 0; i < population.length; i++) {
//            if(population[i].CheckFirstWaypoint()) {
//                System.out.println(i);
//                continue;
//            }
//            if(population[i].error < bestAgent.error) {
//                bestAgent = population[i];
//            }
//        }
//    }
    int numberOfTrackPoints() {
        return bestAgent.track.size();
    }
    public int getLegs() { return legs;}

    public void recomputeErrors() {
        boat.polarToUse = polar;
        bestAgent = new Agent();
        for (Agent a: population) {
            a.ComputeError();
            if(a.error < bestAgent.error) {
                bestAgent = a;
            }
        }
    }

    public void resetErrors() {
        bestAgent = new Agent();
        for (Agent a: population)
            a.error = Long.MAX_VALUE;
    }

    public boolean HasAnyFailedSolutions() {
        for (Agent a: population) {
            if (a.track.isEmpty())
                return true;
        }
        return false;
    }
    Vector2 departPosition() {
        return depart.position;
    }
}
