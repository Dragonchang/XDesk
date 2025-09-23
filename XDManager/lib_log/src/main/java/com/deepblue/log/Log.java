package com.deepblue.log;

public class Log {
    public static void v(String tag, String log) {
        DebugLog.v(tag, log);
        DeepBlueLog.w(tag+"： "+log, DeepBlueLog.V);
    }

    public static void d(String tag, String log) {
        DebugLog.d(tag, log);
        DeepBlueLog.w(tag+"： "+log, DeepBlueLog.D);
    }

    public static void i(String tag, String log) {
        DebugLog.i(tag, log);
        DeepBlueLog.w(tag+"： "+log, DeepBlueLog.I);
    }

    public static void w(String tag, String log) {
        DebugLog.w(tag, log);
        DeepBlueLog.w(tag+"： "+log, DeepBlueLog.W);
    }

    public static void e(String tag, String log) {
        DebugLog.e(tag, log);
        DeepBlueLog.w(tag+"： "+log, DeepBlueLog.E);
    }

    public static void t(Throwable e) {
        DebugLog.t(e);
        DeepBlueLog.printTrace(e, DeepBlueLog.T);
    }

    public static void c(Throwable e) {
        DebugLog.t(e);
        DeepBlueLog.printTrace(e, DeepBlueLog.C);
    }
}
