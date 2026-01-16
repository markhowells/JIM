package uk.co.sexeys.wind;

import uk.co.sexeys.Main;
import uk.co.sexeys.Vector2;
import uk.co.sexeys.Vector3;
import uk.co.sexeys.jgribx.GribFile;
import uk.co.sexeys.jgribx.GribRecord;
import uk.co.sexeys.jgribx.NoValidGribException;
import uk.co.sexeys.jgribx.NotSupportedException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * Created by Jim on 14/06/2018.
 * http://www.tecepe.com.br/wiki/index.php?title=Winds_Tab
 */

public class VRWind extends Wind {
    private final List<Record> dataAL = new ArrayList<>();
    final private Record[] data;
    private final Vector3 a = new Vector3();
    private final Vector3 b = new Vector3();
    private static double[] acosLookup;
    Wind prevailing;

    int lastIndex = 1;

    public static void init() {
        acosLookup = new double[202];
        for (int i = 0; i < 201; i++) {
            acosLookup[i] = Math.acos(((double)i-100)/100) / Math.PI;
        }
        acosLookup[201] = 0;
    }

    public VRWind(Calendar fixTime, Wind prevailing) {
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if(fixTime.after(now))
            fixTime = now;
        this.prevailing = prevailing;
        int[][] forecastPlan =
                {   { 0, 1, 2, 3, 4, 5, 6, 7, 8,  9,10,11,12,13,14,15,16,17,18,19,20,21,22,23},  // 0: fix hour
                    {12,12,12,12,18,18,18,18,18, 18, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6,12,12},  // 1: current forecast run
                    {-1,-1,-1,-1,-1,-1,-1,-1,-1, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // 2: current forecast day delta

                    {18,18,18,18, 0, 0, 0, 0, 0,  0, 6, 6, 6, 6, 6, 6,12,12,12,12,12,12,18,18},  // 3: next forecast run
                    {-1,-1,-1,-1, 0, 0, 0, 0, 0,  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}}; // 4: next forecast day delta

        int[][] downloadPlan =
                {{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23},  // fix hour
                 {18,18,18,18,18,18, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6,12,12,12,12,12,12},  // must get forecast run
                 {-1,-1,-1,-1,-1,-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},  // must get  forecast day delta
                 {18,18,18,18, 0, 0, 0, 0, 0, 0, 6, 6, 6, 6, 6, 6,12,12,12,12,12,12,18,18},   // try forecast hour
                 {-1,-1,-1,-1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}}; // try forecast day delta

        int[] resolutionPlanTime = {0,     24,    48,    391};
        String[] resolutionPlan =  {"0p25","0p50","1p00","" };
        int[] resolutionPlanDelta ={3,     3,     3,     0  };


        String fileFormat = Main.root + "grib" + File.separator + "wind%4d%02d%02d%02d%s_f%03d.grb";
        String GFSFormat = Main.WindSource + "gfs.%4d%02d%02d/%02d/atmos/gfs.t%02dz.pgrb2.%s.f%03d";
        StringBuilder sb = new StringBuilder();
        Formatter formatter = new Formatter(sb);
        List<String>  runFile = new LinkedList<>();
        List<String>  runURL = new LinkedList<>();


        int fixHour = fixTime.get(Calendar.HOUR_OF_DAY);
        int run = downloadPlan[1][fixHour];
        Calendar date = (Calendar) fixTime.clone();
        date.set(Calendar.HOUR_OF_DAY, run);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.add(Calendar.DAY_OF_MONTH, downloadPlan[2][fixHour]);
        int hour = 0;
        int planIndex = 0;

// must get to perform a meaningful calculation
        while (hour <= 390) {  //Build up the gribs in order of time 390 = 128 (GFS runs) * 3hours +6hours from previous forecast
            while (hour >= resolutionPlanTime[planIndex + 1]) { //reduce resolution for later forecasts. Considered not needed
                planIndex += 1;
            }
            String resolution = resolutionPlan[planIndex];
// New GFS forecasts become available at 03:40, 09:40, 15:40 and 21:40
// But VR ignores the existence of the new forecasts until 06:00, 12:00 etc
// This means that VR uses the first 12 forecast hours from each new forecast before switching over to the next forecast
            int dayDelta, runHour;
            if (hour <= 12) {
                run = forecastPlan[1][fixHour];
                dayDelta = forecastPlan[2][fixHour];
                runHour = hour;
            }
// Then use the latest forecast
            else {
                run = (forecastPlan[3][fixHour]);
                dayDelta = forecastPlan[4][fixHour];
                runHour = hour - 6;
            }
            date = (Calendar) fixTime.clone();
            date.set(Calendar.HOUR_OF_DAY, run);
            date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0);
            date.set(Calendar.MILLISECOND, 0);
            date.add(Calendar.DAY_OF_MONTH, dayDelta);

            int year = date.get(Calendar.YEAR);
            int month = date.get(Calendar.MONTH) + 1;
            int day = date.get(Calendar.DATE);
            sb.setLength(0);
            formatter.format(Locale.UK, fileFormat, year, month, day, run, resolution, runHour);
            runFile.add(sb.toString());
            sb.setLength(0);
            formatter.format(Locale.UK, GFSFormat, year, month, day, run, run, resolution, runHour);
            runURL.add(sb.toString());

            hour += 3;
        }

        while (hour < 385) { // get the current forecast
            assert (planIndex + 1 < 24);
            while (hour >= resolutionPlanTime[planIndex + 1]) {
                planIndex += 1;
            }

            String resolution = resolutionPlan[planIndex];
            int year = date.get(Calendar.YEAR);
            int month = date.get(Calendar.MONTH) + 1;
            int day = date.get(Calendar.DATE);
            sb.setLength(0);
            formatter.format(Locale.UK, fileFormat, year, month, day, run, resolution, hour);
            runFile.add(sb.toString());
            sb.setLength(0);
            formatter.format(Locale.UK, GFSFormat, year, month, day, run, run, resolution, hour);
            runURL.add(sb.toString());
            int hourDelta = resolutionPlanDelta[planIndex];
            hour += hourDelta;
        }
        date = (Calendar) fixTime.clone(); // get the latest forecast
        run = downloadPlan[3][fixHour];
        date.set(Calendar.HOUR_OF_DAY, run);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.add(Calendar.DAY_OF_MONTH, downloadPlan[4][fixHour]);
        hour = 0;
        planIndex = 0;
        int runHour;
        while (hour < 391) {
            while (hour >= resolutionPlanTime[planIndex + 1]) {
                planIndex += 1;
            }
            if (planIndex != 0)
                runHour = hour - 6;
            else
                runHour =hour;
            String resolution = resolutionPlan[planIndex];
            int year = date.get(Calendar.YEAR);
            int month = date.get(Calendar.MONTH) + 1;
            int day = date.get(Calendar.DATE);
            sb.setLength(0);
            formatter.format(Locale.UK, fileFormat, year, month, day, run, resolution, runHour);
            runFile.add(sb.toString());
            sb.setLength(0);
            formatter.format(Locale.UK, GFSFormat, year, month, day, run, run, resolution, runHour);
            runURL.add(sb.toString());
            int hourDelta = resolutionPlanDelta[planIndex];
            hour += hourDelta;
        }

        final List<DownloadForecast> downloadForecastList = new ArrayList<>();
        int hits = 120; // This is limited by the number of hits the GFS will take before blocking you
        for (int i = 0; i < runFile.size(); i++) {
            if(runFile.get(i) != null) {
                File previouslyDownloadedForecast = new File(runFile.get(i));
                if (!previouslyDownloadedForecast.exists()) {
                    if (hits <= 0)
                        break;
                    DownloadForecast downloadForecast = new DownloadForecast(runURL.get(i), previouslyDownloadedForecast);
                    downloadForecastList.add(downloadForecast);
                    downloadForecast.start();
                    hits -= 2;
                }
            }
        }
        for (DownloadForecast d : downloadForecastList) {
            try {
                d.join(); // This blocks until all the download threads have finished
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Record record;
        hour = 0;
        planIndex = 0;
        System.out.println();
        while (hour <= 390) {  //Build up the gribs in order of time 390 = 128 (GFS runs) * 3hours +6hours from previous forecast
            while (hour >= resolutionPlanTime[planIndex + 1]) { //reduce resolution for later forecasts. Considered not needed
                planIndex += 1;
            }
            String resolution = resolutionPlan[planIndex];
// New GFS forecasts become available at 03:40, 09:40, 15:40 and 21:40
// But VR ignores the existence of the new forecasts until 06:00, 12:00 etc
// This means that VR uses the first 12 forecast hours from each new forecast before switching over to the next forecast
            int dayDelta;
            Wind.SOURCE source;
            if (hour <= 12) {
                run = forecastPlan[1][fixHour];
                dayDelta = forecastPlan[2][fixHour];
                runHour = hour;
                source = SOURCE.PREVIOUS;
            }
// Then use the latest forecast available
            else {
                run = (forecastPlan[3][fixHour]);
                dayDelta = forecastPlan[4][fixHour];
                runHour = hour - 6;
                source = SOURCE.LATEST;
            }
            date = (Calendar) fixTime.clone();
            date.set(Calendar.HOUR_OF_DAY, run);
            date.set(Calendar.MINUTE, 0);
            date.set(Calendar.SECOND, 0);
            date.set(Calendar.MILLISECOND, 0);
            date.add(Calendar.DAY_OF_MONTH, dayDelta);

            int year = date.get(Calendar.YEAR);
            int month = date.get(Calendar.MONTH) + 1;
            int day = date.get(Calendar.DATE);
            sb.setLength(0);
            formatter.format(Locale.UK, fileFormat, year, month, day, run, resolution, runHour);
            System.out.print(sb);
            record = this.readRecord(sb.toString());
            if (record != null) {
                record.source = source;
                dataAL.add(record);
                System.out.println();
            }
            else { // fallback on previous forecast as better than nothing
                run = forecastPlan[1][fixHour];
                dayDelta = forecastPlan[2][fixHour];
                runHour = hour;
                date = (Calendar) fixTime.clone();
                date.set(Calendar.HOUR_OF_DAY, run);
                date.set(Calendar.MINUTE, 0);
                date.set(Calendar.SECOND, 0);
                date.set(Calendar.MILLISECOND, 0);
                date.add(Calendar.DAY_OF_MONTH, dayDelta);

                year = date.get(Calendar.YEAR);
                month = date.get(Calendar.MONTH) + 1;
                day = date.get(Calendar.DATE);
                sb.setLength(0);
                formatter.format(Locale.UK, fileFormat, year, month, day, run, resolution, runHour);
                System.out.print("Trying " + sb);
                record = this.readRecord(sb.toString());
                if (record != null) {
                    record.source = SOURCE.PREVIOUS;
                    dataAL.add(record);
                    System.out.println();
                }
                else {
                    System.out.println("No more gribs found");
                    break;
                }
            }
            hour += 3;
        }
        this.AddPrevailing();
        data = dataAL.toArray(new Record[0]);
        BackgroundDownload b = new BackgroundDownload(runFile,runURL);
        b.start();
    }

    private class BackgroundDownload extends Thread  {
        // https://www.weather.gov/media/notification/pdf2/scn21-32nomad_changes.pdf
        List<String>  runFile;
        List<String>  runURL;
        BackgroundDownload( List<String>  runFile, List<String>  runURL) {
            this.runFile = runFile;
            this.runURL = runURL;
        }

        public void run() {
            while (true) {
                try {
                    sleep(65 *1000); // wait for GFS server to reset
                } catch (InterruptedException e) {
                    return;
                }
                final List<DownloadForecast> downloadForecastList = new ArrayList<>();
                int hits = 120;
                for (int i = 0; i < runFile.size(); i++) {
                    if(runFile.get(i) != null) {
                        File previouslyDownloadedForecast = new File(runFile.get(i));
                        if (!previouslyDownloadedForecast.exists()) {
                            if (hits <= 0)
                                break;
                            DownloadForecast downloadForecast = new DownloadForecast(runURL.get(i), previouslyDownloadedForecast);
                            downloadForecastList.add(downloadForecast);
                            downloadForecast.start();
                            hits -= 2;
                        }
                    }
                }
                if (downloadForecastList.size() == 0)
                    return;
                else
                    System.out.println("\nBackground download for "+downloadForecastList.size()+ " files.");
                for (DownloadForecast d : downloadForecastList) {
                    try {
                        d.join(); // This blocks until all the download threads have finished
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
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

    public SOURCE getValue(final Vector2 p, final long t, final Vector2 value) { // interpolate over time
        int i = findRecord(t);
        if (i < 1)
            return prevailing.getValue(p,t,value);
        if (-1 == data[i].getValue(p, a))
            return prevailing.getValue(p,t,value);
        if (-1 == data[i - 1].getValue(p, b))
            return prevailing.getValue(p,t,value);
        final long ta = data[i].time;
        final long tb = data[i - 1].time;
        final float dt = tb - ta;
        final float df = t - ta;
        final float j = df / dt;
        assert ( j <= 1.0 && j >= 0.0);
        final float A = (1 - j);

        final float interpolatedU = (float) (a.x * A + b.x * j);
        final float interpolatedV = (float) (a.y * A + b.y * j);

        value.x = interpolatedU;
        value.y = interpolatedV; // TODO possible speed up by keeping direction and strength separate

        return data[i].source;
    }

    Record readRecord(String file) {
        Record record = null;

        try {
            FileInputStream in = new FileInputStream(file);
            GribFile gribFile = new GribFile(in);
            if (gribFile.getRecords().size() != 2) {
                System.out.println("File does not have 2 records. Exiting.\n");
                System.exit(-1);
            }
            List<Calendar> forecastTimes = gribFile.getForecastTimes();
            if (forecastTimes.size() != 1) {
                System.out.println("Not a single forecast time. Exiting.\n");
                System.exit(-1);
            }
            record = new Record();
            record.time = forecastTimes.get(0).getTimeInMillis();
            List<GribRecord> gribRecords = gribFile.getRecords();

            for (GribRecord gribRecord : gribRecords) {
                switch (gribRecord.getParameterNumber()) {
                    case 2: //wind u coordinate
                        record.u = gribRecord.getValues();
                        break;
                    case 3: //wind v coordinate
                        record.v = gribRecord.getValues();
                        break;
                    default:
                        System.out.println("Parameter number not 2 (U) or 3 (V): " + gribRecord.getParameterNumber() + " Exiting.\n");
                        System.exit(-1);
                }
            }
            if (record.u.length != record.v.length) {
                System.out.println("U and V length not equal for some reason Exiting.\n");
                System.exit(-1);
            }
            GribRecord gribRecord = gribRecords.get(0);
            record.stride = gribRecord.getStride();
            record.left = (float) gribRecord.getMinimumLongitude();
            record.right = (float) gribRecord.getMaximumLongitude();
            record.bottom = (float) gribRecord.getMinimumLatitude();
            record.top = (float) gribRecord.getMaximumLatitude();
            record.fx = (record.stride - 1) / (record.right - record.left);
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
            if (record.right > 180) {
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
            if (Main.crossDateLine) {
                record.u = includeDateLine(record.u, record.stride);
                record.v = includeDateLine(record.v, record.stride);
                record.left -= 180;
                record.right += 180;
                record.stride *= 2;
            }
            record.w = new float[record.u.length];
            for (int j = 0; j < record.w.length; j++) {
                record.w[j] = (float) Math.sqrt(record.u[j] * record.u[j] + record.v[j] * record.v[j]);
            }
            record.fy = (record.u.length / record.stride - 1) / (record.top - record.bottom);
        } catch (FileNotFoundException e) {
            System.out.print(" not found on disk. ");
        } catch (IOException | NotSupportedException e) {
            e.printStackTrace();
        } catch ( NoValidGribException e) {
            try {
                System.out.print(" Corrupt file. Reload gribs. ");
                Files.deleteIfExists(Paths.get(file));
            } catch (IOException ee) {
                ee.printStackTrace();
            }
        }
        return record;
    }


    void AddPrevailing() {
        Record[] prevailing = new Record[24];
        String[] uLine;
        String[] vLine;
        String[] wLine;
        try {
            BufferedReader uBR = new BufferedReader(new FileReader(Main.root + "Prevailing" + File.separator + "U_TRENBERTH1.tsv"));
            BufferedReader vBR = new BufferedReader(new FileReader(Main.root + "Prevailing" + File.separator + "V_TRENBERTH1.tsv"));
            BufferedReader wBR = new BufferedReader(new FileReader(Main.root + "Prevailing" + File.separator + "UT_TRENBERTH1.tsv"));
            for (int i = 0; i < 12; i++) {
                prevailing[i] = new Record();
                prevailing[i].source = SOURCE.PREVAILING;
                prevailing[i].stride = 144;
                prevailing[i].bottom = -90;
                prevailing[i].top = 90;
                prevailing[i].left = -180;
                prevailing[i].right = 177.5f;
                prevailing[i].u = new float[73 * prevailing[i].stride];
                prevailing[i].v = new float[73 * prevailing[i].stride];
                prevailing[i].w = new float[73 * prevailing[i].stride];
                prevailing[i].fx = (prevailing[i].stride - 1) / (prevailing[i].right - prevailing[i].left);
                prevailing[i].fy = (prevailing[i].u.length / prevailing[i].stride - 1) / (prevailing[i].top - prevailing[i].bottom);
                uBR.readLine();
                vBR.readLine();
                wBR.readLine();
                for (int j = 0; j < 73; j++) {
                    uLine = uBR.readLine().split("\t");
                    vLine = vBR.readLine().split("\t");
                    wLine = wBR.readLine().split("\t");
                    int m = j * prevailing[i].stride;
                    for (int k = 0; k < prevailing[i].stride; k++) {
                        float u = Float.parseFloat(uLine[k + 1]);
                        float v = Float.parseFloat(vLine[k + 1]);
                        float w = Float.parseFloat(wLine[k + 1]);
                        float norm = (float) Math.sqrt(u * u + v * v);
                        if (norm != 0) {
                            prevailing[i].u[m] =  w * u / norm;
                            prevailing[i].v[m] =  w * v / norm;
                            prevailing[i].w[m++] = w;
                        } else {
                            prevailing[i].u[m] = 1;
                            prevailing[i].v[m] = 0;
                            prevailing[i].w[m++] = w;
                        }
                    }
                }
                if (Main.crossDateLine) {
                    prevailing[i].u = includeDateLine(prevailing[i].u, prevailing[i].stride);
                    prevailing[i].v = includeDateLine(prevailing[i].v, prevailing[i].stride);
                    prevailing[i].w = includeDateLine(prevailing[i].w, prevailing[i].stride);
                    prevailing[i].left -= 180;
                    prevailing[i].right += 180;
                    prevailing[i].stride *= 2;
                }
            }

            for (int i = 12; i < 24; i++) {
                prevailing[i] = prevailing[i - 12];
            }
            Calendar UTC = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            UTC.setTimeInMillis(dataAL.get(dataAL.size() - 1).time);
            UTC.add(Calendar.MONTH, 1);
            int year = UTC.get(Calendar.YEAR);
            int month = UTC.get(Calendar.MONTH);
            UTC.set(year, month, 1);
            for (int i = 0; i < 12; i++) {
                prevailing[month].time = UTC.getTimeInMillis();
                dataAL.add(prevailing[month]);
                month++;
                UTC.add(Calendar.MONTH, 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long GetTime(int record) {
        return data[record].time;
    }

    SOURCE getSOurce(int record){
        return data[record].source;
    }

    static class Record {
        float[] u, v, w;
        long time;
        int stride;
        float top, left, bottom, right, fx, fy;
        SOURCE source;

        Record() {
        }

        int getValue(Vector2 p, Vector3 out) { //TODO very slow in inner loop
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
            float dx = x - i;
            final float dy = y - j;
            float dx1 = 1 - dx; float dy1 = 1 - dy;
            final float A = dx1 * dy1; float B = dx * dy1; float C = dx1 * dy; float D = dx * dy;
            out.x = u[offset00] * A + u[offset10] * B + u[offset01] * C + u[offset11] * D;
            out.y = v[offset00] * A + v[offset10] * B + v[offset01] * C + v[offset11] * D;
            return 0;
        }

        static Record average(Record A, Record B) { //TODO no checks on similarity
            Record result = new Record();
            result.u = new float[A.u.length];
            result.v = new float[A.v.length];
            result.w = new float[A.w.length];
            result.time = (A.time+B.time)/2;
            result.stride = A.stride;
            result.top = A.top;
            result.left = A.left;
            result.bottom = A.bottom;
            result.right = A.right;
            result.fx = A.fx;
            result.fy = A.fy;

            for (int i = 0; i < A.u.length; i++) {
                result.u[i] =  (A.u[i] + B.u[i])/2;
                result.v[i] =  (A.v[i] + B.v[i])/2;
                result.w[i] =  (A.w[i] + B.w[i])/2;
            }
            return result;
        }
        static Record Weight(Record A, Record B, float mix) {
            assert (A.time == B.time);
            assert (A.stride == B.stride);
            assert (A.top == B.top);
            assert (A.left == B.left);
            assert (A.bottom == B.bottom);
            assert (A.right == B.right);
            assert (A.fx == B.fx);
            assert (A.fy == B.fy);
            Record result = new Record();
            result.u = new float[A.u.length];
            result.v = new float[A.v.length];
            result.w = new float[A.w.length];
            result.time = A.time;
            result.stride = A.stride;
            result.top = A.top;
            result.left = A.left;
            result.bottom = A.bottom;
            result.right = A.right;
            result.fx = A.fx;
            result.fy = A.fy;
            result.source =  SOURCE.WEIGHTED;

            for (int i = 0; i < A.u.length; i++) {
                result.u[i] =  A.u[i] * (1 - mix) + B.u[i] * mix;
                result.v[i] =  A.v[i] * (1 - mix) + B.v[i] * mix;
                result.w[i] =  A.w[i] * (1 - mix) + B.w[i] * mix;
            }
            return result;
        }

    }
}

// http://www.globalmarinenet.com/free-grib-file-downloads/

