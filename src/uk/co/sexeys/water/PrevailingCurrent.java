package uk.co.sexeys.water;

import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.units.DateUnit;
import ucar.units.UnitException;
import uk.co.sexeys.Main;
import uk.co.sexeys.Vector2;
import uk.co.sexeys.phys;

import java.io.File;
import java.io.IOException;
import java.util.*;

// https://data.marine.copernicus.eu/products
// Climatology, month of year
// cmems_mod_glo_phy_my_0.083deg-climatology_P1M-m
// copernicusmarine subset -i cmems_mod_glo_phy_my_0.083deg-climatology_P1M-m -v uo -v vo -Z 1 -f prevailingCurrents.nc

public class PrevailingCurrent extends Water{

    //    private final ArrayList<Record> data = new ArrayList<>();
    private Record[] data = new Record[12];
    private long referenceTime;
    private  static long msPerMonth = (long) (phys.msPerDay * 365.25 / 12); // Crude estimate
    private final Vector2 a = new Vector2();
    private final Vector2 b = new Vector2();

    static class Record {
        short[] u,v;
        int stride;
        float top, left, bottom,right, fx, fy;
        float scaleFactor;

        Record (Record c) {
            u = c.u.clone();
            v = c.v.clone();
            stride = c.stride;
            left = c.left;
            right = c.right;
            top = c.top;
            bottom = c.bottom;
            fx = c.fx;
            fy = c.fy;
            scaleFactor = c.scaleFactor;
        }
        Record() {}
        Record(float uf, float vf) {
            scaleFactor = 1;
            byte ub = (byte) (uf/scaleFactor);
            byte vb = (byte) (vf/scaleFactor);
            u = new short[] {ub,ub,ub,ub};
            v = new short[] {vb,vb,vb,vb};
            stride = 2;
            left =   -190;
            right =  180;
            bottom =  -90; // somewhere we cannot get
            top =  -89;
            fx =  (stride-1) / (right - left);
            fy =  (u.length/stride-1) / (top - bottom);
        }

        void getValue (Vector2 p, Vector2 out) { //TODO very slow in inner loop
            final float dlon = p.x - left;
            final float x = dlon * fx;
            final float dlat = p.y - bottom;
            float y = dlat * fy;

            final int i = (int) x;
            final int j = (int) y;
            final int offset00 = i + j * stride;
            final int offset10 = offset00 + 1;
            final int offset01 = offset00 + stride;
            final int offset11 = offset01 + 1;
            final float dx = x - i;
            final float dy = y - j;
            final float dx1 = 1 - dx; float dy1 = 1 - dy;
            final float A = dx1 * dy1; float B = dx * dy1; float C = dx1 * dy; float D = dx * dy;
            out.x = (u[offset00] * A + u[offset10] * B + u[offset01] * C + u[offset11] * D)*scaleFactor;
            out.y = (v[offset00] * A + v[offset10] * B + v[offset01] * C + v[offset11] * D)*scaleFactor;
        }
    }

    public SOURCE getValue(Vector2 p, long t, Vector2 value) {
        if(p.y < -80) { // Quick catch for Antarctica
            value.x = value.y = 0;
            return SOURCE.FIXED;
        }
        long deltaTime = t - referenceTime;
        int monthBefore = (int) (deltaTime / msPerMonth) % 12;
        int monthAfter = (monthBefore + 1) % 12;

        data[monthBefore].getValue(p,a);
        data[monthAfter].getValue(p,b);
        final double dt = msPerMonth;
        double j = deltaTime/dt;
        j = j - Math.floor(j);

        final double A = (1-j);

        value.x = (float) (a.x* A + b.x * j);
        value.y = (float) (a.y* A + b.y * j);
        return SOURCE.PREVAILING;
    }

    public PrevailingCurrent(long time) {
        Calendar refTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        refTime.setTimeInMillis(time);
        int year = refTime.get(Calendar.YEAR);
        // TODO set one year from current month, not from January
        refTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        refTime.set(year, Calendar.JANUARY,1);
        referenceTime = refTime.getTimeInMillis();
        final ArrayList<Record> dataAL = new ArrayList<>();
        String file = Main.root+"Prevailing"+ File.separator + "prevailingCurrents.nc";
        NetcdfFile ncfile = null;
        try {
            ncfile = NetcdfFile.open(file);
            Variable timeVar = ncfile.findVariable("time");
            Variable latVar = ncfile.findVariable("latitude");
            Variable lonVar = ncfile.findVariable("longitude");
            Variable uoVar = ncfile.findVariable("uo");
            Variable voVar = ncfile.findVariable("vo");
            if (null == timeVar || null == latVar || null == lonVar || null == uoVar || null == voVar) return;
            Array times = timeVar.read();
            Attribute units = timeVar.findAttribute("units");
            String tunitsString = units.getStringValue();
            DateUnit referenceUnit = new DateUnit(tunitsString);
            IndexIterator ti = times.getIndexIterator();
            Array lats = latVar.read();
            Array lons = lonVar.read();
            Array uoData = uoVar.read(":,:,:,:");
            Array voData = voVar.read(":,:,:,:");
            Attribute uoUnits = uoVar.findAttribute("scale_factor");
            float scaleFactor = uoUnits.getNumericValue().floatValue();

            int i = 0;
            while (ti.hasNext()) {
                ti.getFloatNext();
                Record r = new Record();
                long nx = lons.getSize();
                long ny = lats.getSize();
                r.scaleFactor = scaleFactor;
                r.stride = (int) nx;
                r.top = lats.getFloat((int) (lats.getSize() - 1));
                r.left = lons.getFloat(0);
                r.bottom = lats.getFloat(0);
                r.right = lons.getFloat((int) (lons.getSize() - 1));
                r.fx = (nx-1) / (r.right - r.left);
                r.fy = (ny-1) / (r.top - r.bottom);
                Array u = uoData.slice(0,i);
                Array v = voData.slice(0,i);

                r.u = (short []) u.copyTo1DJavaArray();
                r.v = (short []) v.copyTo1DJavaArray();
                for (int j = 0; j < r.u.length; j++) {
                    if (r.u[j] < -10000) { //TODO Have to make decision what to do with out of bound data
                        r.u[j] = 0;
                        r.v[j] = 0;
                    }
                }
                dataAL.add(r);
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnitException e) {
            e.printStackTrace();
        } catch (InvalidRangeException e) {
            e.printStackTrace();
        } finally {
            if (null != ncfile) try {
                ncfile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }        }
//        System.out.println("Read tide file: " + file);
        data = dataAL.toArray(new Record[0]);
    }
}


