package net.gcdc.ittgt.client.gpsd;

import com.google.gson.annotations.SerializedName;

public class Watch {
    @Override
    public String toString() {
        return "WATCH [enable=" + enable + ", json=" + json + ", nmea=" + nmea + ", remote="
                + remote + "]";
    }
    /** Message class, always String "WATCH". */
    @SerializedName("class")
    String   clazz = "WATCH";
    /** Enable (true) or disable (false) watcher mode. Default is true. */
    boolean enable = true;
    /** Enable (true) or disable (false) dumping of JSON reports. Default is false. */
    boolean json = true;
    /** Enable (true) or disable (false) dumping of binary packets as pseudo-NMEA. Default is false. */
    boolean nmea = false;
    /** URL of the remote daemon reporting the watch set. If empty, this is a WATCH response from the local daemon. */
    String remote;
}
