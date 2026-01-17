package uk.co.sexeys.JIM;

// https://stackoverflow.com/questions/3997410/how-to-calculate-cross-track-error-gps-core-location
// https://www.tandfonline.com/doi/full/10.1080/17445302.2024.2329011#d1e295

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.sexeys.*;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.waypoint.*;
import uk.co.sexeys.wind.Wind;

import java.awt.*;
import java.util.List;
import java.util.*;

public class CrossTrack extends JIM{
    private static final Logger logger = LoggerFactory.getLogger(CrossTrack.class);

    public static class CrossTrackAgent extends Agent{
        CrossTrackAgent(Agent agent) {
            super(agent);
        }

    }

    public CrossTrack(Boat boat, Wind wind, Water tide) {
        this.boat = boat;
        this.wind = wind;
        this.tide = tide;
        newAgents = new LinkedList<>();
    }

    public void SearchInit () throws Exception{
        boat.polarToUse = boat.polar.raw;
        boat.currentWaypoint = 0;
        while ( boat.waypoints[boat.currentWaypoint ] instanceof Depart ||
                boat.waypoints[boat.currentWaypoint ] instanceof InterimFix)
            boat.currentWaypoint ++;
        if (boat.currentWaypoint < 1) {
            logger.error("***********************************");
            logger.error("Really was expecting first waypoint to be a Fix or Depart");
            logger.error("and the second waypoint to be Expand.");
            logger.error("***********************************");
            System.exit(-1);
        }
        if (! (boat.waypoints[boat.currentWaypoint ] instanceof Expand)) {
            logger.warn("***********************************");
            logger.warn("Starting an ordinary waypoint is STRONGLY discouraged.");
            logger.warn("Start instead with an expanding waypoint.");
            logger.warn("***********************************");
        }
        if(boat.waypoints[boat.currentWaypoint].obstructions.data == null)
            Obstruction.active = null;
        else
            Obstruction.active = boat.waypoints[boat.currentWaypoint].obstructions;
        Agent initialAgent = new CrossTrackAgent(null);
        initialAgent.position = new Vector2(boat.waypoints[boat.currentWaypoint].position);
        initialAgent.setSinCos();
        initialAgent.time = boat.waypoints[boat.currentWaypoint-1].getTime();
        initialAgent.stamina = new Stamina(1.0f);
        initialAgent.currentLeg = boat.currentWaypoint;
        initialAgent.nextTimestep = initialAgent.time + boat.waypoints[boat.currentWaypoint ].timeStep;

        route = new Route(boat.waypoints);
        route.timestep = route.legs[initialAgent.currentLeg].waypoint.timeStep;
        route.currentLeg = boat.currentWaypoint;
        route.currentTime = initialAgent.time;
        route.legElapsedTime = 0;

        if(boat.waypoints[initialAgent.currentLeg-1] instanceof InterimFix) {
            initialAgent.heading = ((InterimFix) boat.waypoints[boat.currentWaypoint-1]).heading;
            newAgents.clear();
            newAgents.add(initialAgent);
            List<Agent> nextAgents = NextTimeStep(newAgents, 0, 1);
            initialAgent = nextAgents.getFirst();
            initialAgent.stamina = new Stamina(((InterimFix) boat.waypoints[initialAgent.currentLeg-1]).stamina);
            Fix v = initialAgent.ToFix();
            v.wind = initialAgent.previousAgent.wind;
            v.tide = initialAgent.previousAgent.tide;
            initialAgent.previousAgent = null;
            try {
                boat.findSpeed(v);
                StringBuilder sb = new StringBuilder();
                Formatter formatter = new Formatter(sb, Locale.UK);
                Vector2 computedWind = v.wind.toBearing();
                float TWA = (computedWind.y+180) - ((InterimFix) boat.waypoints[initialAgent.currentLeg-1]).heading;
                if (TWA < -180)
                    TWA += 360;
                if (TWA > 180)
                    TWA -= 360;
                formatter.format("\n        VR     ME\r\nBoat  %4.1f   %4.1f\r\nWind  %4.1f   %4.1f\r\nTWA %6.0f %6.0f\r\nSail%6d %6d",
                        ((InterimFix) boat.waypoints[initialAgent.currentLeg-1]).speed, v.velocity.mag() * phys.knots,
                        ((InterimFix) boat.waypoints[initialAgent.currentLeg-1]).TWS, computedWind.x * phys.knots,
                        ((InterimFix) boat.waypoints[initialAgent.currentLeg-1]).TWA, TWA,
                        ((InterimFix) boat.waypoints[initialAgent.currentLeg-1]).sail, v.sail);
                logger.info("{}", sb);
            } catch (Exception e) {
                logger.error("Error computing speed", e);
            }
        }
        newAgents.clear();
        newAgents.add(initialAgent);
        logger.debug("Search initialized");
    }

    public void Search(long cutoffTime) throws Exception{
        boolean keepGoing = true;
        int angularRange = 360;
        long timeStep;

        while(keepGoing) {
            if(route.currentTime >= cutoffTime) {
                keepGoing = false;
                float cheapest = Float.MAX_VALUE; // cheapest is closest to lat waypoint
                keyAgent = null;
                for(Agent a:newAgents) {
                    a.cost = Fix.range(route.legs[route.currentLeg].waypoint.position, a.position);
                    if (a.cost < cheapest) {
                        cheapest = a.cost;
                        keyAgent = a;
                    }
                }
                logger.warn("For some reason the route did not finish before the maximum time allowed (Main.JIMCutoff). Check the map. Check Main.JIMCutoff.");
                continue;
            }


            if(route.legElapsedTime == 0){
                angularRange = 360;
            }
            else {
                angularRange = 270;
            }

            timeStep = route.timestep;
            if (route.legs[route.currentLeg].waypoint instanceof Expand)
                timeStep += route.legElapsedTime / Main.ExpandingTimeFactor; //  (from CUDA)

            route.currentTime += timeStep;
            route.legElapsedTime += timeStep;

            List<Agent> nextAgents = NextTimeStep(newAgents, route.currentTime, angularRange);
            CullAgents(nextAgents, newAgents);
            if (newAgents.isEmpty()) {
                logger.warn("For some reason all routes have disappeared. Stopped routing. Check the map for errors. Consider adding/moving gate waypoints.");
                keepGoing = false;
                continue;
            }
            List<Agent> completedAgents = new LinkedList<>();
            for (Agent a:newAgents) {
                if (route.legs[a.currentLeg].waypoint.Reached(
                        a.previousAgent.position,
                        a.position)) {
                    a.currentLeg++;
                    completedAgents.add(a);
                }
            }
            if(!completedAgents.isEmpty()) {
                float cheapest = Float.MAX_VALUE;
                keyAgent = null;
                for(Agent a:completedAgents) {
                    if(a.currentLeg > route.legs.length -1) {
                        keepGoing = false;
                        Vector2 intersection = route.legs[route.legs.length - 1].waypoint.Intersection(
                                a.previousAgent.position, a.position);
                        a.cost = Fix.range(intersection, a.previousAgent.position) / (Fix.range(a.position, a.previousAgent.position) + 1e-20f);
                        if (a.cost < cheapest) {
                            cheapest = a.cost;
                            keyAgent = a;
                        }
                        CrossTrackAgent temp = new CrossTrackAgent(null);
                        temp.position = route.legs[route.legs.length - 1].waypoint.Intersection(
                                keyAgent.previousAgent.position, keyAgent.position);
                        temp.setSinCos();
                        temp.previousAgent = keyAgent.previousAgent;
                        float d1 = Fix.range(keyAgent.position, keyAgent.previousAgent.position);
                        float d2 = Fix.range(temp.position, temp.previousAgent.position);
                        float dt = keyAgent.time - keyAgent.previousAgent.time;
                        temp.time = keyAgent.previousAgent.time + (long) (d2 / d1 * dt);
                        keyAgent = temp;
                        continue;
                    }
                    if (a.currentLeg > route.currentLeg) {
                        route.currentLeg = a.currentLeg;
                        route.timestep = route.legs[route.currentLeg].waypoint.timeStep;
                        route.legElapsedTime = 0;
                    }
                    a.nextTimestep = a.time + route.legs[a.currentLeg].waypoint.timeStep;
                }
//                if(boat.waypoints[route.currentLeg].obstructions.data == null)
//                    Obstruction.active = null;
//                else {
//                    Obstruction.active = boat.waypoints[route.currentLeg].obstructions;
//                    List<Agent> checkedAgents = new LinkedList<>();
//                    for (Agent a: nextAgents) {
//                        boolean OK = true;
//                        Agent b = a;
//                        while (b.previousAgent != null) {
//                            if (Obstruction.Intersection(b.position,b.previousAgent.position) ) {
//                                OK = false;
//                                break;
//                            }
//                            b= b.previousAgent;
//                        }
//                        if (OK) checkedAgents.add(a);
//                    }
//                }
            }
//            System.out.print("\r"+(elapsedTime) / phys.msPerHour+ " "+newAgents.size());
            logger.debug("Time: {} Agents: {}", route.currentTime, newAgents.size());
        }
    }

    private final static Vector2 positionDegrees = new Vector2();
    Vector2 waterTrack = new Vector2();
    Vector2 wNp = new Vector2();
    Vector2 wN = new Vector2();

    List<Agent> NextTimeStep(List<Agent> agents, long currentTime, float  angularRange) throws Exception{
        List<Agent> newAgents = new LinkedList<>();
        Vector2 tidalWind = new Vector2();
        Vector2 vHeading, result = new Vector2();

        for (Agent a: agents) {
            if (a.nextTimestep > currentTime) {
                newAgents.add(a);
                continue;
            }
            positionDegrees.x = a.position.x * phys.degrees;
            positionDegrees.y = a.position.y * phys.degrees;
            Wind.SOURCE windSource = wind.getValue(positionDegrees, a.time, a.wind);
            Water.SOURCE waterSource = null;
            if( Main.useWater)
                waterSource = tide.getValue(positionDegrees, a.time, a.tide);
            a.wind.minus(a.tide, tidalWind);
            float trueWIndSpeed = tidalWind.normalise();  //TODO SQRT
            if(trueWIndSpeed == 0) {
                newAgents.add(a);
                continue;
            }
            for (int i = 0; i < angularRange; i++) { // TODO implicit 1 degree resolution
                Agent agent = new CrossTrackAgent(a);
                agent.windSource = windSource;
                agent.waterSource = waterSource;
                agent.heading = a.heading + i - angularRange/2;
                agent.time = currentTime;
                agent.nextTimestep = currentTime + route.legs[a.currentLeg].waypoint.timeStep;

                vHeading = new Vector2(agent.heading);
                float cosAngle = -tidalWind.dot(vHeading);

                if(cosAngle > 1)
                    cosAngle = 1;
                if(cosAngle < -1)
                    cosAngle  = -1;

                agent.closeHauled = cosAngle < 0;

                if(trueWIndSpeed >100)
                    logger.warn("Wind too high: {}", trueWIndSpeed);


                boat.polarToUse.interpolate(trueWIndSpeed, cosAngle , waterTrack);
                agent.sail = boat.polar.sail.get(trueWIndSpeed, cosAngle);
                wNp.y = wN.x = -tidalWind.x ;
                wN.y = -tidalWind.y;
                wNp.x = -wN.y;

                if(vHeading.dot(wNp) < 0) {
                    wNp.negate();
                    agent.portTack = true;
                }
                else
                    agent.portTack = false;
                long timeStep = currentTime - a.time;
                agent.time = currentTime;

                if (agent.portTack != a.portTack) {
                    if (a.closeHauled) {
                        agent.stamina.tackPenalty(a.wind, a.time);
                    } else {
                        agent.stamina.gybePenalty(a.wind, a.time);
                    }
                }
                if ((agent.sail != a.sail)) {
                    agent.stamina.sailPenalty(a.wind, a.time);
                }
                agent.stamina.recover(timeStep,trueWIndSpeed);

                float factor = agent.stamina.speedFactor(agent.time, a.time);
                if(Main.useIceZone) {
                    factor *= iceZone.SpeedFactor(a.position);
                }

                agent.logV.x = (wN.x * waterTrack.x + wNp.x * waterTrack.y) * factor;
                agent.logV.y = (wN.y * waterTrack.x + wNp.y * waterTrack.y) * factor;

                agent.VOG.x = a.tide.x + agent.logV.x;
                agent.VOG.y = a.tide.y + agent.logV.y;

                float dt = (float) timeStep / phys.msPerSecond;

                float offsetX = agent.VOG.x * dt;
                float offsetY = agent.VOG.y * dt;
                float deltaY = offsetY * phys.rReciprocal;

                agent.position.x = a.position.x + offsetX * phys.rReciprocal / a.cosLatitude;
                agent.position.y = a.position.y + deltaY;
                Obstruction.active = route.legs[a.currentLeg].waypoint.obstructions;

                if (!Main.shoreline.Intersection(a.position, agent.position)) {
                    agent.cosLatitude = a.cosLatitude - deltaY * a.sinLatitude; // fast cos
                    agent.sinLatitude = a.sinLatitude + deltaY * a.cosLatitude;
                    newAgents.add(agent);
                }
            }
        }
        return newAgents;
    }


    void CullAgents(List<Agent> agents, List<Agent> culled) {
        Vector2 result = new Vector2();
        culled.clear();
        for (Route.Leg l:route.legs) {
            for( List bin : l.greatCircleBins) {
                bin.clear();
            }
        }

        for (Agent a : agents) {
            if (a.position.y > Math.toRadians(88))
                continue;
            if (a.position.y < -Math.toRadians(88))
                continue;
            Waypoint waypoint = boat.waypoints[a.currentLeg];
            switch (waypoint) {
                case Diode diode -> {
                    diode.greatCircle.XTE(a.position, result);
                    a.bin = (int) (Math.round(result.x * phys.R / waypoint.binWidth)) + waypoint.numberOfBins / 2;
                    a.cost = (float) (result.y * phys.R);
                }
                case Gate gate -> {
                    gate.greatCircle.XTE(a.position, result);
                    a.bin = (int) (Math.round(result.x * phys.R / waypoint.binWidth)) + waypoint.numberOfBins / 2;
                    a.cost = (float) (result.y * phys.R);
                }
                case Leg leg -> {
                    leg.greatCircle.XTE(a.position, result);
                    a.bin = (int) (Math.round(result.x * phys.R / waypoint.binWidth)) + waypoint.numberOfBins / 2;
                    a.cost = (float) (result.y * phys.R);
                }
                case Destination destination -> {
                    GreatCircle.rangeAndBearing(destination.position, a.position, result);
                    a.bin = (int) (Math.round(result.x) * destination.numberOfBins / 360.0);
                    a.cost = (float) (result.y);
                }
                case Buoy buoy -> {
                    GreatCircle.rangeAndBearing(buoy.position, a.position, result);
                    a.bin = (int) (Math.round(result.y) / buoy.binWidth);
                    if (buoy.clockwise)
                        a.cost = -(result.x - buoy.costAngle + 360) % 360;
                    else
                        a.cost = -(buoy.costAngle - result.x + 360) % 360;
//                System.out.println((a.bin+" "+a.cost));
                }
                case Expand expand -> {
                    GreatCircle.rangeAndBearing(expand.position, a.position, result);
                    a.bin = (int) (Math.round(result.x) * expand.numberOfBins / 360.0);
                    a.cost = -(result.y);
                }
                case null, default -> {
                    logger.error("Waypoint not coded");
                    System.exit(-1);
                }
            }
            if (a.bin < 0)
                continue;
            if (a.bin >= route.legs[a.currentLeg].greatCircleBins.length)
                continue;
            route.legs[a.currentLeg].greatCircleBins[a.bin].add(a);
        }
        for (Route.Leg l : route.legs) {
            int numberOfBins = l.waypoint.numberOfBins;
            Agent[] bestAgent = new Agent[numberOfBins];

            for (int i = 0; i < numberOfBins; i++) {
                bestAgent[i] = null;
                for (Agent a : l.greatCircleBins[i]) {
                    if (a.cost <= l.progress[i]) {
                        l.progress[i] = a.cost;
                        bestAgent[i] = a;
                    }
                }
            }

            for (int i = 0; i < numberOfBins; i++) {
                if (bestAgent[i] != null)
                    culled.add(bestAgent[i]);
            }
        }
    }

    public void draw(Graphics2D g, Mercator screen, long time, boolean showCandidates) {
        Vector2 p,p0;
        Stroke solid = new BasicStroke(3);
        Stroke fine = new BasicStroke(1);
        g.setColor(Color.orange);
        for (Waypoint w: boat.waypoints) {
            if (w.greatCircle != null) {
                p0 = screen.fromRadiansToPoint(w.greatCircle.point(w.greatCircle.d01));
                if (w.greatCircle.d12 > 0) {
                    for (double d = w.greatCircle.d01; d < w.greatCircle.d01 +  w.greatCircle.d12  + 0.00001; d +=  w.greatCircle.d12 /10 ){
                        p = screen.fromRadiansToPoint(w.greatCircle.point(d));
                        g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
                        p0.x = p.x; p0.y = p.y;
                    }
                    if((w instanceof Leg) || (w instanceof Gate)) {
                        double XT = w.binWidth * w.numberOfBins / phys.R / 2;
                        p0 = screen.fromRadiansToPoint(w.greatCircle.crossTrack(w.greatCircle.d01, XT, Math.toRadians(90)));
                        for (double d = w.greatCircle.d01; d < w.greatCircle.d01 + w.greatCircle.d12 + 0.00001; d += w.greatCircle.d12 / 10) {
                            p = screen.fromRadiansToPoint(w.greatCircle.crossTrack(d, XT, Math.toRadians(90)));
                            g.drawLine((int) p0.x, (int) p0.y, (int) p.x, (int) p.y);
                            p0.x = p.x;
                            p0.y = p.y;
                        }
                        p0 = screen.fromRadiansToPoint(w.greatCircle.crossTrack(w.greatCircle.d01, XT, Math.toRadians(-90)));
                        for (double d = w.greatCircle.d01; d < w.greatCircle.d01 + w.greatCircle.d12 + 0.00001; d += w.greatCircle.d12 / 10) {
                            p = screen.fromRadiansToPoint(w.greatCircle.crossTrack(d, XT, Math.toRadians(-90)));
                            g.drawLine((int) p0.x, (int) p0.y, (int) p.x, (int) p.y);
                            p0.x = p.x;
                            p0.y = p.y;
                        }
                    }

                    else if (w instanceof Buoy) {
                        p0 = screen.fromRadiansToPoint(w.position);
                        int r = (int) screen.fromLengthToPixels(w.position,w.numberOfBins*w.binWidth);
                        g.drawOval((int) p0.x-r,(int) p0.y-r,r*2,r*2);
                    }
                }
            }
        }





//        g.setColor(Color.red);
//        p0 = screen.fromRadiansToPoint(gc.point(gc.d01));
//        p = screen.fromRadiansToPoint(gc.crossTrack(gc.d01,XT, Math.toRadians(-50)));
//        g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
//        p = screen.fromRadiansToPoint(gc.crossTrack(gc.d01,XT, Math.toRadians(50)));
//        g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
        g.setColor(Color.black);

        for(Agent X:newAgents) {
            if(X == keyAgent)
                g.setStroke(solid);
            else
                g.setStroke(fine);
            Agent Y = X;
//            p0 = screen.fromRadiansToPoint(Y.position);
//            Font font = new Font("Arial", Font.PLAIN, 14);
//            g.setFont(font);
//            g.drawString(""+Y.bin, (int) p0.x+5, (int) p0.y-5);
            while(Y.previousAgent != null) {
                p0 = screen.fromRadiansToPoint(Y.position);
                p = screen.fromRadiansToPoint(Y.previousAgent.position);
                g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
                Y = Y.previousAgent;
            }
        }
        g.setStroke(solid);
        Agent Y = keyAgent;
        while(Y.previousAgent != null) {
            p0 = screen.fromRadiansToPoint(Y.position);
            p = screen.fromRadiansToPoint(Y.previousAgent.position);
            g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
            Y = Y.previousAgent;
        }
        Y = keyAgent;
        while(true) {
            if(Y.previousAgent.time <= time)
                break;
            if(Y.previousAgent.previousAgent == null)
                break;
            Y = Y.previousAgent;
        }
        p0 = screen.fromRadiansToPoint(Y.position);
        p = screen.fromRadiansToPoint(Y.previousAgent.position);
        Vector2 dp = p0.minus(p);
        float t = (float) (time-Y.previousAgent.time)/(float)(Y.time - Y.previousAgent.time);
        p0 = p.plus(dp.scale(t));
        g.drawLine((int)p0.x, (int) p0.y-10, (int) p0.x, (int) p0.y+10);
        g.drawLine((int)p0.x-10, (int) p0.y, (int) p0.x+10, (int) p0.y);
        g.setStroke(fine);
    }

}

