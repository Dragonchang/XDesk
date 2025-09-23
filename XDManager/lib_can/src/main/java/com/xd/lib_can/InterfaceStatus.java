package com.xd.lib_can;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum InterfaceStatus {
    INTERFACE_STATUS_UP(1, "up"),
    INTERFACE_STATUS_DOWN(2, "down");

    private static final Map<Integer, String> VALUES_MAP;

    static {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (InterfaceStatus each : values()) {
            map.put(each.getCode(), each.getName());
        }
        VALUES_MAP = Collections.unmodifiableMap(map);
    }

    private Integer code;
    private String name;

    InterfaceStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public Integer getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static Map<Integer, String> toMap() {
        return VALUES_MAP;
    }

    public static InterfaceStatus getEnumByCode(Integer code) {
        for (InterfaceStatus each : values()) {
            if (each.getCode().equals(code)) {
                return each;
            }
        }
        return null;
    }

    public static String getNameByCode(Integer code) {
        InterfaceStatus en = getEnumByCode(code);
        if (null != en) {
            return en.getName();
        }
        return null;
    }
}
