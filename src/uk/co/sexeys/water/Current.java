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


import java.io.*;

import java.util.*;
import java.util.List;

/**
 * Created by Jim on 14/06/2018.
 * June 2025 Major overhaul to new Copernicus standard
 * https://www.unidata.ucar.edu/blogs/news/entry/netcdf-java-version-5-8
 */

public class Current extends Water{
    //    private final ArrayList<Record> data = new ArrayList<>();
    Record[] data;
    public final Vector2 a = new Vector2();
    public final Vector2 b = new Vector2();
    int lastIndex = 1;
    Water prevailing;

    static class Record {
        //            short[] u,v;
        float[] u,v;
        long time;
        int stride;
        float top, left, bottom,right, fx, fy;
        float scaleFactor;

        Record() {}
        Record (Record c) {
            u = c.u.clone();
            v = c.v.clone();
            time = c.time;
            stride = c.stride;
            left = c.left;
            right = c.right;
            top = c.top;
            bottom = c.bottom;
            fx = c.fx;
            fy = c.fy;
            scaleFactor = c.scaleFactor;
        }
        Record(float uf, float vf, Calendar now) {
            scaleFactor = 0.01f;
            byte ub = (byte) (uf/scaleFactor);
            byte vb = (byte) (vf/scaleFactor);
            time = now.getTimeInMillis();
            time -= 6L * 30L * phys.msPerDay;
//                u = new short[] {ub,ub,ub,ub};
//                v = new short[] {vb,vb,vb,vb};
            u = new float[] {ub,ub,ub,ub};
            v = new float[] {vb,vb,vb,vb};
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
            try {
                out.x = (u[offset00] * A + u[offset10] * B + u[offset01] * C + u[offset11] * D)*scaleFactor;
                out.y = (v[offset00] * A + v[offset10] * B + v[offset01] * C + v[offset11] * D)*scaleFactor;
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new RuntimeException(e);
            }
            if (out.x >100)
                System.out.println("Value too high");
        }
    }

    private int findRecord(long t) {
        try { // optimised for searches moving forward in time
            if(t > data[data.length-1].time)
                return -1;
            if(t < data[0].time)
                return -1;
            if(t < data[lastIndex-1].time)
                lastIndex = 1;
            while (data[lastIndex].time <= t)
                lastIndex++;
            return lastIndex;
        } catch (IndexOutOfBoundsException e) {
            return -1;
        }
    }


    public SOURCE getValue(Vector2 p, long t, Vector2 value) { // interpolate over time
        if ( findRecord(t) < 0) {
            lastIndex = 1;
            return prevailing.getValue(p,t,value);
        }
        Record box = data[lastIndex]; // TODO need to generalise for different sized gribs
        if ( (box.right <= p.x) ||
                (box.left > p.x) ||
                (box.bottom > p.y) ||
                (box.top <= p.y) ){
            return prevailing.getValue(p, t, value);
        }
        data[lastIndex].getValue(p,a);
        box = data[lastIndex-1];
        if ((box.right <= p.x) ||
                (box.left > p.x) ||
                (box.bottom > p.y) ||
                (box.top <= p.y) ){
            return prevailing.getValue(p, t, value);
        }
        data[lastIndex-1].getValue(p,b);
        final long ta = data[lastIndex].time;
        final long tb = data[lastIndex-1].time;
        final double dt = tb - ta;
        final double df = t - ta;
        final double j = df/dt;
        final double A = (1-j);

        value.x = (float) (a.x* A + b.x * j);
        value.y = (float) (a.y* A + b.y * j);
        if (value.x >100)
            System.out.println("Value ntoo high");
        return SOURCE.CURRENT;
    }

    static final String fileFormat = Main.root+"tide"+ File.separator+"tide%4d%02d%02d.rec";

    public Current(List<String> files, Water prevailing) {
        super();// List of tide files (without extension)
        final ArrayList<Record> dataAL = new ArrayList<>();
        for (String f: files) {
//                System.out.print("\r"+f);
            ReadFile(f,dataAL);
        }
        data = dataAL.toArray(new Record[0]);
        this.prevailing = prevailing;
    }
//


    void ReadFile(String file, ArrayList<Record> data )  {
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
//                Array uoData = uoVar.read(":,:,:");
//                Array voData = voVar.read(":,:,:");
            Array uoData = uoVar.read(":,:,:,:");
            Array voData = voVar.read(":,:,:,:");
//                Attribute uoUnits = uoVar.findAttribute("scale_factor");
//                float scaleFactor = uoUnits.getNumericValue().floatValue();

            int i = 0;
            while (ti.hasNext()) {
                Record r = new Record();
                long nx = lons.getSize();
                long ny = lats.getSize();
                r.time =  referenceUnit.makeDate(  ti.getFloatNext()  ).getTime();
//                    r.scaleFactor = scaleFactor;
                r.scaleFactor = 1.0f;
                r.stride = (int) nx;
                r.top = lats.getFloat((int) (lats.getSize() - 1));
                r.left = lons.getFloat(0);
                r.bottom = lats.getFloat(0);
                r.right = lons.getFloat((int) (lons.getSize() - 1));
                r.fx = (nx-1) / (r.right - r.left);
                r.fy = (ny-1) / (r.top - r.bottom);
                Array u = uoData.slice(0,i);
                Array v = voData.slice(0,i);

//                    r.u = (short []) u.copyTo1DJavaArray();
//                    r.v = (short []) v.copyTo1DJavaArray();
                r.u = (float []) u.copyTo1DJavaArray();
                r.v = (float []) v.copyTo1DJavaArray();
                for (int j = 0; j < r.u.length; j++) {
//                        if (r.u[j] < -10000) { //TODO Have to make decision what to do with out of bound data
                    if (r.u[j] > 10000) { //TODO Have to make decision what to do with out of bound data
                        r.u[j] = 0;
                        r.v[j] = 0;
                    }
                }
                data.add(r);
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
            }
        }
//            System.out.println("Read tide file: " + file);
    }
}

// http://www.globalmarinenet.com/free-grib-file-downloads/


