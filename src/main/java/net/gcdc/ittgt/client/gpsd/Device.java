package net.gcdc.ittgt.client.gpsd;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

public class Device {
    @SerializedName("class")
    String clazz = "DEVICE";
    Date activated;
    String path;
}
