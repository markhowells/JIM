package uk.co.sexeys.waypoint;

import uk.co.sexeys.*;

import java.awt.*;
import java.util.LinkedList;

public class Destination extends Waypoint{
    float radius2; // radians2
    float radius; // m


    Destination(double lat, double lon, float  radius) {
        this.position.x = (float) (lon * phys.radiansPerDegree);
        this.position.y = (float) (lat * phys.radiansPerDegree);
        this.radius = radius;
        radius2 = (float) ( radius* radius/phys.R/phys.R);
    }

    public boolean Reached(Vector2 dummy, Vector2 position) {
        float distance = Fix.range(this.position,position);
        return (distance < radius);
    }

    public Vector2 Intersection(Vector2 start, Vector2 end) {
        // https://mathworld.wolfram.com/Circle-LineIntersection.html
        Mercator m = new Mercator(position, phys.mPerNM*60*Math.cos(position.y));
        Vector2 p1 = m.fromRadiansToPoint(start);
        Vector2 p2 = m.fromRadiansToPoint(end);
        Vector2 d = p2.minus(p1);

        // TODO cos latitude
        float dr2 = d.mag2();
        float D = p1.x * p2.y - p2.x * p1.y;
        float delta2 = radius*radius * dr2 - D*D;
        if (delta2 < 0)
            return null; // No intersection
        float delta = (float) Math.sqrt(delta2);
        float x1 = ( D * d.y + Math.signum(d.y) * d.x * delta) /dr2;
        float x2 = ( D * d.y - Math.signum(d.y) * d.x * delta) /dr2;
        float y1 = (-D * d.x + Math.abs(d.y) * delta) /dr2;
        float y2 = (-D * d.x - Math.abs(d.y) * delta) /dr2;
        float d1 = (p1.x -x1)*(p1.x -x1) + (p1.y -y1)*(p1.y - y1);
        float d2 = (p1.x -x2)*(p1.x -x2) + (p1.y -y2)*(p1.y - y2);
        Vector2 intersection;
        if(d1 < d2) {
            intersection = m.fromPointToLatLng(x1,y1);
        }
        else
            intersection = m.fromPointToLatLng(x2,y2);
        return intersection.scale(phys.radiansPerDegree);
    }

    public void Draw(Graphics2D g, Mercator screen) {
        Vector2 p = screen.fromRadiansToPoint(position);
        g.drawLine((int) p.x - 10, (int) p.y, (int) p.x + 10, (int) p.y);
        g.drawLine((int) p.x, (int) p.y - 10, (int) p.x, (int) p.y + 10);
        int r = (int) screen.fromLengthToPixels(position, radius);
        Stroke current = g.getStroke();
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g.setStroke(dashed);
        g.drawOval((int) p.x-r,(int) p.y-r,r*2,r*2);
        g.setStroke(current);
        obstructions.Draw(g,screen);
    }

    public Destination(String string, LinkedList<Obstruction> obstructions, Waypoint previous) {
        String format = "Destination: 54*32.1'S 12*3.456'W 1 nm 1000 bins of 4 nm 1 hour step\n";
        String[] temp = string.split("Destination: ");
        String[] temp1 = temp[1].split("\n");
        String[] temp2 = temp1[0].split(" ");
        if (temp2.length != 12) {
            System.out.print(
                    "Your input does not follow the format\n" + format +
                            "Specifically I was expecting twelve space characters.\n");
            System.exit(0);
        }
        if (!temp2[3].equals("nm")) {
            System.out.print(
                    "Your input does not follow the format\n" + format +
                            "Specifically I can only work with destination radii in nm.\n");
            System.exit(0);
        }
        position = new Vector2(
                Fix.parseLongitudeDMS(temp2[1]),
                Fix.parseLatitudeDMS(temp2[0]))
                .scale(phys.radiansPerDegree);
        this.radius = Float.parseFloat(temp2[2]) * (float) phys.mPerNM;
        radius2 = (float) ( radius* radius/phys.R/phys.R);
        if ( (previous.position.x != position.x) || (previous.position.y != position.y) )
            greatCircle = new GreatCircle(previous.position,position);
        else
            greatCircle = previous.greatCircle;
        ReadJIMData(temp2);
//        if (previous instanceof Diode) {
//            double XT = binWidth*numberOfBins/phys.R/2;
//            Vector2 pA = greatCircle.crossTrack(greatCircle.d01, XT, Math.toRadians(90) );
//            Vector2 pB = greatCircle.crossTrack(greatCircle.d01, XT, Math.toRadians(-90) );
//            if (GreatCircle.range(((Diode) previous).firstPoint,pA) < GreatCircle.range(((Diode) previous).extent,pA)) {
//                obstructions.add(new Obstruction(((Diode) previous).firstPoint,pA));
//                obstructions.add(new Obstruction(((Diode) previous).extent,pB));
//            }
//            else {
//                obstructions.add(new Obstruction(((Diode) previous).extent,pA));
//                obstructions.add(new Obstruction(((Diode) previous).firstPoint,pB));
//            }
//        }
        this.obstructions = new Obstruction(obstructions);
    }
}
