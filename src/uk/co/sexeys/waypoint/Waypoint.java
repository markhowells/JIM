package uk.co.sexeys.waypoint;

import uk.co.sexeys.*;
import java.awt.*;

public abstract class Waypoint {
    public Vector2 position = null; // radians
    public Obstruction obstructions;
    public int numberOfBins = 0;
    public float binWidth = 0; // m
    public long timeStep = 0; // ms
    public GreatCircle greatCircle = null; // route to this waypoint


    public boolean Reached(Vector2 dummy, Vector2 position) {
        if (this.position.x != position.x)
            return false;
        return this.position.y == position.y;
    }
    public Vector2 Intersection(Vector2 dummy, Vector2 dummy1) {
        return new Vector2 (position);
    }
    public Waypoint(){}

    Waypoint(String string) {
        String[] temp = string.split("Depart: ");
        String[] temp1 = temp[1].split("\n");
        String[] temp2 = temp1[0].split(" ");
        if (temp2.length != 3) {
            System.out.print(
"""
Your input does not follow the format
Waypoint: 54*32.1'S 12*3.456'W
Specifically I was expecting five space characters.
Send an email to help@course2steer.co.uk for help.
""");
            System.exit(0);
        }
        position = new Vector2(
                Fix.parseLatitudeDMS(temp2[0]),
                Fix.parseLongitudeDMS(temp2[1]))
                .scale(phys.radiansPerDegree);
    }

    public void Draw(Graphics2D g, Mercator screen) {
        Vector2 p = screen.fromRadiansToPoint(position);
        g.drawLine((int) p.x - 10, (int) p.y, (int) p.x + 10, (int) p.y);
        g.drawLine((int) p.x, (int) p.y - 10, (int) p.x, (int) p.y + 10);
        g.drawRect((int) p.x - 5, (int) p.y - 5,10, 10);
        obstructions.Draw(g,screen);
    }

    public void Draw(Graphics2D g, Mercator screen, long time) {
        System.out.println("Waypoint called with time...");
        Vector2 p = screen.fromRadiansToPoint(position);
        g.drawLine((int) p.x - 10, (int) p.y, (int) p.x + 10, (int) p.y);
        g.drawLine((int) p.x, (int) p.y - 10, (int) p.x, (int) p.y + 10);
        g.drawRect((int) p.x - 5, (int) p.y - 5,10, 10);
        obstructions.Draw(g,screen);
    }

    public void ReadJIMData(String[] fields) {
        int i = 0;
        for (String f:fields) {
            if (f.startsWith("bins"))
                break;
            i++;
        }
        if (i >= fields.length-2) {
            System.out.println("No JIM data in:");
            for (String f:fields)
                System.out.print(f+" ");
            System.out.println();
            System.exit(1);
        }
        try {
            numberOfBins = Integer.parseInt(fields[i - 1]);
        }
        catch (NumberFormatException e) {
            System.out.println("Failed to read JIM data");
            for (String f:fields)
                System.out.print(f+" ");
            System.out.println();
            System.out.println("Example: 1000 bins");
            System.exit(-1);
        }
        i = 0;
        for (String f:fields) {
            if (f.startsWith("of"))
                break;
            i++;
        }
        if (i < fields.length-2) {
            try {
                binWidth = Float.parseFloat((fields[i + 1]) ) * (float) phys.mPerNM;
            }
            catch (NumberFormatException e) {
                System.out.println("Failed to read JIM data");
                for (String f : fields)
                    System.out.print(f + " ");
                System.out.println();
                System.out.println("Example: of 4 nm");
                System.exit(-1);
            }
        }
        i = 0;
        for (String f:fields) {
            if (f.startsWith("hour"))
                break;
            i++;
        }
        if (i >= fields.length) {
            System.out.println("No JIM data in:");
            for (String f:fields)
                System.out.print(f+" ");
            System.out.println();
            System.exit(1);
        }
        try {
            timeStep = (long) (Float.parseFloat((fields[i - 1]) ) * phys.msPerHour);
        }
        catch (NumberFormatException e) {
            System.out.println("Failed to read JIM data");
            for (String f:fields)
                System.out.print(f+" ");
            System.out.println();
            System.out.println("Example: 1 hour step");
            System.exit(-1);
        }
    }

    public long getTime() {
        return -1;
    }
}
