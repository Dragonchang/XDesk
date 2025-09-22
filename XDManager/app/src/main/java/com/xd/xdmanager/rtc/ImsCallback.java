package com.xd.xdmanager.rtc;

import org.json.JSONObject;

public interface ImsCallback {
    void refeshClent();
    void onRemoteAnswerReceived(JSONObject message);
    void onRemoteCandidateReceived(JSONObject message);
    void onHangup(String reason);
}
