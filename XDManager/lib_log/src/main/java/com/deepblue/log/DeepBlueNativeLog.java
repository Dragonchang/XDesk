
package com.deepblue.log;

public class DeepBlueNativeLog {
    private static volatile DeepBlueNativeLog sDeepBlueCLog;

    private static final String LIBRARY_NAME = "clogan";

    private static boolean sIsCloganOk;
    private boolean mIsLoganInit;
    private boolean mIsLoganOpen;


    static {
        try {
            if (!Util.loadLibrary(LIBRARY_NAME, DeepBlueNativeLog.class)) {
                System.loadLibrary(LIBRARY_NAME);
            }
            sIsCloganOk = true;
        } catch (Throwable e) {
            e.printStackTrace();
            sIsCloganOk = false;
        }
    }

    static DeepBlueNativeLog newInstance() {
        if (sDeepBlueCLog == null) {
            synchronized (DeepBlueNativeLog.class) {
                if (sDeepBlueCLog == null) {
                    sDeepBlueCLog = new DeepBlueNativeLog();
                }
            }
        }
        return sDeepBlueCLog;
    }

    public native int native_init(String cache_path, String dir_path, int max_file);
    public native int native_open(String file_name);
    public native int native_write(int flag, String log, long local_time, String thread_name,
                                    long thread_id, int is_main);
    public native void native_flush();


    public int logan_init(String cache_path, String dir_path, int max_file) {
        if (mIsLoganInit) {
            return ConstantCode.CLOGAN_INIT_SUCESS_ALREADY;
        }
        if (!sIsCloganOk) {
            return ConstantCode.CLOGAN_LOAD_SO_FAIL;
        }
        int code= ConstantCode.CLOGAN_LOAD_SO_FAIL;
        try {
            code = native_init(cache_path, dir_path, max_file);
            mIsLoganInit = true;
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
        return code;
    }

    public int logan_open(String file_name) {
        int code = ConstantCode.CLOGAN_LOAD_SO_FAIL;
        if (!mIsLoganInit || !sIsCloganOk) {
            return code;
        }
        try {
            code = native_open(file_name);
            mIsLoganOpen = true;
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
        return code;
    }

    public int logan_write(int flag, String log, long local_time, String thread_name,
                            long thread_id, boolean is_main) {
        int code = ConstantCode.CLOGAN_LOAD_SO_FAIL;
        if (!mIsLoganOpen || !sIsCloganOk) {
            return code;
        }
        try {
            int isMain = is_main ? 1 : 0;
            code = native_write(flag, log, local_time, thread_name, thread_id,
                    isMain);
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
        return code;
    }

    public void logan_flush() {
        if (!mIsLoganOpen || !sIsCloganOk) {
            return;
        }
        try {
            native_flush();
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }
    }
}

