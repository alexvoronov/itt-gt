package net.gcdc.ittgt.model;

import java.util.Date;

public class Vehicle {
    public Date   simulationTimestamp; // ISO 8601 in JSON. Date in Java since Gson natively supports it.
    public int    id;
    public double lat;
    public double lon;
    public double height;
    public double speedMetersPerSecond;
    public double heading;            // Direction of the velocity.
    public double orientation;        // Also absolutely in wgs84.
    public double yawRate;            // Change of orientation.
    public double accelerationLon;
    public double accelerationLat;    // Lattitudinal or lateral?
    public double vehicleLength;
    public double vehicleWidth;
    public double distRefPointToRear;

    @Override
    public String toString() {
        return "Vehicle (t=" + simulationTimestamp + ", id=" + id + ", @("
                + lat + ", " + lon + "), v=" + speedMetersPerSecond +")";
    }
}
