package com.deepblue.log;
import android.util.Log;

public class DebugLog {
    private static boolean isDebug = true;
    public static void v(String tag, String log) {
        if(isDebug && log != null) Log.v(tag, log);
    }

    public static void d(String tag, String log) {
        if(isDebug && log != null)  Log.d(tag, log);
    }

    public static void i(String tag, String log) {
        if(isDebug && log != null)  Log.i(tag, log);
    }

    public static void w(String tag, String log) {
        if(isDebug && log != null)  Log.w(tag, log);
    }

    public static void e(String tag, String log) {
        if(isDebug && log != null)  Log.e(tag, log);
    }

    public static void t(Throwable e) {
        if(isDebug && e != null) {
            e.printStackTrace();
        }
    }
}
