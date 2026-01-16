package uk.co.sexeys.wind;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.units.DateUnit;
import ucar.units.UnitException;

import uk.co.sexeys.Main;
import uk.co.sexeys.Vector2;

import java.io.*;
import java.util.*;

public class SailDocs extends Wind{
    private final List<Record> dataAL = new ArrayList<>();
    final private Record[] data;
    Wind prevailing;
    int lastIndex = 1;

    static class Record  implements Comparable<Record> {
        float[] u,v;
        long time;
        int stride;
        float top, left, bottom,right, fx, fy;

        // define sorting logic
        @Override
        public int compareTo(Record o) {

            // Ascending order
            return (int) (this.time - o.time);
        }

        Record() {
            u = null;
            v = null;
        }

        int getValue (Vector2 p, Vector2 out) { //TODO very slow in inner loop
            if (right <= p.x)
                return -1;
            if (left >= p.x)
                return -1;
            if (bottom > p.y)
                return -1;
            if (top <= p.y)
                return -1;
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
            if(offset11 > u.length) {
                System.out.println("index error");
            }
            out.x = u[offset00] * A + u[offset10] * B + u[offset01] * C + u[offset11] * D;
            out.y = v[offset00] * A + v[offset10] * B + v[offset01] * C + v[offset11] * D;
            return 0;
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

    private final Vector2 a = new Vector2();
    private final Vector2 b = new Vector2();

    public SOURCE getValue(final Vector2 p, final long t, final Vector2 value) { // interpolate over time
        if ( findRecord(t) < 0) {
            lastIndex = 1;
            return prevailing.getValue(p,t,value);
        }
        Record box = data[lastIndex]; // TODO need to generalise for different sized gribs
        if ( (box.right < p.x) ||
                (box.left > p.x) ||
                (box.bottom > p.y) ||
                (box.top < p.y) ){
            return prevailing.getValue(p, t, value);
        }
        data[lastIndex].getValue(p,a);
        data[lastIndex-1].getValue(p,b);
        final long ta = data[lastIndex].time;
        final long tb = data[lastIndex-1].time;
        final double dt = tb - ta;
        final double df = t - ta;
        final double j = df/dt;
        final double A = (1-j);

        value.x = (float) (a.x* A + b.x * j);
        value.y = (float) (a.y* A + b.y * j);
        return SOURCE.LATEST;
    }

    private List<Record> readRecords(String file) {
        List<Record> records = new ArrayList<>();;
        NetcdfFile ncfile = null;
        try {
            ncfile = NetcdfFile.open(file);
            Group root = ncfile.getRootGroup();
            ImmutableList<Group> groups = root.getGroups();
            if (groups.isEmpty()) {
                groups = ImmutableList.of(root);
            }

            UnmodifiableIterator<Group> groupIterator = groups.iterator();
            while (groupIterator.hasNext()) {
                Group group = groupIterator.next();
                Variable timeVar = group.findVariable("time");
                // Assumes that only a single height above ground is downloaded
                Variable latVar = group.findVariable("lat");
                Variable lonVar = group.findVariable("lon");
                Variable uoVar = group.findVariable("u-component_of_wind_height_above_ground");
                Variable voVar = group.findVariable("v-component_of_wind_height_above_ground");
                if (null == timeVar || null == latVar || null == lonVar || null == uoVar || null == voVar) {
                    System.out.println("A variable could not be found - need to debug...\n");
                    System.exit(-1);
                }

                Array times = timeVar.read();
                System.out.println("times: "+times.getSize());
                Attribute units = timeVar.findAttribute("units");
                String tunitsString = units.getStringValue();
                System.out.println(tunitsString);
                DateUnit referenceUnit = new DateUnit(tunitsString);
                IndexIterator ti = times.getIndexIterator();
                Array lats = latVar.read();
                Array lons = lonVar.read();
                Array uoData = uoVar.read(":,:,:,:");
                Array voData = voVar.read(":,:,:,:");
                int index = 0;
                while (ti.hasNext()) {
                    Record record = new Record();
                    long nx = lons.getSize();
                    long ny = lats.getSize();
                    record.time = referenceUnit.makeDate(ti.getFloatNext()).getTime();
                    record.stride = (int) nx;
                    record.top = lats.getFloat((int) (lats.getSize() - 1));
                    record.left = lons.getFloat(0);
                    record.bottom = lats.getFloat(0);
                    record.right = lons.getFloat((int) (lons.getSize() - 1));
                    record.fx = (nx - 1) / (record.right - record.left);
                    Array u = uoData.slice(0, index);
                    Array v = voData.slice(0, index);
                    index++;

                    record.u = (float[]) u.copyTo1DJavaArray();
                    record.v = (float[]) v.copyTo1DJavaArray();
                    if (Float.isNaN(record.u[0]))
                        System.out.println("NaN");
                    if (record.u == null || record.v == null) {
                        System.out.println("U or V nor decoded for some reason Exiting.\n");
                        System.exit(-1);
                    }
                    if (record.u.length != record.v.length) {
                        System.out.println("U and V length not equal for some reason Exiting.\n");
                        System.exit(-1);
                    }
                    if (record.top < record.bottom) { //reorder in memory
                        float[] temp = new float[record.u.length];
                        int i = 0;
                        int rowEnd = record.u.length;
                        int rowStart = rowEnd - record.stride;
                        do {
                            for (int j = rowStart; j < rowEnd; j++) {
                                temp[i++] = record.u[j];
                            }
                            rowEnd -= record.stride;
                            rowStart -= record.stride;
                        } while (i < record.u.length);
                        record.u = temp;
                        temp = new float[record.v.length];
                        i = 0;
                        rowEnd = record.v.length;
                        rowStart = rowEnd - record.stride;
                        do {
                            for (int j = rowStart; j < rowEnd; j++) {
                                temp[i++] = record.v[j];
                            }
                            rowEnd -= record.stride;
                            rowStart -= record.stride;
                        } while (i < record.v.length);
                        record.v = temp;
                        float tempTop = record.top;
                        record.top = record.bottom;
                        record.bottom = tempTop;
                    }
                    if (record.right > 180) { //reorder in memory
                        float[] temp = new float[record.u.length];
                        int i = 0;
                        int rowEnd = record.stride;
                        int midRow = record.stride / 2;
                        int rowStart = 0;
                        do {
                            for (int j = midRow; j < rowEnd; j++) {
                                temp[i++] = record.u[j];
                            }
                            for (int j = rowStart; j < midRow; j++) {
                                temp[i++] = record.u[j];
                            }
                            rowEnd += record.stride;
                            midRow += record.stride;
                            rowStart += record.stride;
                        } while (i < temp.length);
                        record.u = temp;
                        temp = new float[record.v.length];
                        i = 0;
                        rowEnd = record.stride;
                        midRow = record.stride / 2;
                        rowStart = 0;
                        do {
                            for (int j = midRow; j < rowEnd; j++) {
                                temp[i++] = record.v[j];
                            }
                            for (int j = rowStart; j < midRow; j++) {
                                temp[i++] = record.v[j];
                            }
                            rowEnd += record.stride;
                            midRow += record.stride;
                            rowStart += record.stride;
                        } while (i < temp.length);
                        record.v = temp;
                        record.left = -180;
                        record.right = 180;
                    }
                    record.fy = ((float) record.u.length / record.stride - 1) / (record.top - record.bottom);

                    records.add(record);
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println(" not found on disk. Skipping...");
        } catch (IOException | InvalidRangeException | UnitException e) {
            e.printStackTrace();
        } finally {
            if (null != ncfile) try {
                ncfile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return records;
    }

    public SailDocs(List<String> files, Wind prevailing) {
        this.prevailing = prevailing;
        for (String file: files) {
//            System.out.print("\r"+file);
            List<Record> records = this.readRecords(file);
            dataAL.addAll(records);
        }
        Collections.sort(dataAL);
        long lastTime = 0;
        for (Record r: dataAL) {
            if (r.time <= lastTime) {
                System.out.println("Wind grib files are not in time ascending order");
                System.exit(-1);
            }
            lastTime = r.time;
        }
        // Add a prevailing record to end of data to get smooth transition in time
        Record record = new Record();
        Record last = dataAL.getLast();
        record.time =  last.time+ Main.prevailingTransitionPeriod;
        record.stride = last.stride;
        record.top = last.top;
        record.left = last.left;
        record.bottom = last.bottom;
        record.right = last.right;
        record.fx = last.fx;
        record.fy = last.fy;
        record.u = new float[last.u.length];
        record.v = new float[last.v.length];
        Vector2 p = new Vector2();
        Vector2 value = new Vector2();;
        for (int i = 0; i < record.u.length; i ++) {
            p.x = i % record.stride / record.fx + record.left;
            //noinspection IntegerDivisionInFloatingPointContext
            p.y = (i / record.stride) / record.fy + record.bottom;
            prevailing.getValue(p,record.time, value);
            record.u[i] = value.x;
            record.v[i] = value.y;
        }
        dataAL.add(record);
        data = dataAL.toArray(new Record[0]);
    }

    public long GetLastValidForecast() {
        return data[data.length -2].time;
    }

    SOURCE getSOurce(int value){
        if (1 == value)
            return SOURCE.LATEST;
        return SOURCE.PREVAILING;
    }
}


