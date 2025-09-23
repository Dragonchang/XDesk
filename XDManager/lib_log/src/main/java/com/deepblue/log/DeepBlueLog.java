package com.deepblue.log;

public class DeepBlueLog {

    public static final int V = 0x1;
    public static final int D = 0x2;
    public static final int I = 0x3;
    public static final int W = 0x4;
    public static final int E = 0x5;
    public static final int T = 0x6;
    public static final int C = 0x7;
    private static DeepBlueLogControl sLoganControlCenter;

    public static void init(DeepBlueLogConfig loganConfig) {
        sLoganControlCenter = DeepBlueLogControl.instance(loganConfig);
        CrashHandler.getInstance().init();
    }

    /**
     * @param log  表示日志内容
     * @param type 表示日志类型
     * @brief DeepBlueLog写入日志
     */
    public static void w(String log, int type) {
        if (sLoganControlCenter == null) {
            return;
        }
        sLoganControlCenter.write(log, type);
    }

    /**
     * @param e  表示日志内容
     * @param type 表示日志类型
     * @brief DeepBlueLog写入日志
     */
    public static void printTrace(Throwable e, int type) {
        if (sLoganControlCenter == null) {
            return;
        }
        sLoganControlCenter.writeTrace(e, type);
    }
    /**
     * @brief 立即写入日志文件
     */
    public static void f() {
        if (sLoganControlCenter == null) {
            return;
        }
        sLoganControlCenter.flush();
    }

    /**
     * @brief 立即写入日志文件
     */
    public static void dumpHprof(boolean isSync) {
        if (sLoganControlCenter == null) {
            return;
        }
        sLoganControlCenter.dumpHprof(isSync);
    }

    /**
     * @brief 立即写入日志文件
     */
    public static void dumpFD(boolean isSync) {
        if (sLoganControlCenter == null) {
            return;
        }
        sLoganControlCenter.dumpFD(isSync);
    }
}
