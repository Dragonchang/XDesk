package com.xd.xdmanager.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class DataModel {
    @SerializedName(value = "clientIds")
    private List<String> clientIds;

    @SerializedName("type")
    private int type;

    @SerializedName("switch")
    private Boolean isEnabled;

    @SerializedName("value")
    private Float value;

    public DataModel(List<String> clientIds, int type) {
        if (clientIds == null || clientIds.isEmpty()) {
            throw new IllegalArgumentException("clientIds不能为空");
        }
        this.clientIds = clientIds;
        this.type = type;
    }

    public DataModel(List<String> clientIds, int type, Boolean isEnabled, Float value) {
        this(clientIds, type);
        this.isEnabled = isEnabled;
        this.value = value;
    }

    public boolean validate() {
        return clientIds != null && !clientIds.isEmpty() && type > 0;
    }

    public List<String> getClientIds() {
        return clientIds;
    }

    public int getType() {
        return type;
    }

    public boolean getIsEnabled() {
        return isEnabled != null ? isEnabled : false;
    }

    public void setIsEnabled(Boolean enabled) {
        isEnabled = enabled;
    }

    public float getValue() {
        return value != null ? value : 0.0f;
    }

    public void setValue(Float value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "DataModel{" +
                "clientIds=" + clientIds +
                ", type=" + type +
                ", isEnabled=" + isEnabled +
                ", value=" + value +
                '}';
    }
}