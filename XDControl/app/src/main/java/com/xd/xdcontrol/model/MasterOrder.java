package com.xd.xdcontrol.model;

import com.google.gson.annotations.SerializedName;

public class MasterOrder {
    @SerializedName("switch")
    private int isEnabled;

    @SerializedName("value")
    private Float value;

    public MasterOrder(int isEnabled, Float value) {
        this.isEnabled = isEnabled;
        this.value = value;
    }

    public int getIsEnabled() {
        return isEnabled;
    }

    public Float getValue() {
        return value;
    }
}
