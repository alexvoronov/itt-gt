package net.gcdc.ittgt.client;

import java.util.Arrays;

public class SimulinkWorld {

    @Override public String toString() {
        return "SimulinkWorld (vehicles:" + Arrays.toString(vehicles) + ")";
    }

    public SimulinkGt[] vehicles;

}
