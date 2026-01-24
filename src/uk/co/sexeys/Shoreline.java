package uk.co.sexeys;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Shoreline {
    static final double resolution = 0.25; //degree
    Line[][][] dataset;

    static class Line {
        Vector2 start,end;
        Line(Vector2 start, Vector2 end) {
            this.start = start;
            this.end = end;
        }
        Line() {}
    }
    public void scale(float WVSResolution) {
        WVS wvs;
        wvs = new WVS(WVSResolution);
        Iterator<Vector2> dataI;
        LinkedList<Line> lines = new LinkedList<>();
        Vector2 p;
        System.out.println("Loading WVS database");
        try {
            for (int lat = -90; lat < 90; lat++) {
                System.out.print("\r"+Main.spinner[(Main.spinnerCounter++)%4]);
                for (int lon = Main.minLon; lon < Main.maxLon; lon++) {
                    WVS.GridPoint data = wvs.GetGridPoint(lat, lon);
                    dataI = data.data.iterator();
                    for (int nPoints : data.segray) {
                        Line line = new Line();
                        line.start = dataI.next();
                        for (int i = 1; i < nPoints; i++) {
                            try {
                                p = dataI.next();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            line.end = p;
                            lines.add(line);
                            line = new Line();
                            line.start = p;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        double maxLat;
        double minLat;
        double maxLon;
        double minLon;
        class Point {
            final Vector2 p;
            final double d;

            Point(Vector2 pos, Vector2 ref) {
                p = pos;
                double dx = ref.x - pos.x;
                double dy = ref.y - pos.y;
                d = dx * dx + dy * dy;
            }
        }
        LinkedList<Point> points = new LinkedList<>();
        LinkedList<Line> splitLines = new LinkedList<>();
        int count = 0;
        System.out.println("\rSegmenting WVS lines");
        for (Line line : lines) {
            if (count % 10000 == 0)
                System.out.print("\r"+Main.spinner[(Main.spinnerCounter++)%4]);
            count++;
            if (line.start.x > line.end.x) {
                maxLon = line.start.x;
                minLon = line.end.x;
            } else {
                maxLon = line.end.x;
                minLon = line.start.x;
            }
            if (line.start.y > line.end.y) {
                maxLat = line.start.y;
                minLat = line.end.y;
            } else {
                maxLat = line.end.y;
                minLat = line.start.y;
            }
            for (double i = Math.ceil(minLat / resolution + 1e-10) * resolution; i < Math.ceil(maxLat / resolution - 1e-10) * resolution; i += resolution) {
                p = new Vector2((i - line.start.y) /
                        (line.end.y - line.start.y) *
                        (line.end.x - line.start.x) +
                        line.start.x,
                        i);
                points.add(new Point(p, line.start));
            }
            for (double i = Math.ceil(minLon / resolution + 1e-10) * resolution; i < Math.ceil(maxLon / resolution - 1e-10) * resolution; i += resolution) {
                p = new Vector2(i, (i - line.start.x) /
                        (line.end.x - line.start.x) *
                        (line.end.y - line.start.y) +
                        line.start.y);
                points.add(new Point(p, line.start));
            }
            Vector2 lineStart = line.start;
            while (points.size() > 0) {
                Point closest = points.getFirst();
                for (Point point : points) {
                    if (point.d < closest.d)
                        closest = point;
                }
                splitLines.add(new Line(lineStart, closest.p));
                lineStart = closest.p;
                points.remove(closest);
            }

            splitLines.add(new Line(lineStart, line.end));
        }
        int latBins = (int) (180 / resolution);
        int lonBins = (int) ((Main.maxLon-Main.minLon) / resolution);
        LinkedList<Line>[][] bins = new LinkedList[latBins][lonBins];
        for (int i = 0; i < latBins; i++) {
            for (int j = 0; j < lonBins; j++) {
                bins[i][j] = new LinkedList<>();
            }
        }
        System.out.println("\rBuilding WVS database");
        for (Line l : splitLines) {
            if (count % 10000 == 0)
                System.out.print("\r" + Main.spinner[(Main.spinnerCounter++) % 4]);
            count++;
            minLat = l.start.y;
            if (l.end.y < minLat)
                minLat = l.end.y;
            minLon = l.start.x;
            if (l.end.x < minLon)
                minLon = l.end.x;
            if (minLon >= Main.maxLon) {
                minLon = Main.maxLon - resolution;
            }
            if (minLon < Main.minLon) {
                minLon = Main.minLon;
            }
            int latBin = (int) Math.floor((minLat + 90) / resolution);
            int lonBin = (int) Math.floor((minLon - Main.minLon) / resolution);
            bins[latBin][lonBin].add(l);
        }
        dataset = new Line[latBins][lonBins][];
        for (int i = 0; i < latBins; i++) {
            System.out.print("\r" + Main.spinner[(Main.spinnerCounter++) % 4]);
            for (int j = 0; j < lonBins; j++) {
                if (bins[i][j].size() != 0)
                    dataset[i][j] = bins[i][j].toArray(new Line[0]);
            }
        }
    }
    public Shoreline(float WVSResolution) {
        scale(WVSResolution);
    }

    Vector2 p1,p2;
    boolean Intersection(Vector2 start, List<Fix> track) {
        p1 = start;
        for (Fix fix : track) {
            p2 = fix.position;
            if (Intersection(p1, p2))
                return true;
            p1 = p2;
        }
        return false;
    }
    Vector2 startD = new Vector2();
    Vector2 endD = new Vector2();
    public boolean Intersection(Vector2 start, Vector2 end) {
        if (Obstruction.Intersection(start,end))
            return true;
        Line[] data;
        startD.x = start.x* phys.degrees-Main.ChartOffsetX;
        startD.y = start.y* phys.degrees-Main.ChartOffsetY;
        endD.x = end.x* phys.degrees-Main.ChartOffsetX;
        endD.y = end.y* phys.degrees-Main.ChartOffsetY;

        double startX = startD.x/resolution -  Main.minLon/resolution;
        double startY = startD.y/resolution +  90/resolution;
        double endX = endD.x/resolution -  Main.minLon/resolution;
        double endY = endD.y/resolution +  90/resolution;
        int X = (int) Math.floor(startX);
        int Y = (int) Math.floor(startY);
        if (dataset[Y][X] != null) {
            data = dataset[Y][X];
            for (Line l : data) {
                int o1 = orientation(startD, endD, l.start);
                int o2 = orientation(startD, endD, l.end);
                if(o1 == o2)
                    continue;
                int o3 = orientation(l.start, l.end, startD);
                int o4 = orientation(l.start, l.end, endD);

                if (o3 != o4)
                    return true;
            }
        }
        int ex = (int) Math.floor(endX);
        int ey = (int) Math.floor(endY);
        double dx = endX - startX;
        double dy = endY - startY;
        if(X == ex) {
            if (Y == ey) {// quick exit
                    return false;
            }
            dx = 0;
        }
        if (Y == ey) {
            dy = 0;
        }

        int stepX = 1;
        int stepY = 1;
        double tMaxX = 1-(startX -X); // distance to go
        double tMaxY = 1-(startY -Y);
        if(dx < 0) {
            stepX = -1;
            tMaxX = 1-tMaxX;
        }
        if(dy < 0) {
            stepY = -1;
            tMaxY = 1-tMaxY;
        }

        double dxdy = Math.abs(dx/dy);
        double dydx = Math.abs(dy/dx);
        double testValue;
        while ( (X != ex) || (Y != ey) )
        {
            testValue = tMaxX * dydx;
            if(tMaxY == testValue) { // Straight through a grid point
                tMaxY =1;
                tMaxX =1;
                Y +=stepY;
                X +=stepX;
            }
            else if (tMaxY < testValue) { // cross x boundary first
                Y +=stepY;
                tMaxX -= tMaxY * dxdy;
                tMaxY =1;
            }
            else {
                X +=stepX;
                tMaxX =1;
                tMaxY -= testValue;
            }
//            if(Y >= 1800) {
//                System.out.println("Y too big. Oops");
//            }
//            if(Y < 0) {
//                System.out.println("Y too small. Oops");
//            }
            if(X >= dataset[0].length) {
                return true;
            }
            if(X < 0) {
                return true;
            }
            if(dataset[Y][X] == null)
                continue;
            data = dataset[Y][X];
            for (Line l :data) {
                int o1 = orientation(startD, endD, l.start);
                int o2 = orientation(startD, endD, l.end);
                if(o1 == o2)
                    continue;
                int o3 = orientation(l.start, l.end, startD);
                int o4 = orientation(l.start, l.end, endD);
                if (o3 != o4)
                    return true;
            }
        }
        return false;
    }

    static int orientation(Vector2 p, Vector2 q, Vector2 r)
    {
        // See https://www.geeksforgeeks.org/orientation-3-ordered-points/
        // for details of below formula.;
        float val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);

        if (val == 0) return 0;  // colinear

        return (val > 0)? 1: 2; // clock or counterclock wise
    }
    public static LinkedList<Vector2> GridTraversal (Vector2 x, Vector2 y) {
        LinkedList<Vector2> pixels = new LinkedList<>();
        Vector2 gridX = x.scale(1/resolution); // TODO convert to multiplication
        Vector2 gridY = y.scale(1/resolution);
        int X = (int) Math.floor(gridX.x);
        int Y = (int) Math.floor(gridX.y);
        int ex = (int) Math.floor(gridY.x);// End cell coords
        int ey = (int) Math.floor(gridY.y);// End cell coords
        pixels.add(new Vector2(X,Y));
        if(X == ex && Y == ey) // quick exit
            return pixels;

        Vector2 d = gridY.minus(gridX);
        int stepX = 1;
        int stepY = 1;
        if(d.x < 0)
            stepX = -1;
        if(d.y < 0)
            stepY = -1;
        double tMaxX = 1-(gridX.x -X); // distance to go
        double tMaxY = 1-(gridX.y -Y);

        double dxdy = Math.abs(d.x/d.y);
        double dydx = Math.abs(d.y/d.x);
        double testValue;
        while ( (X != ex) || (Y != ey) )
        {
            testValue = tMaxX * dydx;
            if(tMaxY == testValue) { // Straight through a grid point
                tMaxY =1;
                tMaxX =1;
                Y +=stepY;
                X +=stepX;
            }
            else if (tMaxY < testValue) { // cross x boundary first=?
                Y +=stepY;
                tMaxY =1;
                tMaxX -= tMaxY * dxdy;
            }
            else {
                X +=stepX;
                tMaxX =1;
                tMaxY -= testValue;
            }
            pixels.add(new Vector2(X,Y));
        }
        return pixels;
    }

    void testline() {
        Vector2 start = new Vector2(1,1);
        Vector2 end = new Vector2(0,0);
        LinkedList<Vector2> pixels = GridTraversal(start, end);

        boolean[][] grid = new boolean[11][11];
        for (Vector2 p:pixels) {
            grid[ (int) p.y][ (int) p.x] = true;
        }
        for (int i = grid.length-1; i >= 0; i--) {
            for (int j = 0; j < grid[i].length; j++) {
                if (grid[i][j])
                    System.out.print("*");
                else
                    System.out.print(" ");
            }
            System.out.print("\n");
        }
        System.exit(0);
    }
    void PrintDataset() {
        for (int i = dataset.length - 1; i >= dataset.length / 2; i--) {
            for (int j = dataset[i].length / 2; j < dataset[i].length; j++) {
                if (dataset[i][j] != null) {
                    int s = dataset[i][j].length;
                    if (s < 10)
                        System.out.print(".");
                    else
                        System.out.print("*");
                } else
                    System.out.print(" ");
            }
            System.out.println();
        }
    }

    void draw(Graphics2D g, Mercator screen) {
        Path2D results = new Path2D.Float();
        Vector2 p;
        g.setColor(Color.black);

        int startLat = (int) Math.floor((screen.bottomRight.y+90)/resolution);
        if(startLat< 0)
            startLat = 0;
        int endLat = (int) Math.ceil((screen.topLeft.y+90)/resolution);
        if(endLat > 180/resolution)
            endLat = (int)  (180/resolution);
        int startLong = (int) Math.floor((screen.topLeft.x+180)/resolution);
        if(startLong<0)
            startLong += (int) (360/resolution);
        int endLong = (int) Math.ceil((screen.bottomRight.x+180)/resolution);
        if(endLong > 360/resolution)
            endLong -= (int)  (360/resolution);
        Line[] data;
        for (int lat = startLat; lat <= endLat; lat++) {
            for (int lon = startLong; lon < endLong; lon++) {
                if (dataset[lat][lon] == null)
                    continue;
                data = dataset[lat][lon];
                for (Line l : data) {
                    p = screen.fromLatLngToPoint(l.start.y+Main.ChartOffsetY, l.start.x+Main.ChartOffsetX);
                    results.moveTo(p.x, p.y);
                    p = screen.fromLatLngToPoint(l.end.y+Main.ChartOffsetY, l.end.x+Main.ChartOffsetX);
                    results.lineTo(p.x, p.y);
                }
            }
        }
        g.draw(results);
    }
}
