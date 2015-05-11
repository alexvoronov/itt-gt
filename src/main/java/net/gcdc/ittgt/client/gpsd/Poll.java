package net.gcdc.ittgt.client.gpsd;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

public class Poll {
    /** Message class, always String "POLL". */
    @SerializedName("class")
    String   clazz = "POLL";
    /** Timestamp in ISO 8601 format. May have a fractional part of up to .001sec precision. */
    Date time;
    /** Count of active devices. */
    int active;
    /** Comma-separated list of TPV objects. */
    TPV[] tpv;
}
