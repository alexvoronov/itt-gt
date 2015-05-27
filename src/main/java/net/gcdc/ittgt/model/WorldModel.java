package net.gcdc.ittgt.model;

import java.util.Arrays;
import java.util.Date;

public class WorldModel {
    @Override public String toString() {
        return "WorldModel [header=" + header + ", vehicles=" + Arrays.toString(vehicles) + "]";
    }

    public Header header;
    public Vehicle[] vehicles;

    public static class Header {
        @Override public String toString() {
            return "Header [version=" + version + ", samplingTimeMs=" + samplingTimeMs
                    + ", simulationTimestamp=" + simulationTimestamp + "]";
        }
        public int version;
        public int samplingTimeMs;
        public Date simulationTimestamp;  // Serialize to JSON as ISO 8601.
    }
}
