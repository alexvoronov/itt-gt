package net.gcdc.grotr.ittgt.gpsd;

import com.google.gson.annotations.SerializedName;

public class Error {
    /** Message class, always String "ERROR". */
    @SerializedName("class")
    String   clazz = "ERROR";
    String message;
}
