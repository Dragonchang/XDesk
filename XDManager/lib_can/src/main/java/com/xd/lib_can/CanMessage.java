package com.xd.lib_can;


public class CanMessage {
    public long m_canid; //messageID 29bit长度
    public long m_eff = 1; //默认为扩展帧
    public long m_rtr = 0; //默认为数据帧
    public int m_len = 8; //数据长度为8个字节
    public byte[] m_data;

    public CanMessage(long canid,  byte[] data) {
        m_canid = canid;
        m_data = data;
    }
    @Override
    public String toString() {
        return "{canid:" + String.format("0x%X", m_canid) +
                ",eff:" + m_eff +
                ",rtr:" + m_rtr +
                ",len:" + m_len +
                ",data: " +Util.bytesToHex(m_data) +
                "}";
    }
}
