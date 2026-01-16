package uk.co.sexeys.wind;

import uk.co.sexeys.Mercator;
import uk.co.sexeys.Vector2;
import uk.co.sexeys.water.Water;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public abstract class Wind {
    static final int arrowSize = 2;
    public enum SOURCE {
        UNKNOWN,
        LATEST,
        PREVIOUS,
        PREVAILING,
        WEIGHTED,
        FIXED
    }

    public static String Source(SOURCE s){
        if ( s == SOURCE.UNKNOWN )
            return "UNKNOWN";
        if ( s == SOURCE.LATEST )
            return "LATEST";
        if ( s == SOURCE.PREVIOUS )
            return "PREVIOUS";
        if ( s == SOURCE.WEIGHTED )
            return "WEIGHTED";
        if ( s == SOURCE.PREVAILING )
            return "PREVAILING";
        return "FIXED";
    }

    Wind() {}

    public SOURCE getValue(final Vector2 p, final long t, final Vector2 value) {
        value.x = value.y = 2; // Some test values
        return SOURCE.FIXED;
    }

    SOURCE getSource(int index){
        return SOURCE.UNKNOWN;
    }

    static float[] includeDateLine (float[] d, int stride) {
        float[] temp = new float[d.length*2];
        int i = 0;
        int rowEnd = stride;
        int midRow = stride/2;
        int rowStart = 0;
        do{
            for (int j = midRow; j < rowEnd; j++) {
                temp[i++] = d[j];
            }
            for (int j = rowStart; j < rowEnd; j++) {
                temp[i++] = d[j];
            }
            for (int j = rowStart; j < midRow; j++) {
                temp[i++] = d[j];
            }
            rowEnd += stride;
            midRow += stride;
            rowStart += stride;
        } while ( i < temp.length);
        return temp;
    }

    class DownloadForecast extends Thread {
        final String url;
        final File file;

        DownloadForecast(String url, File file) {
            this.url = url;
            this.file = file;
        }

        public void run() {
            try {
                System.out.print("\r"+url);
                String indexURL = url + ".idx";
                URL myUrl = new URL(indexURL);
                HttpsURLConnection conn = (HttpsURLConnection) myUrl.openConnection();
                InputStream is = conn.getInputStream();
                byte[] dataBuffer = new byte[100000];
                int bytesRead;
                StringBuilder idx = new StringBuilder();
                while ((bytesRead = is.read(dataBuffer)) != -1) {
                    idx.append(new String(dataBuffer, 0, bytesRead));
                }
                is.close();
                String[] idxLines = idx.toString().split("\n");
                int i = 0;
                while (!idxLines[i].contains("10 m above ground")) {
                    i++;
                }
                String[] idxFields = idxLines[i].split(":");
                String chunkStart = idxFields[1];
                i += 2;
                idxFields = idxLines[i].split(":");
                int chunkEnd = Integer.parseInt(idxFields[1]) - 1;

                myUrl = new URL(url);
                conn = (HttpsURLConnection) myUrl.openConnection();
                conn.setRequestProperty("Range", "Bytes=" + chunkStart + "-" + chunkEnd);

                is = conn.getInputStream();
                final FileOutputStream fileOutputStream = new FileOutputStream(file);
                dataBuffer = new byte[1024];
                while ((bytesRead = is.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
                is.close();
                fileOutputStream.close();
            } catch (IOException | java.lang.ArrayIndexOutOfBoundsException e ) {
                System.out.println("file: "+url+" not found on server. Skipping...");
            }
        }
    }

    public long GetLastValidForecast() {
        return -1;
    }

    public void draw(Graphics2D g, Mercator screen, long time) {
        double dx = (screen.bottomRight.x - screen.topLeft.x)/20;
        double dy = (screen.topLeft.y - screen.bottomRight.y)/20;
        Vector2 v = new Vector2();
        g.setColor(Color.black);
        for (double i = screen.topLeft.x+dx; i < screen.bottomRight.x; i+= dx) {
            for (double j = screen.bottomRight.y+dy; j < screen.topLeft.y; j+= dy) {
                Vector2 p = screen.fromLatLngToPoint(j, i);
                SOURCE source;
                try {
                    source = getValue(new Vector2(i,j),time,v);
                } catch (Exception e) {
                    continue;
                }
                if (source == SOURCE.PREVIOUS) {
                    g.setColor(Color.blue);
                } else if (source == SOURCE.LATEST) {
                    g.setColor(Color.black);
                } else if (source == SOURCE.WEIGHTED) {
                    g.setColor(Color.darkGray);
                } else if (source == SOURCE.PREVAILING) {
                    g.setColor(Color.green);
                } else {
                    g.setColor(Color.red);
                }
                g.drawLine((int) p.x, (int) p.y,
                        (int) (p.x + v.x* arrowSize), (int) (p.y - v.y* arrowSize));
                g.fillRect((int) p.x-1, (int) p.y-1, 2, 2);
            }
        }
    }

    public long GetTime(int record) {
        return 0;
    }
}