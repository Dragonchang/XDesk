package com.deepblue.log;
import android.text.TextUtils;

public class WriteAction extends Action{

    WriteAction() {
        mAction = Action.WRITE;
    }

    String log; //日志
    long localTime;
    boolean isMainThread;
    long pid;
    long threadId;
    String threadName = "";
    int flag;
    Throwable exception;

    boolean isValid() {
        boolean valid = false;
        if (!TextUtils.isEmpty(log) || (exception!= null)) {
            valid = true;
        }
        return valid;
    }
}
