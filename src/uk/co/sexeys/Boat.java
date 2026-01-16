package uk.co.sexeys;

import uk.co.sexeys.water.Tide;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.waypoint.Depart;
import uk.co.sexeys.waypoint.Waypoint;
import uk.co.sexeys.wind.Wind;

import java.awt.*;
import java.util.*;

/**
 * Created by Jim on 17/06/2018.
 *
 */

public class Boat {
    static LinkedList<Fix> constantHeading = new LinkedList<>();
    static private LinkedList<Fix> constantSail = new LinkedList<>();
    static private LinkedList<Fix> bestTack = new LinkedList<>();
    public BiLinear polarToUse;
    public Polar polar;
    public Waypoint[] waypoints;
    public int currentWaypoint= 0;
    DifferentialEvolution DE;

    Boat() {}

    Boat(Boat b) {
        waypoints = b.waypoints;
        polar = b.polar;
        polarToUse = b.polarToUse;
        DE = null;
    }

    private static final Vector2 tidalWind = new Vector2();
    private static final Vector2 waterTrack = new Vector2();
    private static final Vector2 wN = new Vector2();
    private static final Vector2 wNp = new Vector2();
    public static final IceZone iceZone = new IceZone();

    public float findSpeed(Fix fix) throws Exception {
        fix.wind.minus(fix.tide, tidalWind);
        float trueWIndSpeed = tidalWind.normalise();  //TODO SQRT
        if(trueWIndSpeed == 0) {
            fix.velocity.zero();
            return 0.0f;
        }
        float cosAngle = -tidalWind.dot(fix.heading);

        if(cosAngle > 1)
            cosAngle = 1;
        if(cosAngle < -1)
            cosAngle  = -1;

        if (cosAngle < 0 )
            fix.closeHauled = true;
        else
            fix.closeHauled = false;

        polarToUse.interpolate(trueWIndSpeed, cosAngle, waterTrack);
        fix.sail = polar.sail.get(trueWIndSpeed, cosAngle);
        wNp.y = wN.x = -tidalWind.x ;
        wN.y = -tidalWind.y;
        wNp.x = -wN.y;

        if(fix.heading.dot(wNp) < 0) {
            wNp.negate();
            fix.portTack = true;
        }
        else
            fix.portTack = false;

        fix.velocity.x = wN.x * waterTrack.x + wNp.x * waterTrack.y;
        fix.velocity.y = wN.y * waterTrack.x + wNp.y * waterTrack.y;
        return trueWIndSpeed;
    }
//
//    static long tackPenalty(Vector2 wind) {
//        float windSpeed = wind.mag() * phys.knots;
//        if(windSpeed < 10)
//            return Main.tackPenalty10;
//        if(windSpeed > 10)
//            return Main.tackPenalty30;
//        return Main.tackPenalty10 + (long) ((Main.tackPenalty30 - Main.tackPenalty10) *(windSpeed -10) /20.0);
//    }
//
//    static long gybePenalty(Vector2 wind) {
//        float windSpeed = wind.mag() * phys.knots;
//        if(windSpeed < 10)
//            return Main.gybePenalty10;
//        if(windSpeed > 10)
//            return Main.gybePenalty30;
//        return Main.gybePenalty10 + (long) ((Main.gybePenalty30 - Main.gybePenalty10) *(windSpeed -10) /20.0);
//    }
//
//    static long sailPenalty(Vector2 wind) {
//        float windSpeed = wind.mag() * phys.knots;
//        if(windSpeed < 10)
//            return Main.sailPenalty10;
//        if(windSpeed > 10)
//            return Main.sailPenalty30;
//        return Main.sailPenalty10 + (long) ((Main.sailPenalty30 - Main.sailPenalty10) *(windSpeed -10) /20.0);
//    }

    void courseToSteer(Wind wind, Water water, Shoreline wvs, Vector2 B, LinkedList<Fix> track,
                       long resolution, float tolerance, int canTry)
            throws Exception {
        float cosTestLat = (float) Math.cos(B.y); //TODO pass in a fix and this is already computed...
        float cosTestLatRec = 1/cosTestLat;
        Vector2 offset = new Vector2(),lastPosition,BmF = new Vector2();
        Vector2 positionDegrees = new Vector2();
        Fix workingFix;
        LinkedList<Fix> testTrack = new LinkedList<>();
        int attempts = 0;
        float tolerance2 = tolerance * tolerance, deltaRatio;
        Iterator<Fix> fixIterator = track.descendingIterator();
        Fix A = fixIterator.next();
        Fix previousFix, initialFix = null;
        if(fixIterator.hasNext()) {
            initialFix = fixIterator.next();
            A.stamina.currentStamina = initialFix.stamina.currentStamina; // because A gets changes by this routine.
        }
        Vector2 BmA  = B.minus(A.position);
        if((BmA.x == 0) && (BmA.y == 0) )
            return;
        Vector2 rhumbLine = new Vector2(BmA);
        BmA.x *= cosTestLat;
        do {
            attempts++;
            if(attempts > canTry) {
                Fix.recycle(testTrack);
                throw new Exception("courseToSteer failed to converge " + offset.mag());
            }
            Fix.recycle(testTrack);

            rhumbLine = rhumbLine.plus(offset);
            workingFix = A;
            previousFix = initialFix;
            workingFix.stamina.penaltyEndTime = 0;
            workingFix.heading.copy(rhumbLine);
            workingFix.heading.x *= A.cosLatitude;
            workingFix.heading.normalise();
            findSpeed(workingFix);

            if(previousFix != null) {
                workingFix.getPenalty(previousFix);
            }
            do {
                previousFix = workingFix;
                workingFix = workingFix.nextWorkingFix(this,wind,water,resolution);
                workingFix.getPenalty(previousFix);
                testTrack.add(workingFix);
                if(testTrack.size() > 1000) {
                    Fix.recycle(testTrack);
                    throw new Exception("courseToSteer test track too long " );
                }
                B.minus(workingFix.position, BmF);
                BmF.x *= cosTestLat;
            } while ( BmF.dot(BmA) > 0);
            Vector2 BmL = B.minus(previousFix.position);
            BmL.x *= cosTestLat;
            Vector2 FmL = workingFix.position.minus(previousFix.position);
            FmL.x *= cosTestLat;
            deltaRatio = BmA.dot(BmL) /  BmA.dot(FmL);
            Vector2 delta = FmL.scale(deltaRatio);
            workingFix.position.x = previousFix.position.x + delta.x * cosTestLatRec;
            workingFix.position.y = previousFix.position.y + delta.y;
            offset = B.minus(workingFix.position);
        } while(offset.mag2(cosTestLat) > tolerance2);

        long time = (long) ((deltaRatio-1)*resolution);
        workingFix.setSinCos();
        workingFix.time += time;
        positionDegrees.x = workingFix.position.x * phys.degrees;
        positionDegrees.y = workingFix.position.y * phys.degrees;
        workingFix.windSource = wind.getValue(positionDegrees, workingFix.time, workingFix.wind);
        if (Main.useWater)
            water.getValue(positionDegrees, workingFix.time, workingFix.tide);

        if (wvs != null)
            if(wvs.Intersection(A.position,testTrack) ) {
                Fix.recycle(testTrack);
                throw new Exception("Hit the coast");
            }
        if (Obstruction.Intersection(A.position,testTrack)) {
            Fix.recycle(testTrack);
            throw new Exception("Hit obstruction");
        }
        track.addAll(testTrack);
    }

    private long constantHeading(Wind wind, Tide tide, long resolution) {
        Fix workingFix = new Fix(waypoints[0]);
        try {
            workingFix = workingFix.nextWorkingFix(this, wind, tide,0);
        } catch (Exception e) {
            return 0;
        }
        long endTime = ((Depart)waypoints[currentWaypoint]).getTime() + phys.msPerDay;
        constantHeading.clear();
        while (workingFix.time < endTime) {
            constantHeading.add(workingFix);
            try {
                workingFix = workingFix.nextWorkingFix(this, wind, tide, resolution);
            } catch (Exception e) {
                break;
            }
        }
        return (endTime - ((Depart)waypoints[currentWaypoint]).getTime() );
    }

    void constantSail(Wind wind, Water tide, float bearing, long resolution) {
        Fix workingFix = new Fix(waypoints[currentWaypoint] );
        long resolutionSecond = resolution / phys.msPerSecond;
        double cosAngle;
        boolean tack;
        constantSail.clear();
        try {
            Vector2 p = workingFix.position.scale(phys.degrees);
            workingFix.windSource = wind.getValue(p, workingFix.time, workingFix.wind);
            if(Main.useWater)
                workingFix.waterSource = tide.getValue(p, workingFix.time, workingFix.tide);
            Vector2 tidalWind = workingFix.wind.minus(workingFix.tide);
            double trueWIndSpeed = tidalWind.mag();
            if(trueWIndSpeed == 0) {
                return;
            }
            else {
                cosAngle = -tidalWind.dot(new Vector2(bearing)) / trueWIndSpeed;
                if(cosAngle > 1)
                    cosAngle = 1;
                if(cosAngle < -1)
                    cosAngle  = -1;
                Vector2 waterTrack = polarToUse.interpolate(trueWIndSpeed,cosAngle);
                Vector2 wN = tidalWind.scale(-1 / trueWIndSpeed);
                Vector2 wNp = wN.Perpendicular();
                tack = workingFix.heading.dot(wNp) < 0;
                if (tack)
                    wNp.negate();
                workingFix.velocity.copy(wN.scale(waterTrack.x).plus(wNp.scale(waterTrack.y)));
//                workingFix.gibing = polar.gibeAngle.interpolate(trueWIndSpeed) > cosAngle;
//                workingFix.tacking = polar.tackAngle.interpolate(trueWIndSpeed) < cosAngle;
            }
        } catch (Exception e) {
            return;
        }
        long endTime = ((Depart)waypoints[currentWaypoint]).getTime() + phys.msPerDay;
        LinkedList<Fix> testTrack = new LinkedList<>();
        while (workingFix.time < endTime) {
            testTrack.add(workingFix);
            try {
                workingFix = new Fix(workingFix);
                workingFix.time += resolution;
                Vector2 temp = new Vector2(workingFix.tide).
                        scale(resolutionSecond).
                        scaleAdd(resolutionSecond,workingFix.velocity);
                workingFix.AddPosition(temp);
                Vector2 p = workingFix.position.scale(phys.degrees);
                workingFix.windSource = wind.getValue(p, workingFix.time, workingFix.wind);
                if(Main.useWater)
                    workingFix.waterSource = tide.getValue(p, workingFix.time, workingFix.tide);
                if(workingFix.tide.x > 100)
                    throw new Exception("Hit land");
                Vector2 tidalWind = workingFix.wind.minus(workingFix.tide);
                double trueWIndSpeed = tidalWind.mag();
                if(trueWIndSpeed == 0) {
                    workingFix.velocity.zero();
                }
                else {
                    Vector2 waterTrack = polar.raw.interpolate(trueWIndSpeed, cosAngle);
                    Vector2 wN = tidalWind.scale(-1 / trueWIndSpeed);
                    Vector2 wNp = wN.Perpendicular();
                    if (tack)
                        wNp.negate();
                    workingFix.velocity.copy(wN.scale(waterTrack.x).plus(wNp.scale(waterTrack.y)));
//                    workingFix.gibing = polar.gibeAngle.interpolate(trueWIndSpeed) > cosAngle;
//                    workingFix.tacking = polar.tackAngle.interpolate(trueWIndSpeed) < cosAngle;
                    workingFix.heading = new Vector2(workingFix.velocity);
                    workingFix.heading.normalise();
                }
            } catch (Exception e) {
                break;
            }
        }
        this.constantSail = testTrack;
    }

    void bestTack(Wind wind, Water tide, Vector2 destination,  float tolerance) {
        LinkedList<Fix> gibeStbd = tack(wind, tide, destination, phys.msPerHour, tolerance, false,true);
        LinkedList<Fix> gibePort = tack(wind, tide, destination, phys.msPerHour, tolerance, true,true);
        LinkedList<Fix> tackStbd = tack(wind, tide, destination, phys.msPerHour, tolerance, false,false);
        LinkedList<Fix> tackPort = tack(wind, tide, destination, phys.msPerHour, tolerance, true,false);
        if (showBestTack) { // will be false is error occurred above
            bestTack = gibeStbd;
            if (gibePort.get(gibePort.size() - 1).time < bestTack.get(bestTack.size() - 1).time)
                bestTack = gibePort;
            if (tackStbd.get(tackStbd.size() - 1).time < bestTack.get(bestTack.size() - 1).time)
                bestTack = tackStbd;
            if (tackPort.get(tackPort.size() - 1).time < bestTack.get(bestTack.size() - 1).time)
                bestTack = tackPort;
        }
    }

    private LinkedList<Fix> tack(Wind wind, Water tide, Vector2 destination, long resolution, float tolerance, boolean startOnPortTack, boolean gibing) {
        polarToUse = polar.raw;
        double cosAngle;
        float resolutionSecond = (float) resolution/phys.msPerSecond;
        Coefficient angleToUse;
        LinkedList<Fix> result = new LinkedList<>();
        try {
            angleToUse = polar.tackAngle;
            if(gibing)
                angleToUse = polar.gibeAngle;

            Fix workingFix = new Fix(waypoints[currentWaypoint]);
            workingFix = workingFix.nextWorkingFix(this, wind, tide, 0);
            long bestTime = Long.MAX_VALUE;
            LinkedList<Fix> firstTack = new LinkedList<>();
            LinkedList<Fix> bestTrack = new LinkedList<>();
            LinkedList<Fix> testTrack = new LinkedList<>();
            Vector2 p;
            firstTack.add(workingFix);
            while (true) {
                Vector2 tidalWind = workingFix.wind.minus(workingFix.tide);
                double trueWIndSpeed = tidalWind.mag();
                if (trueWIndSpeed == 0) {
                    throw new Exception("Zero wind at start");
                }
                cosAngle = angleToUse.interpolate(trueWIndSpeed);
                Vector2 waterTrack = polar.raw.interpolate(trueWIndSpeed, cosAngle);
                Vector2 wN = tidalWind.scale(-1 / trueWIndSpeed);
                Vector2 wNp  = wN.Perpendicular();
                if (startOnPortTack)
                    wNp.negate();
                workingFix.velocity.copy(wN.scale(waterTrack.x).plus(wNp.scale(waterTrack.y)));
                workingFix = new Fix(workingFix);
                workingFix.time += resolution;
                Vector2 temp = new Vector2(workingFix.tide).
                        scale(resolutionSecond).
                        scaleAdd(resolutionSecond, workingFix.velocity);
                workingFix.AddPosition(temp);
                p = workingFix.position.scale(phys.degrees);
                workingFix.windSource = wind.getValue(p, workingFix.time, workingFix.wind);
                if(Main.useWater)
                    workingFix.waterSource = tide.getValue(p, workingFix.time, workingFix.tide);
                bestTrack = testTrack;
                testTrack = new LinkedList<>();
                testTrack.add(workingFix);
                try {
                    courseToSteer(wind, tide,  null, destination, testTrack, resolution,  tolerance* phys.rReciprocal, 100);
                } catch (Exception e){
                    firstTack.add(workingFix);
                    continue;
                }
                for (int i = 1; i < testTrack.size(); i++) {
                    if (testTrack.get(i).portTack != testTrack.get(1).portTack) {
                        firstTack.add(workingFix);
                    }
                }
                if(testTrack.get(1).portTack == startOnPortTack) {
                    firstTack.add(workingFix);
                    continue;
                }
                if (testTrack.get(testTrack.size()-1).time > bestTime)
                    break;
                bestTime = testTrack.get(testTrack.size()-1).time;
                firstTack.add(workingFix);
            }
            result.addAll(firstTack);
            result.addAll(bestTrack);
        }   catch (Exception e) {
            result.get(result.size()-1).time = Long.MAX_VALUE;
        }
        return result;
    }


    static boolean showBestTack = false;
    static boolean showConstantCourse = false;
    static boolean showConstantTWA = false;
    static boolean showCandidates = true;

    void draw(Graphics2D g, Mercator screen, long t) {
        long referenceTime = ((Depart)waypoints[0]).getTime();

        if (showConstantCourse) {
            g.setColor(Color.blue);
            for(Fix f: constantHeading) {
                if (f.time > t)
                    break;
                f.draw(g,screen,referenceTime);
            }
        }
        if (showConstantTWA) {
            g.setColor(Color.darkGray);
            for(Fix f: constantSail) {
                if (f.time > t)
                    break;
                f.draw(g,screen,referenceTime);
            }
        }
        if (showBestTack) {
            g.setColor(Color.red);
            for(Fix f: bestTack) {
                if (f.time > t)
                    break;
                f.draw(g,screen,referenceTime);
            }
        }
        g.setColor(Color.black);
    }

    class Sail {
        int id;
        String name;
        double[][] speed;
    }
    void ReadVRPolar(String p) {
        double foil_speedRatio, foil_twaMin, foil_twaMax, foil_twaMerge, foil_twsMin, foil_twsMax, foil_twsMerge;
        double hull_speedRatio;

        String[] sd = p.split("\\{|\\}");
        int i = 0;
        while(!sd[i].contains("polar")) i++;
        while(!sd[i].contains("foil")) i++;
        String[] values = sd[++i].split(",");
        for (String v:values) {
            String[] subValues = v.split("\\:");
            if(subValues[0].contains("speedRatio"))
                foil_speedRatio = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("twaMin"))
                foil_twaMin = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("twaMax"))
                foil_twaMax = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("twaMerge"))
                foil_twaMerge = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("twsMin"))
                foil_twsMin = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("twsMax"))
                foil_twsMax = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("twsMerge"))
                foil_twsMerge = Double.valueOf(subValues[1]);
        }
        while(!sd[i].contains("hull")) i++;
        values = sd[++i].split(",");
        for (String v:values) {
            String[] subValues = v.split("\\:");
            if(subValues[0].contains("speedRatio"))
                hull_speedRatio = Double.valueOf(subValues[1]);
        }
        while(!sd[i].contains("winch")) i++;
        while(!sd[i].contains("tack")) i++;
        values = sd[++i].split(",");
        double winch_tack_timer, winch_tack_ratio, winch_tack_proTimer, winch_tack_proRatio;
        for (String v:values) {
            String[] subValues = v.split("\\:");
            if(subValues[0].contains("stdTimerSec"))
                winch_tack_timer = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("stdRatio"))
                winch_tack_ratio = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("proTimerSec"))
                winch_tack_proTimer = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("proRatio"))
                winch_tack_proRatio = Double.valueOf(subValues[1]);
        }
        while(!sd[i].contains("gybe")) i++;
        values = sd[++i].split(",");
        double winch_gybe_timer, winch_gybe_ratio, winch_gybe_proTimer, winch_gybe_proRatio;
        for (String v:values) {
            String[] subValues = v.split("\\:");
            if(subValues[0].contains("stdTimerSec"))
                winch_gybe_timer = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("stdRatio"))
                winch_gybe_ratio = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("proTimerSec"))
                winch_gybe_proTimer = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("proRatio"))
                winch_gybe_proRatio = Double.valueOf(subValues[1]);
        }
        while(!sd[i].contains("sailChange")) i++;
        values = sd[++i].split(",");
        double winch_sailChange_timer, winch_sailChange_ratio, winch_sailChange_proTimer, winch_sailChange_proRatio;
        for (String v:values) {
            String[] subValues = v.split("\\:");
            if(subValues[0].contains("stdTimerSec"))
                winch_sailChange_timer = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("stdRatio"))
                winch_sailChange_ratio = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("proTimerSec"))
                winch_sailChange_proTimer = Double.valueOf(subValues[1]);
            else if (subValues[0].contains("proRatio"))
                winch_sailChange_proRatio = Double.valueOf(subValues[1]);
        }
        while(!sd[i].contains("tws")) i++;
        String[] array = sd[i].split("\\\"");
        int k = 0;
        while(!array[k].contains("tws")) k++;
        values = array[++k].split("\\[|,|\\]");
        double[] tws = new double[values.length-1];
        for (int j = 0; j < tws.length; j++) {
            tws[j] = Double.valueOf(values[j+1]);
        }
        k = 0;
        while(!array[k].contains("twa")) k++;
        values = array[++k].split("\\[|,|\\]");
        double[] twa = new double[values.length-1];
        for (int j = 0; j < twa.length; j++) {
            twa[j] = Double.valueOf(values[j+1]);
        }
        ArrayList<Sail> sails = new ArrayList<Sail>();
        while(!sd[i].contains("sail")) i++;
        do {
            i++;
            Sail s = new Sail();
            array = sd[i].split("\\\"");
            k = 0;
            while(!array[k].contains("id")) k++;
            k++;
            s.id = Integer.valueOf(array[k].split(":|,")[1]);
            while(!array[k].contains("name")) k++;
            k+=2;
            s.name = array[k];
            while(!array[k].contains("speed")) k++;
            values = array[++k].split("\\[|,|\\]");
            s.speed = new double[twa.length][tws.length];
            int j = 2;
            for (int m = 0;m< twa.length;m++) {
                for (int n = 0; n < tws.length; n++) {
                    s.speed[m][n] = Double.valueOf(values[j++]);
                }
                j+=2;
            }
            i++;
            sails.add(s);
        } while (!sd[i].contentEquals("]"));

    }
}
