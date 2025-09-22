package com.xd.xdcontrol.rtc;


import org.java_websocket.handshake.ServerHandshake;

public interface ISignalCallBack {
    void onOpen(ServerHandshake handshakedata);

    void onMessage(String message);

    void onClose(int code, String reason, boolean remote);

    void onError(Exception ex);
}
