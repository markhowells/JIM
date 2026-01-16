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

public class Tide extends Water{
    //    private final ArrayList<Record> data = new ArrayList<>();
    Record[] data;

    public final Vector2 a = new Vector2();
    public final Vector2 b = new Vector2();
    int lastIndex = 1;

    Water prevailing;

    static class Record {
        short[] u,v;
        long time;
        int stride;
        float top, left, bottom,right, fx, fy;
        float scaleFactor;
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
        Record() {}
        Record(float uf, float vf, Calendar now) {
            scaleFactor = 0.01f;
            byte ub = (byte) (uf/scaleFactor);
            byte vb = (byte) (vf/scaleFactor);
            time = now.getTimeInMillis();
            time -= 6L * 30L * phys.msPerDay;
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



        Record(File file, int index) {
            //            float[] data;
            //            float[] timesD;
            //            try {
            //                Link link = file.findLink("lat");
            //                Object object = link.object;
            //                int size = object.dataLayout.ArraySize();
            //                data = object.dataTree.ReadFloat(0,size);
            //                bottom = data[0];
            //                top = data[data.length-1];
            //                fy = (data.length) / (top - bottom);  // TODO
            //
            //                link = file.findLink("lon");
            //                object = link.object;
            //                size = object.dataLayout.ArraySize();
            //                data = object.dataTree.ReadFloat(0,size);
            //                left = data[0];
            //                right = data[data.length-1];
            //
            //                stride = data.length +1;  //TODO
            //
            //                fx = (stride - 1) / (right - left);
            //
            //                link = file.findLink("time");
            //                object = link.object;
            //                size = object.dataLayout.ArraySize();
            //                timesD = object.dataTree.ReadFloat(0,size);
            //                Attribute units = object.findAttribute("units");
            //                String[] splitUnits = units.getString().split("since ");
            //                String baseTime = splitUnits[1].replace('T', ' ').replace('Z',' ');
            //                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            //                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            //                try {
            //                    cal.setTime(sdf.parse(baseTime));
            //                } catch (ParseException e) {
            //                    System.out.println("Could not parse time units in HDF5 file: "+splitUnits[1]);
            //                }
            //                time = cal.getTimeInMillis() + (long) timesD[index] * phys.msPerHour;
            //
            //                u = Read0mData(file,"uo", index);
            //                v = Read0mData(file,"vo", index);
            //
            //            } catch (IOException e) {
            //                e.printStackTrace();
            //                System.exit(-1);
            //            }
        }

        byte[] Read0mData(File file, String linkName, int index) throws IOException {
            //            Link link = file.findLink(linkName);
            //            Object object = link.object;
            //            long[] searchValues = {index,0,-1,-1};
            //            ArrayList<Tree.Key> results = new ArrayList<>();
            //            object.dataTree.findKeys(searchValues,results);
            //
            //            int size = 1;
            //            for (int i = 2; i < object.dataSpace.dimensions.length; i++)
            //                size *= (object.dataSpace.dimensions[i]+1); // because data is odd
            //            byte[] data = new byte[size];
            //            size = 1;
            //            for (int i = 2; i < object.dataLayout.dimensions.length; i++)
            //                size *= object.dataLayout.dimensions[i];
            //            float[] rawY = new float[size];
            //            float temp;
            //            for (Tree.Key key: results) {
            //                if (key.dataNotValid())
            //                    continue;
            //                ByteBuffer bb = key.ReadData();
            //                for (int i = 0; i < rawY.length; i++) {
            //                    temp = bb.getFloat();
            //                    if (-5 > temp)
            //                        rawY[i] = 0; // TODO find nearest?
            //                    else
            //                        rawY[i] = temp;
            //                }
            //
            //                int rawIndex = 0;
            //                for (int i = 0; i < object.dataLayout.dimensions[2]; i++) {
            //                    int dataIndex = (int) ((i + key.chunkOffset[2]) * (object.dataSpace.dimensions[3] + 1) +
            //                            key.chunkOffset[3]);
            //                    for (int j = 0; j < object.dataLayout.dimensions[3]; j++) {
            //                        data[dataIndex++] = (byte) (rawY[rawIndex++] * byteFactor);
            //                    }
            //                }
            //            }
            //            return data; // TODO still have one extra line of zero data to delete because data is odd
            return null;
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
        return SOURCE.TIDE;
    }

    //    Tide() {}
    //
    //    Tide(float uf, float vf, Calendar now) {
    //        Record r = new Record();
    //        byte u = (byte) (uf*byteFactor);
    //        byte v = (byte) (vf*byteFactor);
    //        r.time = now.getTimeInMillis();
    //        r.time -= 6L * 30L * phys.day;
    //        r.u = new byte[] {u,u,u,u};
    //        r.v = new byte[] {v,v,v,v};
    //        r.stride = 2;
    //        r.left =   -190;
    //        r.right =  180;
    //        r.bottom =  -90;
    //        r.top =  90;
    //        r.fx =  (r.stride-1) / (r.right - r.left);
    //        r.fy =  (r.u.length/r.stride-1) / (r.top - r.bottom);
    //        data.add(r);
    //        r = new Record(r);
    //        r.time += 12 * 30 * phys.day;
    //        data.add(r);
    //    }

    //           gribFile.listRecords(System.out);
    //            System.exit(1);

    static final String fileFormat = Main.root+"tide"+ java.io.File.separator+"tide%4d%02d%02d.rec";

    public Tide(List<String> files, Water prevailing) {// List of tide files (without extension)
        final ArrayList<Record> dataAL = new ArrayList<>();
        for (String f: files) {
//                System.out.print("\r"+f);
            ReadFile(f,dataAL);
        }
        data = dataAL.toArray(new Record[0]);
        this.prevailing = prevailing;
    }

    Tide(Calendar now) {
        final ArrayList<Record> dataAL = new ArrayList<>();
        //        prevailing = new Current(now);
        if( ! Main.useWater)
            return;

        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        Calendar start, end, lastForecast, forecastDate, iterator;
        start = (Calendar) now.clone();
        forecastDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        forecastDate.setTimeInMillis(System.currentTimeMillis());
        lastForecast = (Calendar) forecastDate.clone();
        lastForecast.add(Calendar.HOUR, 5*24);
        end  = (Calendar) lastForecast.clone();
        end.add(Calendar.DAY_OF_WEEK, 29);
        iterator = (Calendar) start.clone();
        while (iterator.getTimeInMillis() < end.getTimeInMillis()) {
            //            if(iterator.getTimeInMillis() > lastForecast.getTimeInMillis()) {
            //                AddSPreviouSynodicMonth(iterator,dataAL);
            //            }
            //            else {
            sb.setLength(0);
            formatter.format(Locale.UK, fileFormat,
                    iterator.get(Calendar.YEAR),
                    iterator.get(Calendar.MONTH)+1,
                    iterator.get(Calendar.DAY_OF_MONTH));
            java.io.File file = new java.io.File(sb.toString());
            if(! file.exists()) {
                if( DownloadFile(iterator,file) )
                    ReadFile(file,dataAL);
                else  {
                    break;
                    //                        System.out.println("Trying to add previous month incorrectly");
                    //                        System.exit(-1);
                }
            }
            else{
                ReadFile(file,dataAL);
            }
            //            }
            iterator.add(Calendar.HOUR, 24);
        }
        if(dataAL.size() != 0)
            data = dataAL.toArray(new Record[0]);
        else {
            data = new Record[1];
            data[0] = new Record(0,0, now);
        }
    }

    boolean DownloadFile(Calendar now, java.io.File file) {
        //        String URLFormat = "ftp://jholmes1:Course2steer.@nrt.cmems-du.eu/Core/NORTHWESTSHELF_ANALYSIS_FORECAST_PHYS_004_001_b/MetO-NWS-PHYS-hi-CUR/metoffice_foam1_amm7_NWS_CUR_b%4d%02d%02d_hi%4d%02d%02d.nc";
        String URLFormat = "ftp://jholmes1:Course2steer.@nrt.cmems-du.eu/Core/NORTHWESTSHELF_ANALYSIS_FORECAST_PHY_004_013/MetO-NWS-PHY-hi-CUR/%4d/%02d/metoffice_foam1_amm15_NWS_CUR_b%4d%02d%02d_hi%4d%02d%02d.nc";
        //                                                ftp://nrt.cmems-du.eu/Core/NORTHWESTSHELF_ANALYSIS_FORECAST_PHY_004_013/MetO-NWS-PHY-hi-TMB-CUR/2020/04/metoffice_foam1_amm15_NWS_TMB_CUR_b20200405_hi20200410.nc
        String downloadedFile = Main.root+"tide"+ java.io.File.separator+"raw.nc";

        // 20220805 Code commented out as nc reader has the error "Version not supported for class type 6:16"
        // I think that they have changed the data format. Note the ftp downloader works OK.
        // Consider using https: version  this may be quicker
    /*        https://resources.marine.copernicus.eu/product-detail/NORTHWESTSHELF_ANALYSIS_FORECAST_PHY_004_013/DATA-ACCESS
                    MetO-NWS-PHY-hi-CUR
                    OpenDAP

    */
        //        StringBuilder sb = new StringBuilder();
        //        Formatter formatter = new Formatter(sb);
        //
        //        Calendar today, forecastDate;
        //        forecastDate = (Calendar) now.clone();
        //        forecastDate.add(Calendar.HOUR, 2*24);
        //        today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        //        today.setTimeInMillis(System.currentTimeMillis());
        //        if(forecastDate.getTimeInMillis() > today.getTimeInMillis())
        //            forecastDate = today;
        //        sb.setLength(0);
        //        formatter.format(Locale.UK, URLFormat,
        //                now.get(Calendar.YEAR),
        //                now.get(Calendar.MONTH)+1,
        //                forecastDate.get(Calendar.YEAR),
        //                forecastDate.get(Calendar.MONTH)+1,
        //                forecastDate.get(Calendar.DAY_OF_MONTH),
        //                now.get(Calendar.YEAR),
        //                now.get(Calendar.MONTH)+1,
        //                now.get(Calendar.DAY_OF_MONTH));
        //        String hidden = sb.toString().replace("jholmes1:Course2steer.","user:password");
        //        System.out.println(hidden); // Check to see if file has been created yet
        //        URL myUrl = null;
        //        try {
        //            myUrl = new URL(sb.toString()); //TODO hidden?
        //        } catch (MalformedURLException e) {
        //            System.out.println("Something went wrong with the date when getting the tides: "+ sb.toString());
        //            System.exit(1);
        //        }
        //        URLConnection conn;
        //        InputStream is = null;
        //        try {
        //            conn = myUrl.openConnection();
        //            is = conn.getInputStream();
        //        } catch (IOException e) {
        //            forecastDate.add(Calendar.HOUR, -24); // Get yesterday's forecast
        //            sb.setLength(0);
        //            formatter.format(Locale.UK, URLFormat,
        //                    now.get(Calendar.YEAR),
        //                    now.get(Calendar.MONTH)+1,
        //                    forecastDate.get(Calendar.YEAR),
        //                    forecastDate.get(Calendar.MONTH)+1,
        //                    forecastDate.get(Calendar.DAY_OF_MONTH),
        //                    now.get(Calendar.YEAR),
        //                    now.get(Calendar.MONTH)+1,
        //                    now.get(Calendar.DAY_OF_MONTH));
        //            System.out.println(sb.toString()); // Check to see if file has been created yet
        //            try {
        //                myUrl = new URL(sb.toString());
        //            } catch (MalformedURLException ee) {
        //                System.out.println("Something went wrong with the date when getting the tides: "+ sb.toString());
        //                System.exit(1);
        //            }
        //            try {
        //                conn = myUrl.openConnection();
        //                is = conn.getInputStream();
        //            } catch (IOException ee) {
        //                System.out.println("Could not find forecast: "+ sb.toString());
        //                return false;
        //            }
        //        }
        //        try {
        //            FileOutputStream fileOutputStream = new FileOutputStream(downloadedFile);
        //            byte[] dataBuffer = new byte[64*1024];
        //            int bytesRead;
        //            while ((bytesRead = is.read(dataBuffer, 0, 64*1024)) != -1) {
        //                fileOutputStream.write(dataBuffer, 0, bytesRead);
        //            }
        //            is.close();
        //            fileOutputStream.close();
        //        } catch (IOException e) {
        //            e.printStackTrace();
        //        }
        Record[] data = new Record[24];
        File HDfile = new File(downloadedFile);
        for (int i = 0; i < data.length; i++) {
            System.out.println(i);
            data[i] = new Record(HDfile, i);
        }
        WriteTideFile(data,file);
        return true;
    }

    void AddSPreviouSynodicMonth(Calendar day, ArrayList<Record> data) {
        long offset = (long) (2*29.531f * phys.msPerDay)- 2*phys.msPerHour; // Not sure why an hour offset is required to synopic time.
        ArrayList<Record> inputData = new ArrayList<>();
        Calendar previousMonth = (Calendar) day.clone();
        previousMonth.add(Calendar.SECOND, (int) (-offset/phys.msPerSecond));
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);

        sb.setLength(0);
        formatter.format(Locale.UK, fileFormat,
                previousMonth.get(Calendar.YEAR),
                previousMonth.get(Calendar.MONTH)+1,
                previousMonth.get(Calendar.DAY_OF_MONTH));
        java.io.File file = new java.io.File(sb.toString());
        if(! file.exists()) {
            if( DownloadFile(previousMonth,file) )
                ReadFile(file,inputData,4);
        }
        else{
            ReadFile(file,inputData,4);
        }
        for (Record r:inputData) {
            r.time += offset;
            if(data.size() >0)
                if(r.time < data.get(data.size()-1).time)
                    continue;
            data.add(r);
        }
    }

    void ReadFile(java.io.File file, ArrayList<Record> data ) {
        //        try {
        //            FileInputStream in = new FileInputStream(file);
        //            DataInputStream dis = new DataInputStream(in);
        //            int nRecords;
        //            nRecords = dis.readInt();
        //            for (int i = 0; i < nRecords; i++) {
        //                Record r = new Record();
        //                r.time = dis.readLong();
        //                r.stride = dis.readInt();
        //                r.top = dis.readFloat();
        //                r.left = dis.readFloat();
        //                r.bottom = dis.readFloat();
        //                r.right = dis.readFloat();
        //                r.fx = dis.readFloat();
        //                r.fy = dis.readFloat();
        //                int size = dis.readInt();
        //                r.u = new byte[size];
        //                r.v = new byte[size];
        //                dis.read(r.u);
        //                dis.read(r.v);
        //                data.add(r);
        //            }
        //            in.close();
        //        } catch (IOException e) {
        //            e.printStackTrace();
        //        }
        //        System.out.println("Read tide file: "+file.getName());
    }

    void ReadFile(java.io.File file, ArrayList<Record> data, int downSample ) {
        //        try {
        //            FileInputStream in = new FileInputStream(file);
        //            DataInputStream dis = new DataInputStream(in);
        //            int nRecords;
        //            nRecords = dis.readInt();
        //            for (int i = 0; i < nRecords; i++) {
        //                Record r = new Record();
        //                r.time = dis.readLong();
        //                r.stride = dis.readInt();
        //                r.top = dis.readFloat();
        //                r.left = dis.readFloat();
        //                r.bottom = dis.readFloat();
        //                r.right = dis.readFloat();
        //                r.fx = dis.readFloat();
        //                r.fy = dis.readFloat();
        //                int size = dis.readInt();
        //                r.u = new byte[size];
        //                r.v = new byte[size];
        //                dis.read(r.u);
        //                dis.read(r.v);
        //                Record d = new Record();
        //                d.time = r.time;
        //                d.stride = r.stride / downSample;
        //                if(d.stride * downSample != r.stride)
        //                    d.stride++;
        //                int ny = size/(r.stride-1);
        //                int d_ny = ny / downSample;
        //                if(d_ny * downSample != ny)
        //                    d_ny++;
        //                int d_size =  d.stride * d_ny;
        //                d.top = r.top;
        //                d.bottom = r.bottom;
        //                d.left = r.left;
        //                d.right = r.right;
        //                d.fx = (d.stride) / (d.right - d.left);
        //                d.fy = d_ny / (d.top - d.bottom);
        //                d.u = new byte[d_size];
        //                d.v = new byte[d_size];
        //                for (int y = 0;y < d_size/d.stride; y++){
        //                    for (int x = 0; x < d.stride; x++) {
        //                        d.u[x+y*d.stride] = r.u[x*downSample + y*r.stride*downSample];
        //                        d.v[x+y*d.stride] = r.v[x*downSample + y*r.stride*downSample];
        //                    }
        //                }
        //                data.add(d);
        //            }
        //            in.close();
        //        } catch (IOException e) {
        //            e.printStackTrace();
        //        }
        //        System.out.println("Read tide file: "+file.getName());
    }

    void ReadFile(String file, ArrayList<Record> data)  {
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
            Array uoData = uoVar.read(":,:,:");
            Array voData = voVar.read(":,:,:");
            Attribute uoUnits = uoVar.findAttribute("scale_factor");
            float scaleFactor = uoUnits.getNumericValue().floatValue();

            int i = 0;
            while (ti.hasNext()) {
                Record r = new Record();
                long nx = lons.getSize();
                long ny = lats.getSize();
                r.time =  referenceUnit.makeDate(  ti.getFloatNext()  ).getTime();
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

    void WriteTideFile(Record[] data, java.io.File file) {
        try {
            FileOutputStream out = new FileOutputStream(file);
            DataOutputStream dos = new DataOutputStream(out);
            dos.writeInt(data.length);
            for (Record r : data) {
                dos.writeLong(r.time);
                dos.writeInt(r.stride);
                dos.writeFloat(r.top);
                dos.writeFloat(r.left);
                dos.writeFloat(r.bottom);
                dos.writeFloat(r.right);
                dos.writeFloat(r.fx);
                dos.writeFloat(r.fy);
                dos.writeInt(r.u.length);
                //                dos.write(r.u);
                //                dos.write(r.v);
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


//    void draw(Graphics2D g, Mercator screen, long time) {
//        double dx = (screen.bottomRight.x - screen.topLeft.x)/20;
//        double dy = (screen.topLeft.y - screen.bottomRight.y)/20;
//        Vector2 v = new Vector2();
//        int index = 0;
//        g.setColor(Color.blue);
//        for (double i = screen.topLeft.x+dx; i < screen.bottomRight.x; i+= dx) {
//            for (double j = screen.bottomRight.y+dy; j < screen.topLeft.y; j+= dy) {
//                Vector2 p = screen.fromLatLngToPoint(j, i);
//                index = getValue(new Vector2(i,j),time,v);
//
//                if (index > 0) {
//                    float magnitude = (float) Math.sqrt(v.x*v.x + v.y*v.y);
//                    if (0 == magnitude)
//                        continue;
//                    if(magnitude <0.2) {
//                        g.setStroke(new BasicStroke(1));
//                        g.drawLine((int) p.x, (int) p.y,
//                                (int) (p.x + v.x * arrowSize/0.2), (int) (p.y - v.y * arrowSize/0.2));
//                    }
//                    else {
//                        g.setStroke(new BasicStroke((int)(magnitude*5)));
//                        g.drawLine((int) p.x, (int) p.y,
//                                (int) (p.x + v.x * arrowSize/magnitude), (int) (p.y - v.y * arrowSize/magnitude));
//                    }
////                        g.fillRect((int) p.x - 2, (int) p.y - 2, 4, 4);
//                }
//            }
//        }
//        g.setStroke(new BasicStroke(1));
//    }
}

// http://www.globalmarinenet.com/free-grib-file-downloads/


