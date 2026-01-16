package uk.co.sexeys;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by Jim on 24/02/2018.
 *
 * All data in Harmonic files is stored against local time from start of local year (NOT UTC)
 * To get UTC SUBTRACT the time idx_time_zone value from the local time
 *
 */
class Compute {
    private Harmonics harmonics;
    private double[] amplitudes;
    private double[] phases;
    private IDX.IDX_entry idx;
    private int currentYear;
    double maxAmplitude;
    double minAmplitude;


    Compute( Harmonics h, IDX.IDX_entry pIDX, int year) {
        harmonics = h;
        idx = pIDX;
        preCompute(year);
    }

    private void preCompute (int year) {
        int a;
        currentYear = year;
        int yearsSinceFirst= year-harmonics.first_year;
        amplitudes = new double[harmonics.num_csts];
        phases = new double[harmonics.num_csts];
        maxAmplitude = 0;
        for (a = 0; a < harmonics.num_csts; a++) {
            amplitudes[a] = idx.pref_sta_data.amplitude[a] * harmonics.m_cst_nodes[a][yearsSinceFirst];
            phases[a] = harmonics.m_cst_epochs[a][yearsSinceFirst] - idx.pref_sta_data.epoch[a];
            maxAmplitude += amplitudes[a];
        }
        minAmplitude = -maxAmplitude;
        if(idx.IDX_type == 't') {
            maxAmplitude = maxAmplitude * idx.IDX_ht_mpy + idx.IDX_ht_off;
            minAmplitude = minAmplitude * idx.IDX_lt_mpy + idx.IDX_lt_off;
        }
    }

    long timeFromYearStart(Calendar calendar) {
        if(calendar.get(Calendar.YEAR) != currentYear) {
            preCompute(calendar.get(Calendar.YEAR));
        }
        Calendar yearTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        yearTime.set(calendar.get(Calendar.YEAR), Calendar.JANUARY, 1, 0, 0, 0);
        return (calendar.getTimeInMillis() - yearTime.getTimeInMillis() )/phys.msPerSecond;
    }

    private double time2dt_tide (double timeUTC, int deriv) {
        double dt_tide = 0.0;
        int a, b;
        double term, tempd, amp;

        tempd = Math.PI / 2.0 * deriv;
        for (a=0; a< harmonics.num_csts; a++) {
            amp = amplitudes[a];
            if(amp != 0) {
                term = amp * Math.cos(tempd + harmonics.m_cst_speeds[a] * timeUTC + phases[a]);
                for (b = deriv; b > 0; b--)
                    term *= harmonics.m_cst_speeds[a];
                dt_tide += term;
            }
        }
        return dt_tide;
    }

    class TideData {
        double data;
        long time;

        TideData(long t, double d) {
            data = d;
            time = t;
        }
    }

    long NextHighWater(long startTime) {
        long t = startTime;
        double current = time2dt_tide(startTime, 1), last;
        while (t <= startTime + 24 * 60 * 60) {
            last = current;
            current = time2dt_tide(t, 1);
            if (current * last <= 0){ // each zero crossing of the rate is an event
                if(current < last) { // this is a high water event
                    last = current;
                    while (current * last > 0) { // refine zero crossing time to nearest minute
                        t -= 60;
                        last = current;
                        current = time2dt_tide(t, 1);
                    }
                    return t;
                }
            }
            t += 3600;
        }
        return -1;
    }

    long PreviousHighWater(long startTime) {
        long t = startTime;
        double current = time2dt_tide(startTime, 1), last;
        while (t >= startTime - 24 * 60 * 60) {
            last = current;
            current = time2dt_tide(t, 1);
            if (current * last <= 0){ // each zero crossing of the rate is an event
                if(current > last) { // this is a high water event
                    last = current;
                    while (current * last > 0) { // refine zero crossing time to nearest minute
                        t += 60;
                        last = current;
                        current = time2dt_tide(t, 1);
                    }
                    return t;
                }
            }
            t -= 3600;
        }
        return -1;
    }



    TideData[] primary(long startTime) {
        int p = 0;
        TideData[] output = new TideData[24*60/30+1];
        for( long t = startTime; t <= startTime+24*60*60; t += 30 * 60 ) {
            output[p++] = new TideData(t, time2dt_tide(t,0));
        }
        return output;
    }


    TideData[] secondary(long startTime) {
        long searchDelta = 600;
        long fineDelta = 60;
        long startSearch = startTime - 24 * 60 * 60;
        long endSearch = startTime + 48 * 60 * 60;

        List<Long> results = new ArrayList<Long>();

        double current, last = time2dt_tide(startSearch, 1);
        long t = startSearch + searchDelta;
        if(     (idx.IDX_ht_mpy != 1 && idx.IDX_ht_off != 0) ||
                (idx.IDX_lt_mpy != 1 && idx.IDX_lt_off != 0) )
            System.out.println("Combined multiplier and offset..");
        while (t <= endSearch) { // each zero crossing of the rate is an event
            current = time2dt_tide(t, 1);
            if (current * last <= 0){ // refine zero crossing time to nearest minute
                double previous = last;
                long ti = t - searchDelta + fineDelta;
                while ( ti <= t) {
                    double latest = time2dt_tide(ti, 1);
                    if (previous * latest < 0)
                        break;
                    previous = latest;
                    ti += fineDelta;
                }
                results.add(ti);
            }
            last = current;
            t += searchDelta;
        }

        List<TideData> secondaryData = new ArrayList<TideData>();

        double offset = (idx.IDX_ht_off + idx.IDX_lt_off )/2;

        if (results.size() <= 12) {
            for (int r = 0; r < results.size() - 1; r++) {
                long start = results.get(r);
                long end = results.get(r + 1);
                boolean LW = time2dt_tide(start,2) > 0; // first event is LW
                double lt_mpy,ht_mpy;
                if(idx.IDX_ht_mpy ==1 && idx.IDX_lt_mpy == 1)
                    if(LW) {
                        double tide = time2dt_tide(start,0);
                        if(tide != 0)
                            lt_mpy = ( tide + idx.IDX_lt_off-offset) / tide;
                        else
                            lt_mpy = 1;
                        tide = time2dt_tide(end,0);
                        if(tide != 0)
                            ht_mpy = ( tide + idx.IDX_ht_off-offset) / tide;
                        else
                            ht_mpy = 1;
                    }
                    else {
                        double tide = time2dt_tide(start,0);
                        if(tide != 0)
                            ht_mpy = ( tide + idx.IDX_ht_off -offset) / tide;
                        else
                            ht_mpy = 1;
                        tide = time2dt_tide(end,0);
                        if(tide != 0)
                            lt_mpy = ( tide + idx.IDX_lt_off -offset) / tide;
                        else
                            lt_mpy = 1;
                    }
                else {
                    lt_mpy = idx.IDX_lt_mpy;
                    ht_mpy = idx.IDX_ht_mpy;
                }

                long interpolator ;
                for (t = start; t < end; t += 30 * 60) {
                    if (LW)
                        interpolator = t - start;
                    else
                        interpolator = end - t;
                    double multiplier = ((ht_mpy - lt_mpy) * interpolator) / (end - start) + lt_mpy;
                    long timeOffset = ((idx.IDX_ht_time_off - idx.IDX_lt_time_off) * interpolator) / (end - start) + idx.IDX_lt_time_off;

                    secondaryData.add(new TideData(t + timeOffset*60, time2dt_tide(t,0) * multiplier + offset));
                }
            }
            int start = 0;
            while (secondaryData.get(start).time < startTime)
                start++;
            start--;
            int end = start;
            while (secondaryData.get(end).time < startTime+24*60*60)
                end++;
            TideData[] output = new TideData[end-start];
            for(int i = start , p = 0; i < end; i++, p++)
                output[p] = secondaryData.get(i);
            return output;
        }

        else { // double tides
            int HW = 0;
            for(long r:results) {
                if(time2dt_tide(r, 0) > 0)
                    HW++;
            }
            long timeOffset;
            double multiplier;
            if (HW > results.size()/2) {
                multiplier = idx.IDX_lt_mpy;
                offset = idx.IDX_lt_off;
                timeOffset = idx.IDX_lt_time_off;
            }
            else {
                multiplier = idx.IDX_ht_mpy;
                offset = idx.IDX_ht_off;
                timeOffset = idx.IDX_ht_time_off;
            }

            int p = 0;
            TideData[] output = new TideData[24*60/30+1];
            for( t = startTime; t <= startTime+24*60*60; t += 30 * 60 ) {
                output[p++] = new TideData(t, time2dt_tide(t - timeOffset,0) * multiplier + offset);
            }
            return output;
        }
    }
}
