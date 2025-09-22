package com.xd.xdmanager.config;

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

    String getSoftVersion();

    void setSoftVersion(String softVersion);

    String getHardVersion();

    void setHardVersion(String hardVersion);

    String getDeviceId();

    int getXNum();

    void setXNum(int xNum_);

    void setYNum(int yNum);

    int getYNum();

    void setDeviceId(String deviceId);

    float getHeight1();

    void setHeight1(float height1);

    float getHeight2();

    void setHeight2(float height2);

    float getHeight3();

    void setHeight3(float height3);
}
