package uk.co.sexeys.waypoint;

import uk.co.sexeys.*;

import java.awt.*;
import java.util.LinkedList;

public class Expand extends Waypoint{
    private final float radius;
    public boolean Reached(Vector2 dummy, Vector2 position) {
        float distance = Fix.range(this.position,position);
        return (distance > radius);
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

    public Expand(String string, LinkedList<Obstruction> obstructions, Waypoint previous) {
        String format = "Expand: 1 nm 1000 bins of 4 nm 1 hour step\n";
        String[] temp = string.split("Expand: ");
        String[] temp1 = temp[1].split("\n");
        String[] temp2 = temp1[0].split(" ");
        if (temp2.length != 7) {
            System.out.print(
                    "Your input does not follow the format\n" + format +
                            "Specifically I was expecting seven space characters.\n");
            System.exit(0);
        }
        if (!temp2[1].equals("nm")) {
            System.out.print(
                    "Your input does not follow the format\n" + format +
                            "Specifically I can only work with expanding radii in nm.\n");
            System.exit(0);
        }
        position = previous.position;
        this.radius = Float.parseFloat(temp2[0]) * (float) phys.mPerNM;
        ReadJIMData(temp2);
        this.obstructions = new Obstruction(obstructions);
    }
}
