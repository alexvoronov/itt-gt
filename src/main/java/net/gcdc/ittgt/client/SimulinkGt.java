package net.gcdc.ittgt.client;

public class SimulinkGt {
    public double x;
    public double y;
    public double psi;
    public double psidot;
    public double v;
    public double a;
    public double rearAxleToRearBumper;
    public double vehicleLength;
    public double vehicleWidth;
    public double vehicleId;

    @Override public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SimulinkGt (x=");
        builder.append(x);
        builder.append(", y=");
        builder.append(y);
        builder.append(", psi=");
        builder.append(psi);
        builder.append(", psidot=");
        builder.append(psidot);
        builder.append(", v=");
        builder.append(v);
        builder.append(", a=");
        builder.append(a);
        builder.append(", rearAxleToRearBumper=");
        builder.append(rearAxleToRearBumper);
        builder.append(", vehicleLength=");
        builder.append(vehicleLength);
        builder.append(", vehicleWidth=");
        builder.append(vehicleWidth);
        builder.append(", vehicleId=");
        builder.append(vehicleId);
        builder.append(")");
        return builder.toString();
    }

}