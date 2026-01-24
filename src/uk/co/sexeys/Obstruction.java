package uk.co.sexeys;

import uk.co.sexeys.rendering.Colors;
import uk.co.sexeys.rendering.Projection;
import uk.co.sexeys.rendering.Renderable;
import uk.co.sexeys.rendering.Renderer;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Obstruction implements Renderable {
    static public Obstruction active = null;
    public Line[] data = null;

    public class Line {
        Vector2 start,end;
    }
    public Obstruction(LinkedList<Obstruction> obstructions) {
        if(obstructions == null) {
            data = null;
            return;
        }
        if(obstructions.size() == 0){
            data = null;
            return;
        }
        LinkedList<Line> lines = new LinkedList<>();
        for(Obstruction o:obstructions) {
            Collections.addAll(lines, o.data);
        }
        data = lines.toArray(new Line[0]);
    }


    public void Draw(Graphics2D g, Mercator screen) {
        if(data != null) {
            g.setColor(Color.RED);

            for (Line l : data) {
                Vector2 p = screen.fromLatLngToPoint(l.start.y,l.start.x);
                Vector2 q = screen.fromLatLngToPoint(l.end.y,l.end.x);
                g.drawLine((int) q.x, (int) q.y, (int) p.x, (int) p.y);
            }
            g.setColor(Color.BLACK);
        }
    }

    @Override
    public void render(Renderer renderer, Projection projection, long time) {
        if (data != null) {
            renderer.setColor(Colors.RED);

            for (Line l : data) {
                Vector2 p = projection.fromLatLngToPoint(l.start.y, l.start.x);
                Vector2 q = projection.fromLatLngToPoint(l.end.y, l.end.x);
                renderer.drawLine(q.x, q.y, p.x, p.y);
            }
            renderer.setColor(Colors.BLACK);
        }
    }
    public Obstruction(String obstructions) {
        LinkedList<Line> lines = new LinkedList<>();
        String[] obstructionLine = obstructions.split("Obstruction: ");
        String[] points = obstructionLine[1].split(";");
        for (int j = 1; j < points.length; j++) {
            Line line = new Line();
            String[] coordinates = points[j-1].split(" ");
            line.start = new Vector2(Fix.parseLongitudeDMS(coordinates[1]),Fix.parseLatitudeDMS(coordinates[0]));
            coordinates = points[j].split(" ");
            line.end = new Vector2(Fix.parseLongitudeDMS(coordinates[1]),Fix.parseLatitudeDMS(coordinates[0]));
            lines.add(line);
        }
        data = lines.toArray(new Line[0]);
    }
    public Obstruction(Vector2 start, Vector2 end) {
        data = new Line[1];
        data[0] = new Line();
        data[0].start = new Vector2(start.x *phys.degrees, start.y *phys.degrees);
        data[0].end = new Vector2(end.x *phys.degrees, end.y *phys.degrees);
    }
    static Vector2 p1,p2;
    public static boolean Intersection(Vector2 start, List<Fix> track) {
        p1 = start;
        for (Fix fix : track) {
            p2 = fix.position;
            if (Intersection(p1, p2))
                return true;
            p1 = p2;
        }
        return false;
    }
    static Vector2 startD = new Vector2();
    static Vector2 endD = new Vector2();
    public static boolean Intersection(Vector2 start, Vector2 end) {
        if(active == null)
            return false;
        if(active.data == null)
            return false;
        startD.x = start.x* phys.degrees;
        startD.y = start.y* phys.degrees;
        endD.x = end.x* phys.degrees;
        endD.y = end.y* phys.degrees;

        for (Line l : active.data) {
            int o1 = Vector2.orientation(startD, endD, l.start);
            int o2 = Vector2.orientation(startD, endD, l.end);
            int o3 = Vector2.orientation(l.start, l.end, startD);
            int o4 = Vector2.orientation(l.start, l.end, endD);
            if (o1 != o2 && o3 != o4)
                return true;
        }
        return false;
    }
}
