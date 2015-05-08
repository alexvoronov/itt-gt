package net.gcdc.grotr.ittgt.gpsd;

import java.util.Date;

public class GpsData {
    private double  lat;
    private double  lon;
    private double  speed;
    private double  bearing;
    private boolean pai;
    private Date    timestamp;

    public double  lat()       { return lat; }
    public double  lon()       { return lon; }
    public double  speed()     { return speed; }
    public double  bearing()   { return bearing; }
    public boolean pai()       { return pai; }
    public Date    timestamp() { return timestamp; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private GpsData gpsData = new GpsData();

        public Builder lat(double lat)           { gpsData.lat = lat; return this; }
        public Builder lon(double lon)           { gpsData.lon = lon; return this; }
        public Builder speed(double speed)       { gpsData.speed = speed; return this; }
        public Builder bearing(double bearing)   { gpsData.bearing = bearing; return this; }
        public Builder pai(boolean pai)          { gpsData.pai = pai; return this; }
        public Builder timestamp(Date timestamp) { gpsData.timestamp = timestamp; return this; }

        public GpsData create() { return gpsData; }
    }
}