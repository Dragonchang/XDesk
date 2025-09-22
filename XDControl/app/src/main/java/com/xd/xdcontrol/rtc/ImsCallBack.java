package com.xd.xdcontrol.rtc;

import org.json.JSONObject;

public interface ImsCallBack {
    void onRemoteCandidateReceived(JSONObject message);
    void onRemoteAnswerReceived(JSONObject message);
    void onHangup(String reason);
}
