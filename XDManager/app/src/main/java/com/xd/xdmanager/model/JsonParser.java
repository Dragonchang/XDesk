package com.xd.xdmanager.model;

import com.google.gson.Gson;

public class JsonParser {
    public static DataModel parseJsonToDataModel(String json) {
        Gson gson = new Gson();
        DataModel dataModel = gson.fromJson(json, DataModel.class);
        if (dataModel.validate()) {
            return dataModel;
        } else {
            throw new IllegalArgumentException("Invalid JSON data");
        }
    }
}
