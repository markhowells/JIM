package uk.co.sexeys;

/**
 * Created by Jim on 10/08/2018.
 *
 */

import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Scanner;

import static java.lang.System.exit;

class IDX {
    private static final int REGION = 0, COUNTRY = 1, STATE = 2;
    private static final int SOURCE_TYPE_ASCII_HARMONIC = 1;
    private String fileName;

    IDX(String fileName) {
        this.fileName = fileName;
    }

    private class abbr_entry {
        int type;
        String short_s, long_s;
    }

    private LinkedList<abbr_entry> abbreviations = new LinkedList<abbr_entry>();

    class IDX_entry {
        int source_data_type = 0;
        Harmonics.StationData pref_sta_data = null;
        Boolean IDX_Useable;
        String IDX_tzname;
        char IDX_type;
        String IDX_zone;
        double IDX_lon;
        double IDX_lat;
        String IDX_station_name;
        int IDX_time_zone;
        int IDX_ht_time_off;
        double IDX_ht_mpy;
        double IDX_ht_off;
        int IDX_lt_time_off;
        double IDX_lt_mpy;
        double IDX_lt_off;
        int IDX_sta_num;
        int IDX_flood_dir;
        int IDX_ebb_dir;
        int IDX_ref_file_num;
        String IDX_reference_name;
        boolean have_offsets;
        int station_tz_offset;
        Vector2 screenPosition;
    }
    private LinkedList<IDX_entry> m_IDX_array = new LinkedList<IDX_entry>();

    IDX_entry FindEntry(String station) {
        for (IDX_entry i: m_IDX_array) {
            if (i.IDX_station_name.contains(station))
                return i;
        }
        return null;
    }

    private LinkedList<IDX_entry> drawn = new LinkedList<IDX_entry>();

    void draw(Graphics2D g, Mercator screen) {
        if(m_IDX_array == null )
            return;
        g.setColor(Color.magenta);
        drawn.clear();
        for (IDX_entry idx: m_IDX_array) {
            if(idx.IDX_type !='T' && idx.IDX_type !='t')
                continue;
            if(!idx.IDX_Useable)
                continue;
            double lon = idx.IDX_lon;
            double lat = idx.IDX_lat;
            idx.screenPosition = screen.fromLatLngToPoint(lat, lon);
            if(lon > screen.topLeft.x && lon < screen.bottomRight.x &&
                    lat < screen.topLeft.y && lat > screen.bottomRight.y) {
                g.fillRect((int) idx.screenPosition.x-10, (int) idx.screenPosition.y-10, 20, 20);
                drawn.add(idx);
            }
            else
                idx.screenPosition = null;
        }
        g.setColor(Color.black);
    }

    IDX_entry closestDrawn(Vector2 p) {
        double d2, closestDistance = Double.MAX_VALUE;
        IDX_entry closest = null;
        for (IDX_entry idx: drawn) {
            d2 = p.minus(idx.screenPosition).mag2();
            if(d2 < closestDistance) {
                closestDistance = d2;
                closest = idx;
            }
        }
        return closest;
    }


    void read() throws IOException {
        FileInputStream is = new FileInputStream(fileName+".idx");
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        is.close();
        String data = new String(buffer);
        Scanner scanner = new Scanner(data);
        char c;
        IDX_entry pIDX = null;
        boolean have_index = false, xref_start = false, secondLine = false;
        scanner.nextLine();
        String line;
        while  (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if(line == null || line.length() == 0)
                continue;
            c = line.charAt(0);
            if(c == '#' || c <= ' ')
                continue;
            if (!have_index && !xref_start) {
                if (line.startsWith("XREF"))
                    xref_start = true;
            }
            else if (!have_index && line.startsWith("*END*")) {
                if (abbreviations.size() == 0) {
                    throw new IOException("HARMONIC.IDX.txt FILE CORRUPT");
                }
                else
                    have_index = true;
            }
            else if (!have_index) {
                abbr_entry entry  = new abbr_entry();
                String [] token = line.split(" ",3);
                if(token[0].startsWith("REGION"))
                    entry.type = REGION;
                else if(token[0].startsWith("COUNTRY"))
                    entry.type = COUNTRY;
                else if(token[0].startsWith("STATE"))
                    entry.type = STATE;
                entry.short_s = token[1];
                entry.long_s = token[2];
                abbreviations.add(entry);
            }
            else if (!secondLine) {
                switch (line.charAt(0)) {
                    case 'T':
                    case 'C':
                        pIDX = decipherIDX(line);
                        pIDX.IDX_ht_time_off = pIDX.IDX_lt_time_off = 0;
                        pIDX.IDX_ht_mpy      = pIDX.IDX_lt_mpy = 1.0;
                        pIDX.IDX_ht_off      = pIDX.IDX_lt_off = 0.0;
                        pIDX.IDX_sta_num     = 0;
                        pIDX.IDX_reference_name = pIDX.IDX_station_name;
                        pIDX.have_offsets = false;
                        pIDX.station_tz_offset = 0;
                        m_IDX_array.add(pIDX);
                        pIDX = null;
                        secondLine = false;
                        break;
                    case 't':
                    case 'c':
                    case 'U':
                    case 'u':
                        pIDX = decipherIDX(line);
                        secondLine = true;
                        break;
                    case 'I': // information
                    default:
                        // ignore
                }
            }
            else {
                if(line.charAt(0) == '^') {
                    if(pIDX == null)
                        throw new IOException("HARMONIC.IDX.txt FILE CORRUPT");
                    Scanner sc = new Scanner(line);
                    pIDX.IDX_ht_time_off = Integer.valueOf(sc.next().substring(1));
                    pIDX.IDX_ht_mpy = Double.valueOf(sc.next());
                    pIDX.IDX_ht_off = Double.valueOf(sc.next());
                    pIDX.IDX_lt_time_off = Integer.valueOf(sc.next());
                    pIDX.IDX_lt_mpy= Double.valueOf(sc.next());
                    pIDX.IDX_lt_off = Double.valueOf(sc.next());
                    pIDX.IDX_sta_num = Integer.valueOf(sc.next());
                    pIDX.IDX_flood_dir = Integer.valueOf(sc.next());
                    pIDX.IDX_ebb_dir = Integer.valueOf(sc.next());
                    pIDX.IDX_ref_file_num = Integer.valueOf(sc.next());
                    pIDX.IDX_reference_name = sc.nextLine();

                    if(Math.abs(pIDX.IDX_flood_dir) > 360)
                        pIDX.IDX_Useable = false;
                    if(Math.abs(pIDX.IDX_ebb_dir) > 360)
                        pIDX.IDX_Useable = false;

                    //    Fix up the secondaries which are identical to masters
                    if(pIDX.IDX_ht_mpy == 0.0)
                        pIDX.IDX_ht_mpy = 1.0;
                    if(pIDX.IDX_lt_mpy == 0.0)
                        pIDX.IDX_lt_mpy = 1.0;
                }
                else if(line.charAt(0) == '&') {
                    String [] token = line.split(" ",9);
                    try {
                        pIDX.IDX_ht_time_off = Integer.valueOf(token[0].substring(1));
                        pIDX.IDX_ht_mpy = Double.valueOf(token[1]);
                        pIDX.IDX_ht_off = Double.valueOf(token[2]);
                        pIDX.IDX_lt_time_off = Integer.valueOf(token[3]);
                        pIDX.IDX_lt_mpy= Double.valueOf(token[4]);
                        pIDX.IDX_lt_off = Double.valueOf(token[5]);
                        pIDX.IDX_sta_num = Integer.valueOf(token[6]);
                        pIDX.IDX_ref_file_num = Integer.valueOf(token[7]);
                        pIDX.IDX_reference_name = token[8];
                    } catch (NumberFormatException e) {
                        token = line.split(" ",10);
                        try {
                            pIDX.IDX_ht_time_off = Integer.valueOf(token[0].substring(1));
                            pIDX.IDX_ht_mpy = Double.valueOf(token[1]);
                            pIDX.IDX_ht_off = Double.valueOf(token[2]);
                            pIDX.IDX_lt_time_off = Integer.valueOf(token[3]);
                            pIDX.IDX_lt_mpy= Double.valueOf(token[4]);
                            pIDX.IDX_lt_off = Double.valueOf(token[5]);
                            pIDX.IDX_sta_num = Integer.valueOf(token[6]);
                            if(pIDX.IDX_tzname == null)
                                pIDX.IDX_tzname = token[7];
                            pIDX.IDX_ref_file_num = Integer.valueOf(token[8]);
                            pIDX.IDX_reference_name = token[9];
                        } catch (NumberFormatException ee) {
                            throw new IOException("HARMONIC.IDX.txt FILE CORRUPT\n"+
                                    m_IDX_array.get(m_IDX_array.size()-1).IDX_reference_name+"\n"+ee);
                        }
                    }

                }
                else {
                    System.out.println(line+ " not recognised");
                    exit(1);
                }
                if(Math.abs(pIDX.IDX_ht_time_off) > 1000) {
                    if (pIDX.IDX_type == 't')
                        pIDX.IDX_Useable = false;
                }
                else if(Math.abs(pIDX.IDX_lt_time_off) > 1000)
                    if(pIDX.IDX_type == 't')
                        pIDX.IDX_lt_time_off = pIDX.IDX_ht_time_off;
                if(pIDX.IDX_lt_off > 100)
                    pIDX.IDX_lt_off = 0;
                pIDX.IDX_ref_file_num = 0;
                pIDX.have_offsets = ( pIDX.IDX_ht_time_off != 0 ||
                        pIDX.IDX_ht_off != 0.0 ||
                        pIDX.IDX_lt_off != 0.0 ||
                        pIDX.IDX_ht_mpy != 1.0 ||
                        pIDX.IDX_lt_mpy != 1.0);
                m_IDX_array.add(pIDX);
                pIDX = null;
                secondLine = false;
            }
        }
    }
    private IDX_entry decipherIDX(String line) {
        IDX_entry pIDX = new IDX_entry();
        pIDX.source_data_type = SOURCE_TYPE_ASCII_HARMONIC;
        pIDX.IDX_Useable = true;
        Scanner sc = new Scanner(line);
        String token;
        try {
            token = sc.next();
            pIDX.IDX_type = token.charAt(0);
            pIDX.IDX_zone = token.substring(1);
            pIDX.IDX_lon = Double.valueOf(sc.next());
            pIDX.IDX_lat = Double.valueOf(sc.next());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        String[] TZ = sc.next().split(":");
        int TZHr = Integer.valueOf(TZ[0]);
        int TZMin = Integer.valueOf(TZ[1]);
        pIDX.IDX_station_name = sc.nextLine().substring(1);
        pIDX.IDX_time_zone = TZHr*60 + (int) Math.signum(TZHr) * TZMin;

//        String TZ = sc.next();
//        if (TZ.contains("-") )
//            pIDX.IDX_time_zone = "GMT"+TZ;
//        else
//            pIDX.IDX_time_zone = "GMT+"+TZ;
//        pIDX.IDX_station_name = sc.nextLine().substring(1);

        return pIDX;
    }
}

