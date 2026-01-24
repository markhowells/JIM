package uk.co.sexeys.CMap;
// CMap.java
// Converted from Python/PyQt5 to Java Swing (December 2025) using ChatGPT


import uk.co.sexeys.Mercator;
import uk.co.sexeys.Vector2;
import uk.co.sexeys.rendering.Projection;
import uk.co.sexeys.rendering.Renderable;
import uk.co.sexeys.rendering.Renderer;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class CMap implements Renderable {
    public int scaleLevel;

    private final CIBCache cibCache = new CIBCache();
    private java.util.List<DrawingInstructions> instructions = new ArrayList<>();

    public CMap(int scale) {
        this.scaleLevel = scale;
    }

    public void update(Mercator screen){
        if (scaleLevel < 0) return; // Chart is off
        double dLon = screen.lon2-screen.lon1;
        if (dLon > CIB.scales[3][1]/3.0) scaleLevel = 3;
        if (dLon > CIB.scales[2][1]/3.0) scaleLevel = 2;
        if (dLon > CIB.scales[1][1]/3.0) scaleLevel = 1;
        if (dLon > CIB.scales[0][1]/3.0) scaleLevel = 0;
        List<CIB> cibs = new ArrayList<>();
        try {
            cibs = cibCache.GetChartFiles(screen.lat1, screen.lon1, screen.lat2, screen.lon2, scaleLevel);
        } catch (Exception ex) {
            System.out.println("Could not read CIBs");
        }
        java.util.List<DrawingInstructions> newInstructions = new ArrayList<>();
        DrawingInstructions found = null;
        for (CIB cib : cibs) {
            boolean available = false;
            for (DrawingInstructions di : instructions) {
                if (di.cib == cib) {
                    available = true;
                    found = di;
                    break;
                }
            }
            if (available) newInstructions.add(found);
            else newInstructions.add(new DrawingInstructions(cib));
        }
        instructions = newInstructions;
    }
    public void draw(Graphics g, Mercator screen) {
        if (scaleLevel < 0) return; // Chart is off
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (DrawingInstructions inst : instructions) {
            Vector2 leftBottom = screen.fromLatLngToPoint(inst.cib.lat_min, inst.cib.lon_min);
            Vector2 rightTop = screen.fromLatLngToPoint(inst.cib.lat_max, inst.cib.lon_max);

            int left = (int) leftBottom.x;
            int bottom = (int) leftBottom.y;
            int right = (int) rightTop.x;
            int top = (int) rightTop.y;

            if (left > screen.x2[0]) continue;
            if (right < 0) continue;
            if (bottom < 0) continue;
            if (top > screen.y2[0]) continue;

            double f_x = (double) (right - left) / 65536.0;
            double f_y = (double) (top - bottom) / 65536.0;

            AffineTransform trans = new AffineTransform();
            trans.translate(left, bottom);
            trans.scale(f_x, f_y);

            g2.setPaint(new Color(0xD7FFFF));
            g2.setStroke(new BasicStroke(1f));
            for (Polygon poly : inst.depth10m) {
                Shape s = trans.createTransformedShape(poly);
                g2.fill(s);
            }

            g2.setPaint(new Color(0xA2E0EB));
            for (Polygon poly : inst.shallow) {
                Shape s = trans.createTransformedShape(poly);
                g2.fill(s);
            }
            for (Polygon poly : inst.obstruction) {
                Shape s = trans.createTransformedShape(poly);
                g2.fill(s);
            }

            g2.setPaint(new Color(0x62AC71));
            for (Polygon poly : inst.interTidal) {
                Shape s = trans.createTransformedShape(poly);
                g2.fill(s);
            }

            g2.setPaint(new Color(0xF6C96E));
            for (Polygon poly : inst.land) {
                Shape s = trans.createTransformedShape(poly);
                g2.fill(s);
            }

            g2.setPaint(new Color(0xC59C4D));
            for (Polygon poly : inst.builtUp) {
                Shape s = trans.createTransformedShape(poly);
                g2.fill(s);
            }

            g2.setPaint(Color.BLACK);
            g2.setStroke(new BasicStroke(1f));
            for (Path2D path : inst.coastline) {
                Shape s = trans.createTransformedShape(path);
                g2.draw(s);
            }

            g2.setStroke(new BasicStroke(2f));
            Point t = new Point();
            for (Point p : inst.rocks) {
                trans.transform(p,t);
                g2.drawLine(t.x - 5, t.y, t.x + 5, t.y);
                g2.drawLine(t.x, t.y - 5, t.x, t.y + 5);
            }

            g2.setPaint(Color.RED);
            for (double[] c : inst.childCharts) {
                Vector2 p1 = screen.fromLatLngToPoint(c[0], c[1]);
                Vector2 p2 = screen.fromLatLngToPoint(c[2], c[3]);
                g2.drawRect((int) p1.x, (int) p2.y, (int) (p2.x - p1.x), (int) (p1.y - p2.y));
            }
        }
        g2.dispose();
    }

    @Override
    public void render(Renderer renderer, Projection projection, long time) {
        if (scaleLevel < 0) return; // Chart is off

        for (DrawingInstructions inst : instructions) {
            Vector2 leftBottom = projection.fromLatLngToPoint(inst.cib.lat_min, inst.cib.lon_min);
            Vector2 rightTop = projection.fromLatLngToPoint(inst.cib.lat_max, inst.cib.lon_max);

            float left = leftBottom.x;
            float bottom = leftBottom.y;
            float right = rightTop.x;
            float top = rightTop.y;

            // Skip if outside viewport
            if (left > projection.getWidth()) continue;
            if (right < 0) continue;
            if (bottom < 0) continue;
            if (top > projection.getHeight()) continue;

            float f_x = (right - left) / 65536.0f;
            float f_y = (top - bottom) / 65536.0f;

            // depth10m - light cyan
            renderer.setColor(0xD7, 0xFF, 0xFF);
            renderer.setSolidStroke(1f);
            for (Polygon poly : inst.depth10m) {
                renderPolygon(renderer, poly, left, bottom, f_x, f_y);
            }

            // shallow and obstruction - blue-cyan
            renderer.setColor(0xA2, 0xE0, 0xEB);
            for (Polygon poly : inst.shallow) {
                renderPolygon(renderer, poly, left, bottom, f_x, f_y);
            }
            for (Polygon poly : inst.obstruction) {
                renderPolygon(renderer, poly, left, bottom, f_x, f_y);
            }

            // interTidal - green
            renderer.setColor(0x62, 0xAC, 0x71);
            for (Polygon poly : inst.interTidal) {
                renderPolygon(renderer, poly, left, bottom, f_x, f_y);
            }

            // land - tan/sand color
            renderer.setColor(0xF6, 0xC9, 0x6E);
            for (Polygon poly : inst.land) {
                renderPolygon(renderer, poly, left, bottom, f_x, f_y);
            }

            // builtUp - darker tan
            renderer.setColor(0xC5, 0x9C, 0x4D);
            for (Polygon poly : inst.builtUp) {
                renderPolygon(renderer, poly, left, bottom, f_x, f_y);
            }

            // coastline - black lines
            renderer.setColor(0, 0, 0);
            renderer.setSolidStroke(1f);
            for (Path2D path : inst.coastline) {
                renderPath(renderer, path, left, bottom, f_x, f_y);
            }

            // rocks - black crosses
            renderer.setSolidStroke(2f);
            for (Point p : inst.rocks) {
                float tx = left + p.x * f_x;
                float ty = bottom + p.y * f_y;
                renderer.drawLine(tx - 5, ty, tx + 5, ty);
                renderer.drawLine(tx, ty - 5, tx, ty + 5);
            }

            // childCharts - red rectangles
            renderer.setColor(255, 0, 0);
            for (double[] c : inst.childCharts) {
                Vector2 p1 = projection.fromLatLngToPoint(c[0], c[1]);
                Vector2 p2 = projection.fromLatLngToPoint(c[2], c[3]);
                renderer.drawRect(p1.x, p2.y, p2.x - p1.x, p1.y - p2.y);
            }
        }
    }

    private void renderPolygon(Renderer renderer, Polygon poly, float offsetX, float offsetY, float scaleX, float scaleY) {
        if (poly.npoints == 0) return;
        float[] xPoints = new float[poly.npoints];
        float[] yPoints = new float[poly.npoints];
        for (int i = 0; i < poly.npoints; i++) {
            xPoints[i] = offsetX + poly.xpoints[i] * scaleX;
            yPoints[i] = offsetY + poly.ypoints[i] * scaleY;
        }
        renderer.fillPolygon(xPoints, yPoints, poly.npoints);
    }

    private void renderPath(Renderer renderer, Path2D path, float offsetX, float offsetY, float scaleX, float scaleY) {
        PathIterator pi = path.getPathIterator(null);
        float[] coords = new float[6];
        float lastX = 0, lastY = 0;
        while (!pi.isDone()) {
            int type = pi.currentSegment(coords);
            float x = offsetX + coords[0] * scaleX;
            float y = offsetY + coords[1] * scaleY;
            if (type == PathIterator.SEG_LINETO) {
                renderer.drawLine(lastX, lastY, x, y);
            }
            lastX = x;
            lastY = y;
            pi.next();
        }
    }
}

class DrawingInstructions {
    public CIB cib;
    public java.util.List<Polygon> land = new ArrayList<>();
    public java.util.List<Path2D> coastline = new ArrayList<>();
    public java.util.List<Polygon> shallow = new ArrayList<>();
    public java.util.List<Polygon> depth10m = new ArrayList<>();
    public java.util.List<Point> rocks = new ArrayList<>();
    public java.util.List<Polygon> interTidal = new ArrayList<>();
    public java.util.List<Polygon> seaBed = new ArrayList<>();
    public java.util.List<Polygon> obstruction = new ArrayList<>();
    public java.util.List<Polygon> builtUp = new ArrayList<>();
    public java.util.List<double[]> childCharts = new ArrayList<>();

    public static final Attributes attributes = new Attributes();
    public static final ObjectTypes objectTypes = new ObjectTypes();

    public DrawingInstructions(CIB cib) {
        this.cib = cib;
        for (GeometryObject o : cib.pobject_block) {
            switch (o.object_type) {
                case 78:
                    GetPolygon(o, this.interTidal);
                    break;
                case 81:
                    GetPolygon(o, this.land);
                    break;
                case 138:
                    GetPolygon(o, this.seaBed);
                    break;
                case 99:
                case 176:
                    GetPolygon(o, this.obstruction);
                    break;
                case 35:
                    Path2D.Double path = GetPath(o);
                    if (path != null) coastline.add(path);
                    break;
                case 14:
                case 139:
                    GetPolygon(o, this.builtUp);
                    break;
                case 44:
                    List<Float> depths = attributes.getFloats(o.attributes_block);
                    if (!depths.isEmpty()) {
                        if (depths.getLast() == 10.0) {
                            GetPolygon(o, this.depth10m);
                        } else if (depths.getLast() < 10.0) {
                            GetPolygon(o, this.shallow);
                        }
                    }
                    break;
                case 168:
                    rocks.add(getPoint(o));
                    break;
                default:
                    // other types ignored
                    break;
            }
        }
        List<String> higherScales = cib.GetHigherScales();
        for (String f : higherScales) {
            CIB c = null;
            try {
                c = new CIB(f,true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            childCharts.add(new double[]{c.lat_min, c.lon_min, c.lat_max, c.lon_max});
        }
    }

    private void GetPolygon(GeometryObject geometry, java.util.List<Polygon> output) {
        if ((geometry.geotype & 0x0f) != 4) return;

        java.util.List<Point> segment = new ArrayList<>();
        Polygon poly = new Polygon();

        for (int index : geometry.pGeometry) {
            java.util.List<Point> points = new ArrayList<>();
            boolean reverse = (index & 0x8000) == 0x8000;
            int edgeIndex = index & 0x1fff;
            GeometryDescriptor evd = geometry.cib.edge_vector_descriptor_block.get(edgeIndex);

            if (reverse) {
                for (int i = evd.points.size() - 1; i >= 0; i--) {
                    C93Point p = (C93Point)  evd.points.get(i);
                    points.add(new Point(p.x(), p.y()));
                }
            } else {
                for (Object o: evd.points) {
                    C93Point p = (C93Point)  o;
                    points.add(new Point(p.x(), p.y()));
                }
            }


            if ((index & 0x4000) == 0x4000) {
                if (poly.npoints == 0) {
                    for (Point p : segment) poly.addPoint((int) p.x, (int) p.y);
                }
                segment = new ArrayList<>();
            } else {
                segment.addAll(points);
            }
        }
        if (poly.npoints == 0) {
            for (Point p : segment) poly.addPoint((int) p.x, (int) p.y);
        }
        if (poly.npoints > 0) output.add(poly);
    }

    private Path2D.Double GetPath(GeometryObject geometry) {
        if ((geometry.geotype & 0x0f) != 2) return null;
        Path2D.Double path = new Path2D.Double();

        for (int index : geometry.pGeometry) {
            boolean reverse = (index & 0x8000) == 0x8000;
            int edgeIndex = index & 0x1fff;
            GeometryDescriptor evd = geometry.cib.edge_vector_descriptor_block.get(edgeIndex);

            if (reverse) {
                C93Point p = (C93Point) evd.points.getLast();
                if (path.getCurrentPoint() == null) path.moveTo(p.x(), p.y());
                else path.lineTo(p.x(), p.y());
                for (int i = evd.points.size() - 1; i >= 0; i--) {
                    p = (C93Point)  evd.points.get(i);
                    path.lineTo(p.x(), p.y());
                }
            } else {
                C93Point p = (C93Point) evd.points.getFirst();
                if (path.getCurrentPoint() == null) path.moveTo(p.x(), p.y());
                else path.lineTo(p.x(), p.y());
                for (int i = 1; i < evd.points.size(); i++) {
                    p = (C93Point)  evd.points.get(i);
                    path.lineTo(p.x(), p.y());
                }
            }

            if ((index & 0x4000) == 0x4000) {
                System.out.println("not sure what this means for a path");
            }
        }
        return path;
    }

    public static Point getPoint(GeometryObject geometry) {
        if ( (geometry.geotype & 0x0F) != 1 ) {
            return null;
        }
        int index = geometry.pGeometry.getFirst();
        C93Point p = geometry.cib.p2dpoint_array.get(index);

        return new Point(p.x(), p.y());
    }
}