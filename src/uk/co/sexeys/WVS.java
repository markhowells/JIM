package uk.co.sexeys;

import uk.co.sexeys.rendering.Colors;
import uk.co.sexeys.rendering.Projection;
import uk.co.sexeys.rendering.Renderable;
import uk.co.sexeys.rendering.Renderer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Jim on 25/02/2018.
 * https://data.admiralty.co.uk/portal/apps/sites/#/marine-data-portal
 */
public class WVS implements Renderable {
    private byte[] buffer = null;

    WVS(double s) {
        try {
            scale(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ReadObstructions();
    }


    private class DataFile {
        String fileName;
        double resolution;

        DataFile(String f, double r) {
            fileName = f;
            resolution = r;
        }
    }

    private DataFile[] dataFiles = {
            new DataFile("wvsfull.dat", 100000),
            new DataFile("wvs250k.dat", 250000),
            new DataFile("wvs1.dat", 1000000),
            new DataFile("wvs3.dat", 3000000),
            new DataFile("wvs12.dat", 12000000),
            new DataFile("wvs43.dat", 43000000)
    };

    GridPoint GetGridPoint(int lat, int lon) {
        return new GridPoint(lat, lon);
    }


    public class GridPoint {
        public List<Vector2> data = new ArrayList<Vector2>();
        public List<Integer> segray = new ArrayList<Integer>();

        GridPoint (int lat, int lon) {
            wvsrtv(lat, lon);
        }
        GridPoint() {};
        private final int PHYSIZ = 3072;
        private int rindex, addr, lperp;

        private int nxtrec(int curpos) {
            /*  Compute the index number for the next logical record in the chain.   */

            rindex = (buffer[curpos + 1 + addr] & 0xff) * 65536 + (buffer[curpos + 2 + addr] & 0xff) * 256 + (buffer[curpos + 3 + addr] & 0xff);
            addr = ((rindex - 1) / lperp) * PHYSIZ;

    /*  Set the previous physical address to the current one, and compute
        the current byte position for the new record.  */

            curpos = ((rindex - 1) % lperp) * logrec;
            return curpos;
        }


        private int movpos(int curpos) {
            curpos = curpos + 2;

            /*  If we hit the end of the physical record, get the next one.  */

            if (curpos % logrec == fulrec)
                curpos = nxtrec(curpos);
            return curpos;
        }


        private int test_bit(int offset, int bitpos) {
            return (buffer[offset] & (0x01 << bitpos));
        }


        private int celchk(int offset) {
            int caddr;
            int ndxpos;
            int bytpos, bitpos, chk;


            /*  Compute the physical address of the 'rindex' cell bit. */

            caddr = (((rindex + logrec * 8) - (offset + 1)) /
                    (PHYSIZ * 8)) * PHYSIZ;

            /*  Compute the 'rindex' position within the physical record.  */

            ndxpos = ((rindex + logrec * 8) - (offset + 1)) % (PHYSIZ * 8);


            /*  Compute the byte and bit positions.  */

            bytpos = ndxpos / 8;
            bitpos = 7 - ndxpos % 8;

            /*  Test the 'rindex' bit and return.  */

            chk = test_bit(caddr + bytpos, bitpos);
            return (chk);
        }


        private void build_seg(float dlat, float dlon, boolean cont, int lnbias) {
            /*  If this is beginning of a new segment, close the last segment.  */
            Vector2 point = new Vector2();
            point.y = dlat - 90.0f ;
            point.x = dlon - 180.0f + lnbias;
            if (!cont) {
                /*  Make sure there are at least two points in the arrays.  */
                if (npts > 0) {
                    // TODO This is the place to test for line segments
                    if(npts == 1) {
                        Vector2 firstPoint = data.get(data.size() -1);
                        Vector2 lastPoint = data.get(data.size() -2);

                        if(lastPoint.y != firstPoint.y || lastPoint.x != firstPoint.x) {
                            segray.add(npts + 1);
                        }
                        else {
                            if (data.size() > 0) data.remove(data.size() - 1);
                            if (data.size() > 0) data.remove(data.size() - 1);
                        }
                    }
                    else
                        segray.add(npts + 1);
                } else {
                    /*  Back up a spot if we got a single point segment.  */
                    if (data.size() > 0) data.remove(data.size() - 1);
                }

                npts = -1;
            }

            /*  Store point.  */
            ++npts;
            data.add(point);
        }


        private int logrec, fulrec;
        private int npts;

        private int wvsrtv(int latd, int lond) {

            int offset, /*version,*/ slatf, nlatf,
                    wlonf, elonf, widef, ioff;
            int curpos, size;
            float todeg;

            int col, segcnt, lat, lon,
                    cnt, latsec, lonsec, lnbias;
            boolean eflag, cont;
            int latoff, lonoff, conbyt, lats, lons;
            float dlat, dlon;

            /*  Initialize variables, open file and read first record.  */

            npts = -1;
            lats = 0;
            lons = 0;
            lnbias = 0;


            /*  Get the file info (we actually looked at sub-setting these).  */

            logrec = buffer[3] & 0xff;
            fulrec = logrec - 4;
            /* version = bytbuf[4]; */
            ioff = buffer[5];
            slatf = (buffer[6] & 0xff) * 256 + (buffer[7] & 0xff);
            nlatf = (buffer[8] & 0xff) * 256 + (buffer[9] & 0xff);
            wlonf = (buffer[10] & 0xff) * 256 + (buffer[11] & 0xff);
            elonf = (buffer[12] & 0xff) * 256 + (buffer[13] & 0xff);
            if (elonf < wlonf) elonf += 360;
            if (slatf + nlatf + wlonf + elonf == 0) {
                nlatf = 180;
                elonf = 360;
            }
            widef = elonf - wlonf;
            size = (nlatf - slatf) * widef;
            todeg = 3600.0f * ioff;
            offset = (size - 1) / (logrec * 8) + 2;
            lperp = PHYSIZ / logrec;


            /*  Check for longitude entered in 0-360 world.  */

            if (lond > 180) lnbias = 360;
            if (lond < -180) lnbias = -360;


            /*  Compute integer values for retrieval and adjust if necessary. */

            lat = latd + 90;
            lon = lond;


            col = lon % 360;
            if (col < -180) col = col + 360;
            if (col >= 180) col = col - 360;
            col += 180;
            if (col < wlonf) col += 360;
            rindex = (lat - slatf) * widef + (col - wlonf) + 1 + offset;


            /*  Check for cell outside of file area or no data.  */
            if(obstructions[latd+90][col] != null) {
                data.addAll(obstructions[latd+90][col].data);
                data.add(new Vector2());
                segray.addAll(obstructions[latd+90][col].segray);
            }
            if (lat < slatf || lat >= nlatf || col < wlonf || col >= elonf || (celchk(offset)) == 0)
                return (0);


    /*  Compute physical record address, read record and save as previous
        address.  */

            eflag = false;
            addr = ((rindex - 1) / lperp) * PHYSIZ;

            /*  Compute byte position within physical record.  */

            curpos = ((rindex - 1) % lperp) * logrec;

            /*  If not at end of segment, process the record.  */

            while (!eflag) {
        /*  Get first two bytes of header and break out count and
            continuation bit.  */

                segcnt = ((buffer[curpos + addr] & 0xff) % 128) * 4 + (buffer[curpos + addr + 1] & 0xff) / 64 + 1;
                cont = test_bit(curpos + addr, 7) != 0; // buffer[curpos+addr] / 128;

        /*  If this is a continuation record get offsets from the second
            byte.  */

                if (cont) {
                    latoff = (((buffer[curpos + addr + 1] & 0xff) % 64) / 8) * 65536;
                    lonoff = ((buffer[curpos + addr + 1] & 0xff) % 8) * 65536;
                }


                /*  If this is an initial record set the offsets to zero.  */

                else {
                    latoff = 0;
                    lonoff = 0;
                    // Additional in WVS1993 decoder
                    // short rank = bytbuf[curpos+1]%64;
                    // fprintf (stderr, "rank = %d\n", rank);
                }


        /*  Update the current byte position and get a new record if
            necessary.  */

                curpos = movpos(curpos);


                /*  Compute the rest of the latitude offset.  */

                latoff += (buffer[curpos + addr] & 0xff) * 256 + (buffer[curpos + 1 + addr] & 0xff);

                curpos = movpos(curpos);


                /*  Compute the rest of the longitude offset.  */

                lonoff += (buffer[curpos + addr] & 0xff) * 256 + (buffer[curpos + 1 + addr] & 0xff);


        /*  If this is a continuation record, bias the lat and lon offsets
            and compute the position.  */

                if (cont) {
                    latoff -= 262144;
                    lonoff -= 262144;
                    lats += latoff;
                    lons += lonoff;
                }


                /*  Else, compute the position.             */

                else {
                    lats = (int) (lat * todeg) + latoff;
                    lons = (int) (col * todeg) + lonoff;
                }


                /*  Set the position.  */

                dlat = (float) lats / todeg;
                dlon = (float) lons / todeg;


                /*  Update the current byte position.       */

                curpos += 2;


                /*  Get the continuation pointer.           */

                conbyt = ((rindex - 1) % lperp) * logrec + fulrec;


        /*  If there is no continuation pointer or the byte position is
            not at the position pointed to by the continuation pointer,
            process the segment data.  */

                if (buffer[conbyt + addr] == 0 || (curpos + 1) % logrec <= (buffer[conbyt + addr] & 0xff)) {
            /*  If at the end of the logical record, get the next record
                in the chain.  */

                    if (curpos % logrec == fulrec && buffer[conbyt + addr] == 0)
                        curpos = nxtrec(curpos);

                    build_seg(dlat, dlon, cont, lnbias);
                    cont = true;

            /*  If the end of the segment has been reached, set the end
                flag.  */

                    if ((curpos + 1) % logrec == (buffer[conbyt + addr] & 0xff)) eflag = true;


                    /*  Process the segment.                */

                    for (cnt = 2; cnt <= segcnt; ++cnt) {

                        /*  Compute the position from the delta record.  */

                        latsec = (buffer[curpos + addr] & 0xff) - 128;
                        lats += latsec;
                        dlat = (float) lats / todeg;
                        lonsec = (buffer[curpos + addr + 1] & 0xff) - 128;
                        lons += lonsec;
                        dlon = (float) lons / todeg;

                        build_seg(dlat, dlon, cont, lnbias);

                        curpos += 2;

                        conbyt = ((rindex - 1) % lperp) * logrec + fulrec;


                /*  If the end of the segment has been reached, set the
                    end flag and break out of for loop.  */

                        if ((curpos + 1) % logrec == (buffer[conbyt + addr] & 0xff)) {
                            eflag = true;
                            break;
                        } else {
                            if (curpos % logrec == fulrec)
                                curpos = nxtrec(curpos);
                        }
                    }
                }

                /*  Break out of while loop if at the end of the segment.  */

                else {
                    break;
                }                           /*  end if      */
            }                               /*  end while   */


            /*  Call the build_seg routine to flush the buffers.  */
            build_seg(999.0f, 999.0f, false, lnbias);
            return 1;
        }
    }

    private DataFile currentFile = null;

    void scale(double scale) throws IOException {
        DataFile file = null;
        for (DataFile f : dataFiles) {
            file = f;
            if (f.resolution > scale)
                break;
        }
        if (file == null)
            throw new IOException("scale <= 0 ?");
        if (file != currentFile) {
            File f = new File(Main.root+ "WVS" + File.separator + file.fileName);
            InputStream is = new FileInputStream(f);
            buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            currentFile = file;
        }
        world = new GridPoint[181][720];
    }

    Vector2 p1,p2;
    int minLat,maxLat,minLon,maxLon;
    Vector2 d1,d2;
    Vector2 p;
    Iterator<Vector2> dataI;
    private GridPoint[][] world;
    private int worldOffset = 360;


    boolean Intersection(Vector2 start, List<Fix> track) {
        p1 = start.scale(phys.degrees);
        for (int t = 0; t < track.size(); t++) {
            p2 = track.get(t).position.scale(phys.degrees);
            if(p2.x > p1.x) {
                maxLon = (int) Math.floor(p2.x);
                minLon = (int) Math.floor(p1.x);
            }
            else {
                maxLon = (int) Math.floor(p1.x);
                minLon = (int) Math.floor(p2.x);
            }
            if(p2.y > p1.y) {
                maxLat = (int) Math.floor(p2.y);
                minLat = (int) Math.floor(p1.y);
            }
            else {
                maxLat = (int) Math.floor(p1.y);
                minLat = (int) Math.floor(p2.y);
            }
            for (int lat = minLat; lat <= maxLat+1; lat++) { //TODO ************** Bodge to correct for dangling latitudes in WVS database
                for (int lon = minLon; lon < (int) maxLon+2; lon++) {
                    if (world[lat+90][lon+worldOffset] == null)
                        world[lat+90][lon+worldOffset] = new GridPoint(lat, lon);
                    dataI = world[lat+90][lon+worldOffset].data.iterator();
                    for (int nPoints : world[lat+90][lon+worldOffset].segray) {
                        d1 = dataI.next();
                        for (int i = 1; i < nPoints; i++) {
                            d2 = dataI.next();
                            int o1 = orientation(p1, p2, d1);
                            int o2 = orientation(p1, p2, d2);
                            int o3 = orientation(d1, d2, p1);
                            int o4 = orientation(d1, d2, p2);

                            // General case
                            if (o1 != o2 && o3 != o4)
                                return true;
                            d1 = d2;
                        }
                    }
                }
            }
            p1 = p2;
        }
        return false;
    }
    boolean Intersection(Vector2 start, Vector2 end) {
        p1 = start.scale(phys.degrees);
            p2 = end.scale(phys.degrees);
            if(p2.x > p1.x) {
                maxLon = (int) Math.floor(p2.x);
                minLon = (int) Math.floor(p1.x);
            }
            else {
                maxLon = (int) Math.floor(p1.x);
                minLon = (int) Math.floor(p2.x);
            }
            if(p2.y > p1.y) {
                maxLat = (int) Math.floor(p2.y);
                minLat = (int) Math.floor(p1.y);
            }
            else {
                maxLat = (int) Math.floor(p1.y);
                minLat = (int) Math.floor(p2.y);
            }
        try {
            for (int lat = minLat; lat < maxLat+1; lat++) {
                for (int lon = minLon; lon < (int) maxLon+2; lon++) {
                    if (world[lat+90][lon+worldOffset] == null)
                        world[lat+90][lon+worldOffset] = new GridPoint(lat, lon);
                    dataI = world[lat+90][lon+worldOffset].data.iterator();
                    for (int nPoints : world[lat+90][lon+worldOffset].segray) {
                        d1 = dataI.next();
                        for (int i = 1; i < nPoints; i++) {
                            d2 = dataI.next();
                            int o1 = orientation(p1, p2, d1);
                            int o2 = orientation(p1, p2, d2);
                            int o3 = orientation(d1, d2, p1);
                            int o4 = orientation(d1, d2, p2);

                            // General case
                            if (o1 != o2 && o3 != o4)
                                return true;
                            d1 = d2;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    int orientation(Vector2 p, Vector2 q, Vector2 r)
    {
        // See https://www.geeksforgeeks.org/orientation-3-ordered-points/
        // for details of below formula.
//        double val = ((double)q.y -(double) p.y) * ((double)r.x - (double)q.x) -
//                ((double)q.x - (double)p.x) * ((double)r.y - (double)q.y);
        float val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);

        if (val == 0) return 0;  // colinear

        return (val > 0)? 1: 2; // clock or counterclock wise
    }

    void draw(Graphics2D g, Mercator screen) {
        Path2D results = new Path2D.Float();
        Vector2 p;
        g.setColor(Color.black);

        int startLat = (int) Math.floor(screen.bottomRight.y);
        if(startLat< -90)
            startLat = -90;
        int endLat = (int) Math.ceil(screen.topLeft.y);
        if(endLat > 90)
            endLat = 90;
        int startLong = (int) Math.floor(screen.topLeft.x);
        if(startLong<-360)
            startLong = -360;
        int endLong = (int) Math.ceil(screen.bottomRight.x);
        if(endLong > 360)
            endLong = 360;

        for (int lat = startLat; lat <= endLat; lat++) { //TODO ************** Bodge to correct for dangling latitudes in WVS database
            for (int lon = startLong; lon < endLong; lon++) {
                if (world[lat+90][lon+worldOffset] == null)
                    world[lat+90][lon+worldOffset] = new GridPoint(lat, lon);
                dataI = world[lat+90][lon+worldOffset].data.iterator();
                for (int nPoints : world[lat+90][lon+worldOffset].segray) {
                    d1 = dataI.next();
                    p = screen.fromLatLngToPoint(d1.y+Main.ChartOffsetY, d1.x+ Main.ChartOffsetX);
                    results.moveTo(p.x, p.y);
                    for (int i = 1; i < nPoints; i++) {
                        d1 = dataI.next();
                        p = screen.fromLatLngToPoint(d1.y+Main.ChartOffsetY, d1.x+Main.ChartOffsetX);
                        results.lineTo(p.x, p.y);
                    }
                }
            }
        }
        g.draw(results);
    }

    @Override
    public void render(Renderer renderer, Projection projection, long time) {
        renderer.setColor(Colors.BLACK);

        Vector2 topLeft = projection.getTopLeft();
        Vector2 bottomRight = projection.getBottomRight();

        int startLat = (int) Math.floor(bottomRight.y);
        if (startLat < -90) startLat = -90;
        int endLat = (int) Math.ceil(topLeft.y);
        if (endLat > 90) endLat = 90;
        int startLong = (int) Math.floor(topLeft.x);
        if (startLong < -360) startLong = -360;
        int endLong = (int) Math.ceil(bottomRight.x);
        if (endLong > 360) endLong = 360;

        for (int lat = startLat; lat <= endLat; lat++) {
            for (int lon = startLong; lon < endLong; lon++) {
                if (world[lat + 90][lon + worldOffset] == null)
                    world[lat + 90][lon + worldOffset] = new GridPoint(lat, lon);
                Iterator<Vector2> iter = world[lat + 90][lon + worldOffset].data.iterator();
                for (int nPoints : world[lat + 90][lon + worldOffset].segray) {
                    Vector2 first = iter.next();
                    Vector2 p1 = projection.fromLatLngToPoint(first.y + Main.ChartOffsetY, first.x + Main.ChartOffsetX);
                    for (int i = 1; i < nPoints; i++) {
                        Vector2 next = iter.next();
                        Vector2 p2 = projection.fromLatLngToPoint(next.y + Main.ChartOffsetY, next.x + Main.ChartOffsetX);
                        renderer.drawLine(p1.x, p1.y, p2.x, p2.y);
                        p1 = p2;
                    }
                }
            }
        }
    }
//    String TSZ ="""
//            56 18 1;56*55.36'N 018*17.12'E;56*57'N 018*17'E
//            59 19 1;59*50.02'N 019*54.83'E;59*50.4'N 019*54.48'E;59*50.53'N 019*54.48'E;59*50.62'N 019*55.02'E;59*50.62'N 019*55.35'E;59*50.33'N 019*57.5'E;59*49.93'N 019*56'E;59*50.02'N 019*54.83'E
//            """;
            // RORC
//        String TSZ =        "27 -13 1;27*0'N 13*0'W;27*0'N 14*0'W\n"+
//            "27 -14 1;27*0'N 14*0'W;27*0'N 15*0'W\n"+
//            "27 -15 1;27*0'N 15*0'W;27*0'N 16*0'W\n"+
//            "27 -16 1;27*0'N 16*0'W;28*0'N 17*0'W\n"+
//            "28 -17 1;28*0'N 17*0'W;28*48'N 17*48'W\n"+
//            "12 -62 1;12*10'N 61*40'W;13*0'N 61*14'W\n"+
//                    "13 -62 1;13*0'N 61*14'W;14*0'N 61*0'W\n"+
//                    "14 -62 1;14*0'N 61*0'W;15*0'N 61*0'W\n"+
//            "54 21 1;54*00'N 21*30'E;55*00'N 21*30'E\n"+
//                    "55 21 1;55*00'N 21*30'E;56*00'N 21*30'E\n"+
//                    "56 21 1;56*00'N 21*30'E;57*00'N 21*30'E\n"+
//                    "57 21 2;57*00'N 21*30'E;57*23'N 21*30'E\n"+
//                    "       22;57*30'N 21*00'E;57*30'N 21*50'E\n"+
//
//                    "58 24 1;58*00'N 24*35'E;58*23'N 24*35'E\n"+
//                    "57 24 1;57*00'N 24*35'E;58*00'N 24*35'E\n"+
//                    "54 11 1;54*44'N 11*57'E;55*00'N 11*57'E\n"+
//                    "55 11 1;55*00'N 11*57'E;56*00'N 11*57'E\n"+
////                    "55 19 1;55*0.007103'N 19*44.002103'E;55*10'N 19*00'E\n"+
////                    "55 18 1;55*10'N 19*0'E;55*20'N 18*00'E\n"+
////                    "55 17 1;55*20'N 18*0'E;55*30'N 17*00'E\n"+
//                    "57 18 1;57*30'N 18*30'E;57*30'N 19*00'E\n"+
//                    "57 19 1;57*30'N 19*00'E;57*30'N 20*00'E\n"+
//                    "57 20 1;57*30'N 20*00'E;57*30'N 21*00'E\n"+
////                    "57 21 1;57*30'N 21*00'E;57*30'N 21*50'E\n"+
//                    "60 22 1;60*00'N 22*23'E;61*00'N 22*23'E\n"+
//                    "59 22 1;59*45'N 22*00'E;59*45'N 22*23'E;60*00'N 22*23'E\n"+
//                    "59 21 1;60*00'N 21*00'E;59*45'N 21*00'E;59*45'N 22*00'E\n"+
//
////                    "47 -4 1;47*20'N 03*14'W;49*00'N 03*14'W\n"+
//
//
//                    "-35 173 1;34*59.25'S 173*56.5'E;34*58.5'S 173*57.75'E\n"+
//            "-36 174 1;35*10.3'S 174*19.5'E;35*10.12'S 174*19.6'E\n"+
//
//            "48 -6 1;49*0'N 5*35.1'W;48*48.6'N 5*25.2'W;48*37.2'N 5*12'W;48*29.4'N 5*22'W;48*35.1'N 5*42.2'W;48*42.6'N 6*2.8'W;48*56.5'N 5*51.6'W;49*0'N 5*42.2'W\n"+
//            "49 -6 2;49*0'N 5*42.2'W;49*2'N 5*37'W;49*0'N 5*35.1'W\n"+
//            "       ;50*0'N 5*51.7'W;49*56'N 6*00'W\n"+
//            "49 -3 1;50*0'N 2*24.7'W;49*51.3'N 2*21.1'W;49*46.3'N 2*50'W;50*0'N 2*55.7'W\n"+
//            "50 -3 1;50*0'N 2*55.7'W;50*3.5'N 2*57.4'W;50*8.5'N 2*28.3'W;50*0'N 2*24.7'W\n" +

//            "50 -7 2;50*00'N 6*05'W;50*20'N 6*05'W;50*20'N 6*00'W\n"+
//            "       ;50*00'N 6*50.4'W;50*04.2'N 6*48.5'W;50*01.2'N 6*32.8'W;50*00'N 6*33.3'W\n"+
//            "50 -6 1;50*20'N 6*00'W;50*20'N 5*49.6'W;50*01'N 5*49.6'W;50*00'N 5*51.7'W\n" +
//            "-44 147 2;43*14.5'S 147*59.9'E;43*13'S 147*59'E\n"+
//            "         ;43*3.28'S 147*24.95'E;43*3.28'S 147*25.15'E;43*3.45'S 147*25.15'E;43*3.45'S 147*24.95'E;43*3.28'S 147*24.95'E\n";

    String TSZ = "43 -10 1;43*31.40'N 10*5.20'W;" + //TSS CAP FINISTERRE
                            "43*21'N 9*36.40'W;" +
                            "43*21'N 9*36.4'W;" +
                            "43*10.5'N 9*44'W;" +
                            "42*52.8'N 9*44'W;" +
                            "42*52.8'N 10*13.85'W;" +
                            "43*18.95'N 10*13.85'W;" +
                            "43*31.40'N 10*5.20'W\n" +
            "37 -9 1;37*2.5'N 9*11.7'W;" +  //TSS CABO SAO VINCENTE
                    "36*56.7'N 9*10.5'W;" +
                    "36*51.5'N 9*4.3'W;" +
                    "36*50.1'N 8*57.2'W;" +
                    "36*25.2'N 9*6'W;" +
                    "36*28.46'N 9*21.6'W;" +
                    "36*44.2'N 9*39.85'W;" +
                    "36*56.6'N 9*43.3'W;" +
                    "37*2.5'N 9*11.7'W\n" +
            "38 -9 1;38*52'N 9*41.1'W;" +  // TSS CAPE ROCA
                    "38*39.7'N 9*40'W;" +
                    "38*33.9'N 10*11.7'W;" +
                    "38*40.9'N 10*13.8'W;" +
                    "38*52'N 10*13.8'W;" +
                    "38*52'N 9*41.1'W\n" +
            "28 -14 1;28*19.8'N 14*47.7'W;" +  //TSS CANARIES EST
                    "27*48.78'N 15*0.35'W;" +
                    "27*51.5'N 15*8.85'W;" +
                    "28*20.5'N 14*57.1'W;" +
                    "28*19.8'N 14*47.7'W\n" +
            "28 -15 1;28*33.8'N 15*39.3'W;" +  //TSS CANARIES OUEST
                    "27*58.4'N 16*12.95'W;" +
                    "28*3.45'N 16*19.65'W;" +
                    "28*38.1'N 15*46.8'W;" +
                    "28*33.8'N 15*39.3'W\n" +
//            "21 -30 1;21*31'N 16*25'W;" + //ZI	MAURITANIE
//                    "16*0'N 16*25'W;" +
//                    "16*0'N 17*35'W;" +
//                    "21*31'N 17*35'W;" +
//                    "21*31'N 16*25'W\n" +
            "-24 -42 1;24*54'S 42*51'W;" + //Rio de Janeiro
                    "25*30'S 42*28.3333333333333'W;" +
                    "25*55'S 43*20'W;" +
                    "25*35.8333333333333'S 43*45'W;" +
                    "24*54'S 42*51'W\n" +
            "-21 -44 1;21*30'S 39*45'W;" + //ZI Cabo Frio
                    "21*56'S 39*14'W;" +
                    "23*0'S 40*17.1666666666667'W;" +
                    "23*44.8333333333333'S 41*15'W;" +
                    "23*29'S 41*38'W;" +
                    "22*8'S 40*25'W;" +
                    "21*30'S 39*45'W\n" +
//            "49 -5 1;49*2.05016666666667'N 5*36.7001666666667'W;" +     // TSS	OUESSANT
//                    "48*48.6'N 5*25.0001666666667'W;" +
//                    "48*37.2'N 5*11.85'W;" +
//                    "48*29.3501666666667'N 5*22.05'W;" +
//                    "48*34.9998333333333'N 5*42.4998333333333'W;" +
//                    "48*42.4998333333333'N 6*3.10016666666667'W;" +
//                    "48*56.4'N 5*51.6'W;" +
//                    "49*2.05016666666667'N 5*36.7001666666667'W\n" +
//            "40 -28 1;40*54'N 28*34.002'W;" +  //ZPB	Acores
//                    "40*0'N 31*49.998'W;" +
//                    "37*34.998'N 31*51.402'W;" +
//                    "36*20.4'N 25*35.598'W;" +
//                    "36*37.602'N 24*13.002'W;" +
//                    "37*48.6'N 23*49.998'W;" +
//                    "40*54'N 28*34.002'W;\n" +
//            "45 -25 1;17*45'N 25*21'W;" +    //ZPB	Cap	Vert
//                    "16*10.002'N 26*7.998'W;" +
//                    "14*40.002'N 25*25.002'W;" +
//                    "14*31.998'N 22*49.998'W;" +
//                    "15*58.002'N 22*7.998'W;" +
//                    "17*42'N 22*40.002'W;" +
//                    "17*45'N 25*21'W\n" +
//            "49 -6 1;49*35.538'N 6*16.398'W;" +      //TSS	South	Scilly
//                    "49*46.098'N 6*16.56'W;" +
//                    "49*46.032'N 6*29.55'W;" +
//                    "49*35.55'N 6*34.098'W;" +
//                    "49*35.538'N 6*16.398'W\n" +
//            "-41 173 1;41*00.0'S 173*00.0'E;" +      //NZ Gap filler
//            "41*00.0'S 175*00.0'E;\n" +
            "46 -3 1;46*57.132'N 2*31.602'W;" +    //ZI	Eoliennes	Yeu
                    "46*50.916'N 2*24.258'W;" +
                    "46*48.024'N 2*29.466'W;" +
                    "46*53.076'N 2*35.436'W;" +
                    "46*54.966'N 2*35.508'W;" +
                    "46*57.132'N 2*31.602'W\n";
//            "50 -3 1;50*0'N 2*55.7'W;" +   // Mid channel
//                    "50*3.5'N 2*57.4'W;" +
//                    "50*8.5'N 2*28.3'W;" +
//                    "50*0'N 2*24.7'W\n"+
//            "49 -3 1;50*0'N 2*24.7'W;49*51.3'N 2*21.1'W;49*46.3'N 2*50'W;50*0'N 2*55.7'W\n";





    private GridPoint[][] obstructions = new GridPoint[181][361];

    void ReadObstructions() {
        if(TSZ.length() == 0)
            return;
        String[] lines = TSZ.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String[] line = lines[i].split(";");
            String[] gridData = line[0].split(" ");
//            if(gridData.length != 3) {
//                if(! gridData[0].contains("*")){
//                    System.out.println("Obstruction data does not begin with a * or 3 numbers:\n"+line);
//                    System.exit(-1);
//                }
//                double maxLat = Double.MIN_VALUE;
//                double minLat = Double.MAX_VALUE;
//                double maxLon = Double.MIN_VALUE;
//                double minLon = Double.MAX_VALUE;
//                LinkedList<Vector2> conordinate = new LinkedList<>();
//                for (int j = 1; j < line.length; j++) {
//                    String[] coordinates = line[j].split(" ");
//                    Vector2 p = new Vector2(Fix.parseLongitude(coordinates[1]),Fix.parseLatitude(coordinates[0]));
//                    conordinate.add(p);
//                    if(p.x > maxLon)
//                        maxLon = p.x;
//                    if(p.x < minLon)
//                        minLon = p.x;
//                    if(p.y > maxLat)
//                        maxLat = p.y;
//                    if(p.y < minLat)
//                        minLat = p.y;
//                }
//                LinkedList<Vector2> splitLine = new LinkedList<>();
//
//                splitLine.add(conordinate.getFirst());
//                double x = Math.ceil(minLon);
//                while (x <  Math.ceil(maxLon)) {
//                    Vector2 p = new Vector2(x,(x-conordinate.getFirst().x)/
//                            (conordinate.getLast().x- conordinate.getFirst().x) *
//                            (conordinate.getLast().y- conordinate.getFirst().y) +
//                            conordinate.getFirst().y);
//                    splitLine.add(p);
//                }
//                splitLine.add(conordinate.getLast());
//
//
//                if(obstructions[gridLat+90][gridLon+180] == null) {
//                    obstructions[gridLat+90][gridLon+180] = new GridPoint();
//                }
//                GridPoint gp = obstructions[gridLat+90][gridLon+180];
//                int last = 0;
//                while(true){
//                    for (int j = 1; j < line.length; j++) {
//                        String[] coordinates = line[j].split(" ");
//                        Vector2 p = new Vector2(Fix.parseLongitude(coordinates[1]),Fix.parseLatitude(coordinates[0]));
//                        gp.data.add(p);
//                    }
//                    gp.segray.add(gp.data.size() - last);
//                    last = gp.data.size();
//                    gridSegments--;
//                    if(gridSegments == 0)
//                        break;
//                    i++;
//                    line = lines[i].split(";");
//                }
//            }
            int gridLat = Integer.parseInt(gridData[0]);
            int gridLon = Integer.parseInt(gridData[1]);
            int gridSegments = Integer.parseInt(gridData[2]);
            if(obstructions[gridLat+90][gridLon+180] == null) {
                obstructions[gridLat+90][gridLon+180] = new GridPoint();
            }
            GridPoint gp = obstructions[gridLat+90][gridLon+180];
            int last = 0;
            while(true){
                for (int j = 1; j < line.length; j++) {
                    String[] coordinates = line[j].split(" ");
                    Vector2 p = new Vector2(Fix.parseLongitude(coordinates[1]),Fix.parseLatitude(coordinates[0]));
                    gp.data.add(p);
                }
                gp.segray.add(gp.data.size() - last);
                last = gp.data.size();
                gridSegments--;
                if(gridSegments == 0)
                    break;
                i++;
                line = lines[i].split(";");
            }
        }
    }
}
