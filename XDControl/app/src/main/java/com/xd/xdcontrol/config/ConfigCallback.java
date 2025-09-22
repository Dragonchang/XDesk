package com.xd.xdcontrol.config;

/**
 * @author wg
 */
public interface ConfigCallback {
    String getTcpServiceIpHost();

    void setTcpServiceIpHost(String host);

    int getTcpServiceIpPort();

    void setTcpServiceIpPort(int port);

    float getSafeDistance();

    void setSafeDistance(float safeStr);

    float getSafeDistance2();

    void setSafeDistance2(float safeStr);

    int getWifiNum();

    void setWifiNum(int wifi_num);

    void setMode(int host);

    int getMode();

    String getDeviceId();

    void setDeviceId(String deviceId);

    String getIP();

    void setIP(String ip);

    String getNetType();

    void setNetType(String nettype);

    int getX();

    void setX(int x);

    int getY();

    void setY(int y);
}
