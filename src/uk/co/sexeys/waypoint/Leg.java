package uk.co.sexeys.waypoint;

import uk.co.sexeys.*;

import java.awt.*;
import java.util.LinkedList;

public class Leg extends Waypoint{
    Vector2 approach;

    public boolean Reached(Vector2 start,Vector2 end) {
        //TODO probably needs cos latitude
        Vector2 relativePos = end.minus(position);
        return approach.dot(relativePos) > 0;
    }

    public Vector2 Intersection(Vector2 start,Vector2 end) {
        return end; // TODO needs proper calculation
    }


    public void Draw(Graphics2D g, Mercator screen) {
        Vector2 p = screen.fromRadiansToPoint(position);
        g.drawLine((int) p.x - 10, (int) p.y, (int) p.x + 10, (int) p.y);
        g.drawLine((int) p.x, (int) p.y - 10, (int) p.x, (int) p.y + 10);
        double XT = binWidth * numberOfBins / phys.R / 2;
        Stroke current = g.getStroke();
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g.setStroke(dashed);
        Vector2 p0 = screen.fromRadiansToPoint(greatCircle.crossTrack(greatCircle.d01 + greatCircle.d12, XT, Math.toRadians(90)));
        g.drawLine((int) p.x, (int) p.y, (int) p0.x, (int) p0.y );
        p0 = screen.fromRadiansToPoint(greatCircle.crossTrack(greatCircle.d01 + greatCircle.d12, XT, Math.toRadians(-90)));
        g.drawLine((int) p.x, (int) p.y, (int) p0.x, (int) p0.y );
        g.setStroke(current);
        obstructions.Draw(g,screen);
    }

    public Leg(String string, LinkedList<Obstruction> obstructions, Waypoint previous) {
        String format = "Leg: 54*32.1'S 12*3.456'W 1000 bins of 4 nm 1 hour step\n";

        String[] temp = string.split("Leg: ");
        String[] temp1 = temp[1].split("\n");
        String[] temp2 = temp1[0].split(" ");

        position = new Vector2(
                Fix.parseLongitudeDMS(temp2[1]),
                Fix.parseLatitudeDMS(temp2[0]))
                .scale(phys.radiansPerDegree);

        greatCircle = new GreatCircle(previous.position,position);
        approach = new Vector2(Math.toDegrees(greatCircle.alpha2));
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
