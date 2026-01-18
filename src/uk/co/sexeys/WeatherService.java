package uk.co.sexeys;
import uk.co.sexeys.water.Water;
import uk.co.sexeys.water.PrevailingCurrent;
import uk.co.sexeys.water.Current;
import uk.co.sexeys.water.Tide;
import uk.co.sexeys.wind.Wind;
import uk.co.sexeys.wind.Prevailing;
import uk.co.sexeys.wind.SailDocs;
import uk.co.sexeys.wind.VRWind;

import java.util.Calendar;
import java.util.List;

/**
 * Service for managing weather data (wind, water/currents, waves).
 * Encapsulates loading and querying of meteorological data.
 */
public class WeatherService {

    private Wind wind;
    private Water water;
    private Waves waves;

    /**
     * Initialize wind data from GRIB files.
     */
    public void loadWindFromGrib(List<String> files, long departureTime) {
        Wind prevailing = new Prevailing(departureTime);
        if (files != null && !files.isEmpty()) {
            wind = new SailDocs(files, prevailing);
        } else {
            wind = prevailing;
        }
    }

    /**
     * Initialize wind data from Virtual Regatta API.
     */
    public void loadVRWind(Calendar time, long departureTime) {
        VRWind.init();
        wind = new VRWind(time, new Prevailing(departureTime));
    }

    /**
     * Initialize current/tide data from GRIB files.
     */
    public void loadWaterFromGrib(List<String> currentFiles, List<String> tideFiles, long departureTime) {
        water = new PrevailingCurrent(departureTime);
        if (currentFiles != null && !currentFiles.isEmpty()) {
            water = new Current(currentFiles, water);
        }
        if (tideFiles != null && !tideFiles.isEmpty()) {
            water = new Tide(tideFiles, water);
        }
    }

    /**
     * Initialize wave data from GRIB files.
     */
    public void loadWavesFromGrib(List<String> files, long departureTime) {
        if (files != null && !files.isEmpty()) {
            waves = new Waves(files);
        } else {
            waves = new Waves(departureTime); // No waves
        }
    }

    /**
     * Get wind value at a position and time.
     *
     * @param positionDegrees Position in degrees (x=lon, y=lat)
     * @param timeMs          Time in milliseconds
     * @param result          Vector2 to store result (x=east component, y=north component) in m/s
     * @return Wind source type
     */
    public Wind.SOURCE getWind(Vector2 positionDegrees, long timeMs, Vector2 result) {
        if (wind != null) {
            return wind.getValue(positionDegrees, timeMs, result);
        }
        result.x = 0;
        result.y = 0;
        return Wind.SOURCE.PREVAILING;
    }

    /**
     * Get water/current value at a position and time.
     *
     * @param positionDegrees Position in degrees (x=lon, y=lat)
     * @param timeMs          Time in milliseconds
     * @param result          Vector2 to store result (x=east component, y=north component) in m/s
     * @return Water source type
     */
    public Water.SOURCE getWater(Vector2 positionDegrees, long timeMs, Vector2 result) {
        if (water != null) {
            return water.getValue(positionDegrees, timeMs, result);
        }
        result.x = 0;
        result.y = 0;
        return Water.SOURCE.PREVAILING;
    }

    /**
     * Get wave height at a position and time.
     *
     * @param positionDegrees Position in degrees (x=lon, y=lat)
     * @param timeMs          Time in milliseconds
     * @return Wave height in meters
     */
    public float getWaveHeight(Vector2 positionDegrees, long timeMs) {
        if (waves != null) {
            return waves.getValue(positionDegrees, timeMs);
        }
        return 0;
    }

    /**
     * Get the last available time for weather data.
     */
    public long getLastWindTime() {
        if (wind instanceof SailDocs) {
            return ((SailDocs) wind).GetLastValidForecast();
        }
        return Long.MAX_VALUE;
    }

    // Getters for direct access when needed for rendering

    public Wind getWind() {
        return wind;
    }

    public Water getWater() {
        return water;
    }

    public Waves getWaves() {
        return waves;
    }

    // Setters for dependency injection

    public void setWind(Wind wind) {
        this.wind = wind;
    }

    public void setWater(Water water) {
        this.water = water;
    }

    public void setWaves(Waves waves) {
        this.waves = waves;
    }
}
