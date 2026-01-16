package uk.co.sexeys.wind;

import uk.co.sexeys.Main;
import uk.co.sexeys.Vector2;
import uk.co.sexeys.jgribx.GribFile;
import uk.co.sexeys.jgribx.GribRecord;
import uk.co.sexeys.jgribx.NoValidGribException;
import uk.co.sexeys.jgribx.NotSupportedException;

import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Created by Jim on 14/06/2018.
 *
 */

public class C2SWind extends Wind{
    private final List<Record> dataAL = new ArrayList<>();
    final private Record[] data;

    static class Record {
        float[] u,v;
        long time;
        int stride;
        float top, left, bottom,right, fx, fy;

        Record() {}

        int getValue (Vector2 p, Vector2 out) { //TODO very slow in inner loop
            if (right < p.x)
                return -1;
            if (left > p.x)
                return -1;
            if (bottom > p.y)
                return -1;
            if (top < p.y)
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
            out.x = u[offset00] * A + u[offset10] * B + u[offset01] * C + u[offset11] * D;
            out.y = v[offset00] * A + v[offset10] * B + v[offset01] * C + v[offset11] * D;
            return 0;
        }
    }

    private int findRecord(long t,  int lastIndex) {
        try { // optimised for searches moving forward in time
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

    public int getValue(final Vector2 p, final long t, final Vector2 value, final int lastIndex) { // interpolate over time
        int i = findRecord(t, lastIndex);
        if(i < 1)
            return -1;
        if(-1 == data[i].getValue(p,a))
            return -1;
        if(-1 == data[i-1].getValue(p,b))
            return -1;
        final long ta = data[i].time;
        final long tb = data[i-1].time;
        final double dt = tb - ta;
        final double df = t - ta;
        final double j = df/dt;
        final double A = (1-j);

        value.x = (float) (a.x* A + b.x * j);
        value.y = (float) (a.y* A + b.y * j);
        return i;
    }

    private Record readRecord(String file) {
        Record record = null;
        try {
            FileInputStream in = new FileInputStream(file);
            GribFile gribFile = new GribFile(in);
            if(gribFile.getRecords().size() != 2) {
                System.out.println("File does not have 2 records. Exiting.\n");
                System.exit(-1);
            }
            List<Calendar> forecastTimes = gribFile.getForecastTimes();
            if(forecastTimes.size() != 1) {
                System.out.println("Not a single forecast time. Exiting.\n");
                System.exit(-1);
            }
            record = new Record();
            record.time = forecastTimes.get(0).getTimeInMillis();
            List<GribRecord> gribRecords = gribFile.getRecords();

            for (GribRecord gribRecord:gribRecords) {
                switch(gribRecord.getParameterNumber()) {
                    case 2: //wind u coordinate
                        record.u = gribRecord.getValues();
                        break;
                    case 3: //wind v coordinate
                        record.v = gribRecord.getValues();
                        break;
                    default:
                        System.out.println("Parameter number not 2 (U) or 3 (V): "+ gribRecord.getParameterNumber()+" Exiting.\n");
                        System.exit(-1);
                }
            }
            if(record.u.length != record.v.length) {
                System.out.println("U and V length not equal for some reason Exiting.\n");
                System.exit(-1);
            }
            GribRecord gribRecord = gribRecords.get(0);
            record.stride = gribRecord.getStride();
            record.left = (float) gribRecord.getMinimumLongitude();
            record.right = (float) gribRecord.getMaximumLongitude();
            record.bottom = (float) gribRecord.getMinimumLatitude();
            record.top = (float) gribRecord.getMaximumLatitude();
            record.fx  = (record.stride-1) / (record.right - record.left);
            if(record.top < record.bottom) { //reorder in memory
                float[] temp = new float[record.u.length];
                int i = 0;
                int rowEnd = record.u.length;
                int rowStart = rowEnd - record.stride;
                do{
                    for (int j = rowStart; j < rowEnd; j++) {
                        temp[i++] = record.u[j];
                    }
                    rowEnd -= record.stride;
                    rowStart -= record.stride;
                } while ( i < record.u.length);
                record.u = temp;
                temp = new float[record.v.length];
                i = 0;
                rowEnd = record.v.length;
                rowStart = rowEnd - record.stride;
                do{
                    for (int j = rowStart; j < rowEnd; j++) {
                        temp[i++] = record.v[j];
                    }
                    rowEnd -= record.stride;
                    rowStart -= record.stride;
                } while ( i < record.v.length);
                record.v = temp;
                float tempTop = record.top; record.top = record.bottom; record.bottom = tempTop;
            }
            if(record.right > 180) { //reorder in memory
                float[] temp = new float[record.u.length];
                int i = 0;
                int rowEnd = record.stride;
                int midRow = record.stride/2;
                int rowStart = 0;
                do{
                    for (int j = midRow; j < rowEnd; j++) {
                        temp[i++] = record.u[j];
                    }
                    for (int j = rowStart; j < midRow; j++) {
                        temp[i++] = record.u[j];
                    }
                    rowEnd += record.stride;
                    midRow += record.stride;
                    rowStart += record.stride;
                } while ( i < temp.length);
                record.u = temp;
                temp = new float[record.v.length];
                i = 0;
                rowEnd = record.stride;
                midRow = record.stride/2;
                rowStart = 0;
                do{
                    for (int j = midRow; j < rowEnd; j++) {
                        temp[i++] = record.v[j];
                    }
                    for (int j = rowStart; j < midRow; j++) {
                        temp[i++] = record.v[j];
                    }
                    rowEnd += record.stride;
                    midRow += record.stride;
                    rowStart += record.stride;
                } while ( i < temp.length);
                record.v = temp;
                record.left = -180; record.right = 180;
            }
            if(Main.crossDateLine) {
                record.u = includeDateLine(record.u,record.stride);
                record.v = includeDateLine(record.v,record.stride);
                record.left -= 180; record.right += 180;
                record.stride *= 2;
            }
            record.fy = (record.u.length/record.stride-1) / (record.top - record.bottom);
        } catch (FileNotFoundException e) {
            System.out.println(" not found on disk. Skipping...");
        } catch (IOException | NotSupportedException | NoValidGribException e) {
            e.printStackTrace();
        }
        return record;
    }

    void AddPrevailing() {
        Record[] prevailing = new Record[24];
        String[] uLine;
        String[] vLine;
        String[] wLine;
        try {
            BufferedReader uBR = new BufferedReader(new FileReader(Main.root+"Prevailing"+ File.separator+"U_TRENBERTH1.tsv"));
            BufferedReader vBR = new BufferedReader(new FileReader(Main.root+"Prevailing"+ File.separator+"V_TRENBERTH1.tsv"));
            BufferedReader wBR = new BufferedReader(new FileReader(Main.root+"Prevailing"+ File.separator+"UT_TRENBERTH1.tsv"));
            for (int i = 0; i < 12; i++) {
                prevailing[i] = new Record();
                prevailing[i].stride = 144;
                prevailing[i].bottom = -90;
                prevailing[i].top = 90;
                prevailing[i].left = -180;
                prevailing[i].right = 177.5f;
                prevailing[i].u = new float[73*prevailing[i].stride];
                prevailing[i].v = new float[73*prevailing[i].stride];
                prevailing[i].fx  = (prevailing[i].stride-1) / (prevailing[i].right - prevailing[i].left);
                prevailing[i].fy = (prevailing[i].u.length/prevailing[i].stride-1) / (prevailing[i].top - prevailing[i].bottom);
                uBR.readLine();
                vBR.readLine();
                wBR.readLine();
                for (int j = 0; j < 73; j++) {
                    uLine = uBR.readLine().split("\t");
                    vLine = vBR.readLine().split("\t");
                    wLine = wBR.readLine().split("\t");
                    int m = j*prevailing[i].stride;
                    for (int k = 0; k < prevailing[i].stride; k++) {
                        float u = Float.parseFloat(uLine[k+1]);
                        float v = Float.parseFloat(vLine[k+1]);
                        float w = Float.parseFloat(wLine[k+1]);
                        float norm = (float) Math.sqrt(u*u + v*v);
                        if(norm != 0) {
                            prevailing[i].u[m] = w * u/norm;
                            prevailing[i].v[m] = w * v/norm;
                        }
                        else {
                            prevailing[i].u[m] = 0;
                            prevailing[i].v[m] = 0;
                        }
                    }
                }
                if(Main.crossDateLine) {
                    prevailing[i].u = includeDateLine(prevailing[i].u,prevailing[i].stride);
                    prevailing[i].v = includeDateLine(prevailing[i].v,prevailing[i].stride);
                    prevailing[i].left -= 180; prevailing[i].right += 180;
                    prevailing[i].stride *= 2;
                }
            }

            for (int i = 12; i < 24; i++) {
                prevailing[i] = prevailing[i-12];
            }
            Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            UTC.setTimeInMillis(dataAL.get(dataAL.size()-1).time);
            UTC.add(Calendar.MONTH,1);
            int year = UTC.get(Calendar.YEAR);
            int month = UTC.get(Calendar.MONTH);
            UTC.set(year,month,1);
            for (int i = 0; i < 12; i++) {
                prevailing[month].time = UTC.getTimeInMillis();
                dataAL.add(prevailing[month]);
                month++;
                UTC.add(Calendar.MONTH,1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public C2SWind(Calendar requestedTime) {
        Calendar fixTime;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if(requestedTime.before(now))
            fixTime = requestedTime;
        else
            fixTime = now;
        String fileFormat = Main.root + "grib" + File.separator + "wind%4d%02d%02d%02d_f%03d.grb";
        String GFSFormat = Main.WindSource + "gfs.%4d%02d%02d/%02d/atmos/gfs.t%02dz.pgrb2." + Main.WindResolution + ".f%03d";
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        List<String>  runFile = new LinkedList<>();
        List<String>  runURL = new LinkedList<>();

        Calendar testDate = (Calendar) fixTime.clone();
        testDate.set(Calendar.HOUR_OF_DAY, 5);
        testDate.set(Calendar.MINUTE, 30);
        testDate.set(Calendar.SECOND, 0);
        testDate.set(Calendar.MILLISECOND, 0);

        Calendar baseRun = (Calendar) testDate.clone();
        baseRun.set(Calendar.MINUTE, 0);
        if (fixTime.before(testDate)) {
            baseRun.add(Calendar.DAY_OF_MONTH, -1);
            baseRun.set(Calendar.HOUR_OF_DAY, 18);
        } else {
            testDate.add(Calendar.HOUR_OF_DAY, 6);
            if (fixTime.before(testDate)) {
                baseRun.set(Calendar.HOUR_OF_DAY, 0);
            } else {
                testDate.add(Calendar.HOUR_OF_DAY, 6);
                if (fixTime.before(testDate)) {
                    baseRun.set(Calendar.HOUR_OF_DAY, 6);
                } else {
                    testDate.add(Calendar.HOUR_OF_DAY, 6);
                    if (fixTime.before(testDate)) {
                        baseRun.set(Calendar.HOUR_OF_DAY, 12);
                    } else {
                        baseRun.set(Calendar.HOUR_OF_DAY, 18);
                    }
                }
            }
        }

        for (int forecast = 0; forecast <= 384; forecast +=3) {
            int runForecast = forecast;
            Calendar run = (Calendar) baseRun.clone();
            Calendar productionDate = (Calendar) baseRun.clone();
            productionDate.add(Calendar.MINUTE,(int)(3.6*60)); // 03:36 by experiment
            productionDate.add(Calendar.SECOND,43*forecast/3); // 43 by experiment
            int hour = run.get(Calendar.HOUR_OF_DAY);
            int year = run.get(Calendar.YEAR);
            int month = run.get(Calendar.MONTH) + 1;
            int day = run.get(Calendar.DATE);
            sb.setLength(0);
            formatter.format(Locale.UK, fileFormat, year, month, day, hour, runForecast);
            runFile.add(sb.toString());
            sb.setLength(0);
            formatter.format(Locale.UK, GFSFormat, year, month, day, hour, hour, runForecast);
            runURL.add(sb.toString());
        }
        final List<Wind.DownloadForecast> downloadForecastList = new ArrayList<>();
        int hits = 120;
        for (int i = 0; i < runFile.size(); i++) {
            File previouslyDownloadedForecast = new File(runFile.get(i));
            if (!previouslyDownloadedForecast.exists()) {
                if(hits <=0 )
                    break;
                Wind.DownloadForecast downloadForecast = new Wind.DownloadForecast(runURL.get(i), previouslyDownloadedForecast);
                downloadForecastList.add(downloadForecast);
                downloadForecast.start();
                hits -= 2;
            }
        }
        for (Wind.DownloadForecast d : downloadForecastList) {
            try {
                d.join(); // This blocks until all the download threads have finished
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        for (int i = 0; i < runFile.size(); i++) {
            System.out.print("\r"+runFile.get(i));
            C2SWind.Record recordA = this.readRecord(runFile.get(i));
            if(recordA == null)
                continue;
            dataAL.add(recordA);
        }
        this.AddPrevailing();
        data = dataAL.toArray(new C2SWind.Record[0]);
    }
}

// http://www.globalmarinenet.com/free-grib-file-downloads/
