package com.xd.xdcontrol.model;

import com.google.gson.Gson;

public class JsonParser {
    public static MasterOrder parseJsonToDataModel(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, MasterOrder.class);
    }
}
