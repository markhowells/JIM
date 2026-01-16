package uk.co.sexeys.CMap;

import java.io.InputStream;

public class GeometryDescriptor {

    public final int index;
    public final int nPoints;
    public java.util.ArrayList<Object> points = new java.util.ArrayList<>();

    public int xmin = Integer.MAX_VALUE;
    public int xmax = Integer.MIN_VALUE;
    public int ymin = Integer.MAX_VALUE;
    public int ymax = Integer.MIN_VALUE;

    public GeometryDescriptor(InputStream is, int index) throws Exception {
        this.index = index;
        this.nPoints = DecodeTables.readShort(is);
    }

    public void Read2DPoints(InputStream is) throws Exception {
        for (int i = 0; i < nPoints; i++) {
            int x = DecodeTables.readShort(is);
            int y = DecodeTables.readShort(is);
            C93Point p = new C93Point(x, y);
            points.add(p);

            xmin = Math.min(xmin, x);
            xmax = Math.max(xmax, x);
            ymin = Math.min(ymin, y);
            ymax = Math.max(ymax, y);
        }
    }

    public void Read3DPoints(InputStream is) throws Exception {
        for (int i = 0; i < nPoints; i++) {
            int x = DecodeTables.readShort(is);
            int y = DecodeTables.readShort(is);
            int z = DecodeTables.readShort(is);
            points.add(new C93Point3D(x, y, z));
        }
    }
}
