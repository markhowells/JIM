package uk.co.sexeys.JIM;

// Jim's Isochrone Method

import uk.co.sexeys.*;
import uk.co.sexeys.rendering.Colors;
import uk.co.sexeys.rendering.Projection;
import uk.co.sexeys.rendering.Renderable;
import uk.co.sexeys.rendering.Renderer;
import uk.co.sexeys.water.Tide;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.waypoint.Depart;
import uk.co.sexeys.wind.Wind;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public abstract class JIM implements Renderable {
    public Boat boat;
    public Wind wind;
    Water tide;
    public static final IceZone iceZone = new IceZone();

    public Agent keyAgent;
    List<Agent> newAgents;
    public Route route;

    public JIM(){}

    public void Search(long cutoffTime) throws Exception{}
    public void SearchInit () throws Exception{}
    public void draw(Graphics2D g, Mercator screen, long time, boolean showCandidates) {
        Vector2 p,p0;
        Stroke solid = new BasicStroke(3);
        Stroke fine = new BasicStroke(1);
        g.setColor(Color.black);

        GreatCircle gc = new GreatCircle(boat.waypoints[0].position,boat.waypoints[1].position);
//        GreatCircle gc = new GreatCircle(  // test code
//                new Vector2((float) Math.toRadians(-71.6),(float) Math.toRadians(-33) ),
//                new Vector2((float) Math.toRadians(121.8),(float) Math.toRadians(31.4) )) ;
        p0 = screen.fromRadiansToPoint(gc.point(gc.d01));
        for (double d = gc.d01; d < gc.d01 +  gc.d12  + 0.00001; d +=  gc.d12 /10 ){
            p = screen.fromRadiansToPoint(gc.point(d));
            g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
            p0.x = p.x; p0.y = p.y;
        }

        double XT = boat.waypoints[1].binWidth*boat.waypoints[1].numberOfBins/phys.R;
        p0 = screen.fromRadiansToPoint(gc.crossTrack(gc.d01, XT, Math.toRadians(90) ));
        for (double d = gc.d01; d < gc.d01 +  gc.d12  + 0.00001; d +=  gc.d12 /10 ){
            p = screen.fromRadiansToPoint(gc.crossTrack(d,XT, Math.toRadians(90)));
            g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
            p0.x = p.x; p0.y = p.y;
        }
        p0 = screen.fromRadiansToPoint(gc.crossTrack(gc.d01, XT, Math.toRadians(-90) ));
        for (double d = gc.d01; d < gc.d01 +  gc.d12  + 0.00001; d +=  gc.d12 /10 ){
            p = screen.fromRadiansToPoint(gc.crossTrack(d,XT, Math.toRadians(-90)));
            g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
            p0.x = p.x; p0.y = p.y;
        }
        g.setColor(Color.red);
        p0 = screen.fromRadiansToPoint(gc.point(gc.d01));
        p = screen.fromRadiansToPoint(gc.crossTrack(gc.d01,XT, Math.toRadians(-50)));
        g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
        p = screen.fromRadiansToPoint(gc.crossTrack(gc.d01,XT, Math.toRadians(50)));
        g.drawLine((int)p0.x, (int) p0.y, (int) p.x, (int) p.y);
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

    @Override
    public void render(Renderer renderer, Projection projection, long time) {
        // Early return if not initialized
        if (boat == null || boat.waypoints == null || boat.waypoints.length < 2) return;
        if (newAgents == null || keyAgent == null) return;

        Vector2 p, p0;
        renderer.setColor(Colors.BLACK);

        GreatCircle gc = new GreatCircle(boat.waypoints[0].position, boat.waypoints[1].position);
        // Guard against zero distance (waypoints at same position) which causes infinite loop
        // This can happen when Depart and Expand share the same position
        if (gc.d12 <= 0 || Double.isNaN(gc.d12) || Double.isInfinite(gc.d12)) {
            // Draw agent tracks without the great circle reference track
            drawAgentTracks(renderer, projection, time);
            return;
        }

        p0 = projection.fromRadiansToPoint(gc.point(gc.d01));
        for (double d = gc.d01; d < gc.d01 + gc.d12 + 0.00001; d += gc.d12 / 10) {
            p = projection.fromRadiansToPoint(gc.point(d));
            renderer.drawLine(p0.x, p0.y, p.x, p.y);
            p0.x = p.x;
            p0.y = p.y;
        }

        double XT = boat.waypoints[1].binWidth * boat.waypoints[1].numberOfBins / phys.R;
        p0 = projection.fromRadiansToPoint(gc.crossTrack(gc.d01, XT, Math.toRadians(90)));
        for (double d = gc.d01; d < gc.d01 + gc.d12 + 0.00001; d += gc.d12 / 10) {
            p = projection.fromRadiansToPoint(gc.crossTrack(d, XT, Math.toRadians(90)));
            renderer.drawLine(p0.x, p0.y, p.x, p.y);
            p0.x = p.x;
            p0.y = p.y;
        }
        p0 = projection.fromRadiansToPoint(gc.crossTrack(gc.d01, XT, Math.toRadians(-90)));
        for (double d = gc.d01; d < gc.d01 + gc.d12 + 0.00001; d += gc.d12 / 10) {
            p = projection.fromRadiansToPoint(gc.crossTrack(d, XT, Math.toRadians(-90)));
            renderer.drawLine(p0.x, p0.y, p.x, p.y);
            p0.x = p.x;
            p0.y = p.y;
        }

        renderer.setColor(Colors.RED);
        p0 = projection.fromRadiansToPoint(gc.point(gc.d01));
        p = projection.fromRadiansToPoint(gc.crossTrack(gc.d01, XT, Math.toRadians(-50)));
        renderer.drawLine(p0.x, p0.y, p.x, p.y);
        p = projection.fromRadiansToPoint(gc.crossTrack(gc.d01, XT, Math.toRadians(50)));
        renderer.drawLine(p0.x, p0.y, p.x, p.y);
        renderer.setColor(Colors.BLACK);

        drawAgentTracks(renderer, projection, time);
    }

    private void drawAgentTracks(Renderer renderer, Projection projection, long time) {
        Vector2 p, p0;

        renderer.setColor(Colors.BLACK);
        renderer.setStrokeWidth(1);
        for (Agent X : newAgents) {
            if (X == keyAgent)
                renderer.setStrokeWidth(3);
            else
                renderer.setStrokeWidth(1);
            Agent Y = X;
            while (Y.previousAgent != null) {
                p0 = projection.fromRadiansToPoint(Y.position);
                p = projection.fromRadiansToPoint(Y.previousAgent.position);
                renderer.drawLine(p0.x, p0.y, p.x, p.y);
                Y = Y.previousAgent;
            }
        }

        renderer.setStrokeWidth(3);
        Agent Y = keyAgent;
        while (Y.previousAgent != null) {
            p0 = projection.fromRadiansToPoint(Y.position);
            p = projection.fromRadiansToPoint(Y.previousAgent.position);
            renderer.drawLine(p0.x, p0.y, p.x, p.y);
            Y = Y.previousAgent;
        }

        Y = keyAgent;
        while (Y.previousAgent != null) {
            if (Y.previousAgent.time <= time)
                break;
            if (Y.previousAgent.previousAgent == null)
                break;
            Y = Y.previousAgent;
        }
        if (Y.previousAgent == null) return;
        p0 = projection.fromRadiansToPoint(Y.position);
        p = projection.fromRadiansToPoint(Y.previousAgent.position);
        Vector2 dp = p0.minus(p);
        float t = (float) (time - Y.previousAgent.time) / (float) (Y.time - Y.previousAgent.time);
        p0 = p.plus(dp.scale(t));
        renderer.drawLine(p0.x, p0.y - 10, p0.x, p0.y + 10);
        renderer.drawLine(p0.x - 10, p0.y, p0.x + 10, p0.y);
        renderer.setStrokeWidth(1);
    }

    public void drawTWA(Graphics2D g) {
        int[] data = new int[19];
        int[] sails = new int[boat.polar.polars.size()];
        Agent Y = keyAgent;
        Vector2 vHeading;
        int count = 0;
        while(Y.previousAgent != null) {
            count ++;
            Vector2 tidalWind = Y.wind.minus(Y.tide);
            double trueWIndSpeed = tidalWind.mag();
            if (trueWIndSpeed == 0) {
                Y = Y.previousAgent;
                continue;
            }
            vHeading = new Vector2(Y.heading);

            double cosAngle = -tidalWind.dot(vHeading) / trueWIndSpeed;
            double TWA = Math.acos(cosAngle);
            data[(int)(Math.toDegrees(TWA)/10)]++;
            sails[Y.sail]++;
            // TODO integrate engine times
            Y = Y.previousAgent;
        }
//        for (int i = 0; i < sails.length ; i++) {
//            System.out.println(boat.polar.polars.get(i).name+": "+(sails[i]*100)/count +"%");
//        }
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

    public Long GetTime() {
        return keyAgent.time;
    }
    Long GetElapsedTime() {
        return keyAgent.time-((Depart) boat.waypoints[boat.currentWaypoint]).getTime();
    }

    static public boolean OutOfOrder(Agent first, Agent last) {
        Agent Y = last;
        while(true) {
            if(Y.previousAgent == first)
                return false;
            if(Y.previousAgent == null)
                break;
            Y = Y.previousAgent;
        }
        return true;
    }

    static public int GetTrackCount(Agent first, Agent last) {
        int count = 0;
        Agent Y = last;
        while (Y.previousAgent != first) {
            count++;
            Y = Y.previousAgent;
            if (Y == null)
                return -1;
        }
        return count;
    }

    public String GPX(String title) {
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb, Locale.UK);
        Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sb.append("\n\n<gpx>\n" +
                "<metadata><author>Course2Steer</author></metadata>\n" +
                "<trk>\n" +
                "<name>").append(title).append("</name>\n").append("<trkseg>\n");

        Agent A = keyAgent;
        LinkedList<Agent> track = new LinkedList<>();
        while(A.previousAgent != null) {
            track.add(A);
            A = A.previousAgent;
        }
        track = track.reversed();
        Vector2 vHeading;
        for (Agent Y : track) {
            Vector2 tidalWind = Y.wind.minus(Y.tide);
            double trueWIndSpeed = tidalWind.mag();
            if (trueWIndSpeed == 0) {
                continue;
            }
            vHeading = new Vector2(Y.heading);

            double cosAngle = -tidalWind.dot(vHeading) / trueWIndSpeed;
            double TWA = Math.acos(cosAngle);

            // TODO integrate engine times
            double latd = Math.toDegrees(Y.position.y);
            double lond = Math.toDegrees(Y.position.x);

            String tack = "Stbd";
            if(Y.portTack)
                tack = "Port";
            UTC.setTimeInMillis(Y.time);
            formatter.format("<trkpt lat=\"%.3f\" lon=\"%.3f\">\n" +
                            "<time>%s</time>\n" +
                            "<desc>%03.0fT %4.1fSOG %3.0fTWA %4.1fTWS %s %s</desc>\n" +
                            "</trkpt>\n",
                    latd,  lond, format.format(UTC.getTime()),
                    (Y.heading + 360) % 360, Y.VOG.mag() * phys.knots, Math.toDegrees(TWA), trueWIndSpeed * phys.knots,
                    tack, boat.polar.polars.get(Y.sail).name);
        }
        sb.append("</trkseg>\n" +
                "</trk>\n" +
                "</gpx>\n");
        return sb.toString();
    }

}
