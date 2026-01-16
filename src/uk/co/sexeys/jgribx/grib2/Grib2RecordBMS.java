/*
 * ============================================================================
 * JGribX
 * ============================================================================
 * Written by Andrew Spiteri <andrew.spiteri@um.edu.mt>
 * Adapted from JGRIB: http://jgrib.sourceforge.net/
 * 
 * Licensed under MIT: https://github.com/spidru/JGribX/blob/master/LICENSE
 * ============================================================================
 */
package uk.co.sexeys.jgribx.grib2;

import uk.co.sexeys.jgribx.GribInputStream;
import uk.co.sexeys.jgribx.Logger;
import uk.co.sexeys.jgribx.NotSupportedException;

import java.io.IOException;

/**
 *
 * @author AVLAB-USER3
 */
public class Grib2RecordBMS
{
    protected enum Indicator
    {
        BITMAP_SPECIFIED, BITMAP_PREDETERMINED, BITMAP_PREDEFINED, BITMAP_NONE
    }
    
    protected Indicator indicator;
    protected int length;
    
    public static Grib2RecordBMS readFromStream(GribInputStream in) throws IOException, NotSupportedException
    {
        Grib2RecordBMS bms = new Grib2RecordBMS();
        
        /* [1-4] Length of section in octets */
        bms.length = in.readUINT(4);
        
        /* [5] Section number */
        int section = in.readUINT(1);
        if (section != 6)
        {
            Logger.println("BMS contains invalid section number", Logger.ERROR);
            return null;
        }
        
        /* [6] Bitmap indicator */
        int bmIndicator = in.readUINT(1);
        bms.indicator = bms.determineIndicator(bmIndicator);
        if (bms.indicator != Indicator.BITMAP_NONE)
        {
            throw new NotSupportedException("BMS bitmap not yet supported");
//            in.skip(bms.length - 6);
        }
        return bms;
    }
    
    private Indicator determineIndicator(int value)
    {
        Indicator ind;
        if (value == 0)
        {
            ind = Indicator.BITMAP_SPECIFIED;
        }
        else if (value >= 1 && value <= 253)
        {
            ind = Indicator.BITMAP_PREDETERMINED;
        }
        else if (value == 254)
        {
            ind = Indicator.BITMAP_PREDEFINED;
        }
        else
        {
            ind = Indicator.BITMAP_NONE;
        }
        return ind;
    }
}
