package net.gcdc.ittgt.model;

import java.util.Date;

public class WorldModel {
    public Header header;
    public Vehicle[] vehicles;

    public static class Header {
        public int version;
        public int samplingTimeMs;
        public Date simulationTimestamp;  // Serialize to JSON as ISO 8601.
    }
}
