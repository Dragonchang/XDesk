package com.xd.lib_can;


public class CanMessage {
    public long m_canid;
    public long m_eff;
    public long m_rtr;
    public int m_len;
    public int[] m_data;

    public CanMessage(long canid, long eff, long rtr, int len, int[] data) {
        m_canid = canid;
        m_eff = eff;
        m_rtr = rtr;
        m_len = len;
        m_data = data;
    }
    @Override
    public String toString() {
        return "{canid:" + m_canid +
                ",eff:" + m_eff +
                ",rtr:" + m_rtr +
                ",len:" + m_len +
                ",data:{}" +Util.intArrayToHexString(m_data) +
                "}";
    }
}
