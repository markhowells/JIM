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

import java.io.IOException;

/**
 * GRIB record Local Use Section (LUS).
 * @author spidru
 */
public class Grib2RecordLUS
{
    private int length;
    
    public Grib2RecordLUS(GribInputStream in) throws IOException
    {
        in.mark(10);
        
        length = in.readUINT(4);
        int section = in.readUINT(1);
        
        if (section != 2)
        {
            in.reset();
            length = 0;
        }
    }
    
    protected int getLength()
    {
        return length;
    }
}
