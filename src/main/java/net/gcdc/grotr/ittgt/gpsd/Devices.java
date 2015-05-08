package net.gcdc.grotr.ittgt.gpsd;

import com.google.gson.annotations.SerializedName;

public class Devices {
    @SerializedName("class")
    String clazz = "DEVICES";
    Device[] devices;
}
