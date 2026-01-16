package uk.co.sexeys.waypoint;

import uk.co.sexeys.*;

import java.awt.*;
import java.util.LinkedList;

public class Diode extends Gate{
    public int handedness; // 1: clockwise or -1: anticlockwise

    public boolean Reached(Vector2 start,Vector2 end) {
        int o4 = Vector2.orientation(firstPoint, extent, end);
        if (o4 != handedness)
            return false;
        int o3 = Vector2.orientation(firstPoint, extent, start);
        if( o3 == o4 )
            return false;
        int o1 = Vector2.orientation(start, end, firstPoint);
        int o2 = Vector2.orientation(start, end, extent);
        return o1 != o2;
    }

    public Vector2 Intersection(Vector2 start,Vector2 end) {
        // https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection
        float D = (start.x -end.x)*(firstPoint.y- extent.y) - (start.y - end.y) * (firstPoint.x- extent.x);
        float t = (start.x -firstPoint.x)*(firstPoint.y- extent.y) - (start.y - firstPoint.y) * (firstPoint.x- extent.x);
        float u = (end.x -start.x)*(start.y- firstPoint.y) - (end.y - start.y) * (start.x- firstPoint.x);
        if(D> 0 ) {
            if (t < 0 || t > D) {
                return null;
            }
            if (u < 0 || u > D) {
                return null;
            }
        }
        else {
            if (t > 0 || t < D) {
                return null;
            }
            if (u > 0 || u < D) {
                return null;
            }
        }
        float tD = t/D;
        return new Vector2(start.x + tD*(end.x-start.x), start.y + tD*(end.y-start.y));
    }


    public void Draw(Graphics2D g, Mercator screen) {
        Vector2 p = screen.fromRadiansToPoint(firstPoint);
        g.drawLine((int) p.x - 10, (int) p.y, (int) p.x + 10, (int) p.y);
        g.drawLine((int) p.x, (int) p.y - 10, (int) p.x, (int) p.y + 10);
        g.drawOval((int) p.x-15, (int) p.y-15,30,30);
        if(handedness == -1) {
            g.drawLine((int) p.x+15, (int) p.y, (int) p.x+20, (int) p.y - 5);
            g.drawLine((int) p.x+15, (int) p.y, (int) p.x+10, (int) p.y - 5);
        }
        else {
            g.drawLine((int) p.x+15, (int) p.y, (int) p.x+20, (int) p.y + 5);
            g.drawLine((int) p.x+15, (int) p.y, (int) p.x+10, (int) p.y + 5);
        }

        Vector2 q = screen.fromRadiansToPoint(extent);
        g.drawLine((int) q.x - 10, (int) q.y, (int) q.x + 10, (int) q.y);
        g.drawLine((int) q.x, (int) q.y - 10, (int) q.x, (int) q.y + 10);
        Stroke current = g.getStroke();
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g.setStroke(dashed);
        g.drawLine((int) q.x, (int) q.y, (int) p.x, (int) p.y );
        g.setStroke(current);
        obstructions.Draw(g,screen);
    }

    public Diode(String string, LinkedList<Obstruction> obstructions, Waypoint previous) {
        String format = "Diode: 54*32.1'S 12*3.456'W to 54*35.1'S 12*3.456'W clockwise|anticlockwise 1000 bins of 4 nm 1 hour step\n";

        String[] temp = string.split("Diode: ");
        String[] temp1 = temp[1].split("\n");
        String[] temp2 = temp1[0].split(" ");
        if (temp2.length != 14) {
            System.out.print(
                    "Your input does not follow the format\n" + format +
                            "Specifically I was expecting fourteen space characters.\n");
            System.exit(0);
        }
        if (!temp2[2].equals("to")) {
            System.out.print(
                    "Your input does not follow the format\n" + format +
                            "Specifically I was expecting a to keyword between positions.\n");
            System.exit(0);
        }
        firstPoint = new Vector2(
                Fix.parseLongitudeDMS(temp2[1]),
                Fix.parseLatitudeDMS(temp2[0]))
                .scale(phys.radiansPerDegree);
        extent = new Vector2(
                Fix.parseLongitudeDMS(temp2[4]),
                Fix.parseLatitudeDMS(temp2[3]))
                .scale(phys.radiansPerDegree);
        if(temp2[5].equals("clockwise"))
            handedness = -1;
        else if(temp2[5].equalsIgnoreCase("anticlockwise"))
            handedness = 1;
        else {
            System.out.print(
                    "Your input does not follow the format\n" + format +
                            "Specifically I was expecting a CLOCKWISE or ANTICLOCKWISE keyword at the end.\n");
            System.exit(0);
        }
        position = firstPoint.plus(extent).scale(0.5);
        greatCircle = new GreatCircle(previous.position,position);
        ReadJIMData(temp2);

        double XT = binWidth*(numberOfBins+1)/phys.R/2;
        Vector2 pA = greatCircle.crossTrack(greatCircle.d01+ greatCircle.d12, XT, Math.toRadians(90) );
        Vector2 pB = greatCircle.crossTrack(greatCircle.d01+ greatCircle.d12, XT, Math.toRadians(-90) );
        if (GreatCircle.range(firstPoint,pA) < GreatCircle.range(extent,pA)) {
            obstructions.add(new Obstruction(firstPoint,pA));
            obstructions.add(new Obstruction(extent,pB));
        }
        else {
            obstructions.add(new Obstruction(extent,pA));
            obstructions.add(new Obstruction(firstPoint,pB));
        }
        this.obstructions = new Obstruction(obstructions);
    }
}
