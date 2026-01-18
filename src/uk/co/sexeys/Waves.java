package uk.co.sexeys;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.units.DateUnit;
import ucar.units.UnitException;
import uk.co.sexeys.jgribx.GribFile;
import uk.co.sexeys.jgribx.GribRecord;
import uk.co.sexeys.rendering.Colors;
import uk.co.sexeys.rendering.Projection;
import uk.co.sexeys.rendering.Renderable;
import uk.co.sexeys.rendering.Renderer;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;


public class Waves implements Renderable {
    private static final Logger logger = LoggerFactory.getLogger(Waves.class);
    private Record[] data;

    private final Vector2 a = new Vector2();
    private final Vector2 b = new Vector2();
    int lastIndex = 1;

    static class Record {
        float[] heights;
        long time;
        int stride;
        float top, left, bottom, right, fx, fy;

        Record(Record c) {
            heights = c.heights.clone();
            time = c.time;
            stride = c.stride;
            left = c.left;
            right = c.right;
            top = c.top;
            bottom = c.bottom;
            fx = c.fx;
            fy = c.fy;
        }

        Record() {
        }

        Record(float uf, Calendar now) {
            time = now.getTimeInMillis();
            time -= 6L * 30L * phys.msPerDay;
            heights = new float[]{uf, uf, uf, uf};
            stride = 2;
            left = -190;
            right = 180;
            bottom = -90; // somewhere we cannot get
            top = 90;
            fx = (stride - 1) / (right - left);
            fy = (heights.length / stride - 1) / (top - bottom);
        }


        float getValue(Vector2 p) { //TODO very slow in inn er loop
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
            final float dx1 = 1 - dx;
            float dy1 = 1 - dy;
            final float A = dx1 * dy1;
            float B = dx * dy1;
            float C = dx1 * dy;
            float D = dx * dy;
            return (heights[offset00] * A + heights[offset10] * B + heights[offset01] * C + heights[offset11] * D);
        }
    }

    private int findRecord(long t) {
        if (t > data[data.length - 1].time)
            return -1;
        if (t < data[0].time)
            return -1;
        if (t < data[lastIndex - 1].time)
            lastIndex = 1;
        while (data[lastIndex].time <= t)
            lastIndex++;
        return lastIndex;
    }


    public float getValue(Vector2 p, long t) { // interpolate over time
        if (findRecord(t) < 0) {
            return -1;
        }
        Record box = data[lastIndex]; // TODO need to generalise for different sized gribs
        if ((box.right < p.x) ||
                (box.left > p.x) ||
                (box.bottom > p.y) ||
                (box.top < p.y)) {
            return -1;
        }
        float a = data[lastIndex].getValue(p);
        float b = data[lastIndex - 1].getValue(p);
        final long ta = data[lastIndex].time;
        final long tb = data[lastIndex - 1].time;
        final double dt = tb - ta;
        final double df = t - ta;
        final double j = df / dt;
        final double A = (1 - j);
        return (float) (a * A + b * j);
    }

    public Waves(long time) { // No waves
        Calendar refTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        refTime.setTimeInMillis(time);
        Calendar endTime = (Calendar) refTime.clone();
        endTime.add(Calendar.YEAR, 1);
        final ArrayList<Record> dataAL = new ArrayList<>();
        dataAL.add(new Record(0,refTime));
        dataAL.add(new Record(0,endTime));

        data = dataAL.toArray(new Record[0]);
    }

    public Waves(List<String> files) {// List of wave files
        final ArrayList<Record> dataAL = new ArrayList<>();
        for (String f : files) {
            if (f.toLowerCase().endsWith(".grb") || f.toLowerCase().endsWith(".grib") || f.toLowerCase().endsWith(".grib2")) {
                ReadGribFile(f, dataAL);
            } else {
                ReadNetCDFFile(f, dataAL);
            }
        }
        // Sort by time
        dataAL.sort(Comparator.comparingLong(r -> r.time));
        data = dataAL.toArray(new Record[0]);
    }

    /**
     * Read wave data from GRIB file using jgribx library.
     * Looks for significant wave height parameter.
     */
    void ReadGribFile(String file, ArrayList<Record> data) {
        try {
            FileInputStream in = new FileInputStream(file);
            GribFile gribFile = new GribFile(in);
            List<Calendar> forecastTimes = gribFile.getForecastTimes();
            List<GribRecord> gribRecords = gribFile.getRecords();

            for (GribRecord gribRecord : gribRecords) {
                // Parameter 100 is typically significant wave height in GRIB2
                // Parameter 11 can also be wave height depending on the table
                int paramNum = gribRecord.getParameterNumber();
                String paramDesc = gribRecord.getParameterDescription();

                // Check for wave height parameters (varies by GRIB version/table)
                // Common codes: 100 (HTSGW), 140 (WVHGT), or descriptions containing "wave" and "height"
                boolean isWaveHeight = paramNum == 100 || paramNum == 140 ||
                    (paramDesc != null && paramDesc.toLowerCase().contains("wave") &&
                     paramDesc.toLowerCase().contains("height"));

                if (isWaveHeight) {
                    Record r = new Record();
                    r.time = gribRecord.getForecastTime().getTimeInMillis();
                    r.heights = gribRecord.getValues();
                    r.stride = gribRecord.getStride();
                    r.left = (float) gribRecord.getMinimumLongitude();
                    r.right = (float) gribRecord.getMaximumLongitude();
                    r.bottom = (float) gribRecord.getMinimumLatitude();
                    r.top = (float) gribRecord.getMaximumLatitude();

                    // Handle latitude ordering if needed
                    if (r.top < r.bottom) {
                        float[] temp = new float[r.heights.length];
                        int i = 0;
                        int rowEnd = r.heights.length;
                        int rowStart = rowEnd - r.stride;
                        do {
                            for (int j = rowStart; j < rowEnd; j++) {
                                temp[i++] = r.heights[j];
                            }
                            rowEnd -= r.stride;
                            rowStart -= r.stride;
                        } while (i < r.heights.length);
                        r.heights = temp;
                        float swap = r.top;
                        r.top = r.bottom;
                        r.bottom = swap;
                    }

                    r.fx = (r.stride - 1) / (r.right - r.left);
                    r.fy = (r.heights.length / r.stride - 1) / (r.top - r.bottom);
                    data.add(r);
                }
            }
            in.close();
        } catch (Exception e) {
            logger.error("Error reading GRIB wave file: {}", file, e);
        }
    }

    /**
     * Read wave data from NetCDF file (original implementation).
     */
    void ReadNetCDFFile(String file, ArrayList<Record> data) {
        NetcdfFile ncfile = null;
        try {
            ncfile = NetcdfFile.open(file);
            Variable timeVar = ncfile.findVariable("time");
            Variable latVar = ncfile.findVariable("lat");
            Variable lonVar = ncfile.findVariable("lon");
            Variable heightVar = ncfile.findVariable("Significant_height_of_combined_wind_waves_and_swell_surface");

            if (null == timeVar || null == latVar || null == lonVar || null == heightVar) return;
            Array times = timeVar.read();
            Attribute units = timeVar.findAttribute("units");
            String tunitsString = units.getStringValue();
            DateUnit referenceUnit = new DateUnit(tunitsString);
            IndexIterator ti = times.getIndexIterator();
            Array lats = latVar.read();
            Array lons = lonVar.read();
            Array heightData = heightVar.read(":,:,:");

            int i = 0;
            while (ti.hasNext()) {
                Record r = new Record();
                long nx = lons.getSize();
                long ny = lats.getSize();
                r.time = referenceUnit.makeDate(ti.getFloatNext()).getTime();
                r.stride = (int) nx;
                r.top = lats.getFloat((int) (lats.getSize() - 1));
                r.left = lons.getFloat(0);
                r.bottom = lats.getFloat(0);
                r.right = lons.getFloat((int) (lons.getSize() - 1));
                r.fx = (nx - 1) / (r.right - r.left);
                r.fy = (ny - 1) / (r.top - r.bottom);
                Array height = heightData.slice(0, i);

                r.heights = (float[]) height.copyTo1DJavaArray();
                data.add(r);
                i++;
            }
        } catch (IOException e) {
            logger.error("IO error reading NetCDF wave file", e);
        } catch (UnitException e) {
            logger.error("Unit error reading NetCDF wave file", e);
        } catch (InvalidRangeException e) {
            logger.error("Invalid range error reading NetCDF wave file", e);
        } finally {
            if (null != ncfile) try {
                ncfile.close();
            } catch (IOException e) {
                logger.error("Error closing NetCDF file", e);
            }
        }
    }

    void draw(Graphics2D g, Mercator screen, long time) {
        double dx = (screen.bottomRight.x - screen.topLeft.x) / 20;
        double dy = (screen.topLeft.y - screen.bottomRight.y) / 20;
        Vector2 v = new Vector2();
        g.setColor(Color.blue);
        for (double i = screen.topLeft.x + dx; i < screen.bottomRight.x; i += dx) {
            for (double j = screen.bottomRight.y + dy; j < screen.topLeft.y; j += dy) {
                Vector2 p = screen.fromLatLngToPoint(j, i);
                float height  =  getValue(new Vector2(i, j), time);
                g.setColor(Color.red);
                int rect = 30;
                if (height > Main.waveWarning) {
                    g.fillRect((int) (p.x - rect/2), (int) (p.y - rect/2), (int) rect, (int) rect);
                }
            }
        }
        g.setStroke(new BasicStroke(1));
    }

    @Override
    public void render(Renderer renderer, Projection projection, long time) {
        Vector2 topLeft = projection.getTopLeft();
        Vector2 bottomRight = projection.getBottomRight();

        double dx = (bottomRight.x - topLeft.x) / 20;
        double dy = (topLeft.y - bottomRight.y) / 20;

        renderer.setColor(Colors.BLUE);
        for (double i = topLeft.x + dx; i < bottomRight.x; i += dx) {
            for (double j = bottomRight.y + dy; j < topLeft.y; j += dy) {
                Vector2 p = projection.fromLatLngToPoint(j, i);
                float height = getValue(new Vector2((float) i, (float) j), time);
                renderer.setColor(Colors.RED);
                int rect = 30;
                if (height > Main.waveWarning) {
                    renderer.fillRect(p.x - rect / 2f, p.y - rect / 2f, rect, rect);
                }
            }
        }
        renderer.setSolidStroke(1);
    }
}
