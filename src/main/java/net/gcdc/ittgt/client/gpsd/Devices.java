package net.gcdc.ittgt.client.gpsd;

import com.google.gson.annotations.SerializedName;

public class Devices {
    @SerializedName("class")
    String clazz = "DEVICES";
    Device[] devices;
}
