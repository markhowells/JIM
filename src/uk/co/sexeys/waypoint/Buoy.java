package uk.co.sexeys.waypoint;

import uk.co.sexeys.*;

import java.awt.*;
import java.util.LinkedList;

public class Buoy extends Waypoint {
    public boolean clockwise;
    public float rounded;     // bearing to buoy when considered rounded
    public float costAngle; // bearing equal to highest cost
    public static final float relativeCostAngle = 45; // angle for reached and cost calculations


    public boolean Reached(Vector2 firstPoint,Vector2 secondPoint) {
        float sf = (greatCircle.BearingtoEnd(firstPoint) - rounded +360) %360;
        float ef = (greatCircle.BearingtoEnd(secondPoint) - rounded + 360) % 360;
        if (clockwise)
            return (sf > relativeCostAngle) && (ef < relativeCostAngle);
        else {
            return (sf < relativeCostAngle) && (ef > relativeCostAngle);
        }
    }

    public Vector2 Intersection(Vector2 start,Vector2 end) {
        // https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection
        float dx = (float) Math.sin(Math.toRadians(rounded));
        float dy = (float) Math.cos(Math.toRadians(rounded));

        float D = (start.x -end.x)*(dy) - (start.y - end.y) * (dx);
        float t = (start.x -position.x)*(dy) - (start.y - position.y) * (dx);
        float u = (end.x -start.x)*(start.y- position.y) - (end.y - start.y) * (start.x- position.x);
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
        Vector2 p = screen.fromRadiansToPoint(position);
        g.drawLine((int) p.x - 10, (int) p.y, (int) p.x + 10, (int) p.y);
        g.drawLine((int) p.x, (int) p.y - 10, (int) p.x, (int) p.y + 10);
        g.drawOval((int) p.x-15, (int) p.y-15,30,30);
        int r = (int) screen.fromLengthToPixels(position,numberOfBins*binWidth);

        float dx = (float) Math.sin(Math.toRadians(rounded));
        float dy = (float) Math.cos(Math.toRadians(rounded));
        Stroke current = g.getStroke();
        Stroke dashed = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{9}, 0);
        g.setStroke(dashed);
        g.drawLine((int) p.x , (int) p.y, (int) (p.x + dx*r), (int) (p.y - dy * r));
        g.setStroke(current);

        if(clockwise) {
            g.drawLine((int) p.x+15, (int) p.y, (int) p.x+20, (int) p.y - 5);
            g.drawLine((int) p.x+15, (int) p.y, (int) p.x+10, (int) p.y - 5);
        }
        else {
            g.drawLine((int) p.x+15, (int) p.y, (int) p.x+20, (int) p.y + 5);
            g.drawLine((int) p.x+15, (int) p.y, (int) p.x+10, (int) p.y + 5);
        }
        obstructions.Draw(g,screen);
    }

    public Buoy(String string, LinkedList<Obstruction> obstructions, Waypoint previous) {
        String format = "Buoy: 54*32.1'S 12*3.456'W clockwise|anticlockwise 100 degrees 360 bins of 4 nm 1 hour step\n";

        String[] temp = string.split("Buoy: ");
        String[] temp1 = temp[1].split("\n");
        String[] temp2 = temp1[0].split("\\s+");

        position = new Vector2(
                Fix.parseLongitudeDMS(temp2[1]),
                Fix.parseLatitudeDMS(temp2[0]))
                .scale(phys.radiansPerDegree);

        if(temp2[2].equals("clockwise"))
            clockwise = true;
        else if(temp2[2].equalsIgnoreCase("anticlockwise"))
            clockwise = false;
        else {
            System.out.print(
                    "Your input does not follow the format\n" + format +
                    "Specifically I was expecting a CLOCKWISE or ANTICLOCKWISE keyword at the end.\n");
            System.exit(0);
        }
        rounded = Float.parseFloat(temp2[3]);
        if (clockwise)
            costAngle = (rounded + relativeCostAngle +360) %360;
        else
            costAngle = (rounded - relativeCostAngle +360) %360;
        greatCircle = new GreatCircle(previous.position,position);
        ReadJIMData(temp2);
        obstructions.add(new Obstruction(position,Fix.destination(position,Math.toRadians(costAngle),numberOfBins*binWidth)));
        this.obstructions = new Obstruction(obstructions);
    }

}
