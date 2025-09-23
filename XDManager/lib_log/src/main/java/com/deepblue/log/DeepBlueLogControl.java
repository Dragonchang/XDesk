package com.deepblue.log;

import android.os.Looper;
import android.text.TextUtils;

import java.util.concurrent.ConcurrentLinkedQueue;

public class DeepBlueLogControl {

    private final static int THREAD_NAME_LENGTH = 13;
    private static volatile DeepBlueLogControl sLoganControlCenter;

    private String mCachePath; // 缓存文件路径
    private String mPath; //文件路径
    private long mSaveTime; //存储时间
    private long mMaxLogFile;//最大文件大小
    private long mMinSDCard;
    private long mMaxQueue; //最大队列数
    private long mPid;

    private DeepBlueLogThread mLogThread;
    private DeepBlueMemoryMonitor mDeepBlueMemoryMonitor;
    private ANRCatcher mANRCatcher;
    private ConcurrentLinkedQueue<Action> mCacheLogQueue = new ConcurrentLinkedQueue<>();

    static DeepBlueLogControl instance(DeepBlueLogConfig config) {
        if (sLoganControlCenter == null) {
            synchronized (DeepBlueLogControl.class) {
                if (sLoganControlCenter == null) {
                    sLoganControlCenter = new DeepBlueLogControl(config);
                }
            }
        }
        return sLoganControlCenter;
    }

    private DeepBlueLogControl(DeepBlueLogConfig config) {
        if (!config.isValid()) {
            throw new NullPointerException("config's param is invalid");
        }

        mPath = config.mPathPath;
        mCachePath = config.mCachePath;
        mSaveTime = config.mDay;
        mMinSDCard = config.mMinSDCard;
        mMaxLogFile = config.mMaxFile;
        mMaxQueue = config.mMaxQueue;
        mPid = android.os.Process.myPid();
        init();
    }
    private void init() {
        if (mLogThread == null) {
            mLogThread = new DeepBlueLogThread(mCacheLogQueue, mCachePath, mPath, mSaveTime,
                    mMaxLogFile, mMinSDCard);
            mLogThread.setName("log-thread");
            mLogThread.start();
        }
        if (mDeepBlueMemoryMonitor == null) {
            mDeepBlueMemoryMonitor = new DeepBlueMemoryMonitor(mPath);
        }
        mANRCatcher = new ANRCatcher(mPath);
    }

    void dumpHprof(boolean isSync) {
        if(isSync) {
            mDeepBlueMemoryMonitor.syncDumpHprof();
        } else {
            mDeepBlueMemoryMonitor.asyncDumpHprof();
        }
    }

    void dumpFD(boolean isSync) {
        if(isSync) {
            mDeepBlueMemoryMonitor.syncDumpFD();
        } else {
            mDeepBlueMemoryMonitor.asyncDumpFD();
        }
    }

    void write(String log, int flag) {
        if (TextUtils.isEmpty(log)) {
            return;
        }
        WriteAction action = new WriteAction();
        long threadLog = Thread.currentThread().getId();
        boolean isMain = false;
        if (Looper.getMainLooper() == Looper.myLooper()) {
            isMain = true;
        }
        action.log = log;
        action.localTime = System.currentTimeMillis();
        action.flag = flag;
        action.isMainThread = isMain;
        action.threadId = threadLog;
        action.pid = mPid;
        action.threadName = getThreadName();
        mCacheLogQueue.add(action);
        if (mLogThread != null) {
            mLogThread.notifyRun();
        }
    }

     String getThreadName() {
        String threadName = Thread.currentThread().getName();
        if(!TextUtils.isEmpty(threadName)) {
            if(threadName.length() < THREAD_NAME_LENGTH ) {
                int left = THREAD_NAME_LENGTH - threadName.length();
                StringBuilder str = new StringBuilder(threadName);
                do{
                    str.append(" ");
                    --left;
                } while (left > 0);
                threadName = str.toString();
            } else {
                threadName = threadName.substring(0,THREAD_NAME_LENGTH);
            }

        }
        return threadName;
    }

    void writeTrace(Throwable e, int flag) {
        if (e == null) {
            return;
        }
        WriteAction action = new WriteAction();
        long threadLog = Thread.currentThread().getId();
        boolean isMain = false;
        if (Looper.getMainLooper() == Looper.myLooper()) {
            isMain = true;
        }
        action.exception = e;
        action.localTime = System.currentTimeMillis();
        action.flag = flag;
        action.isMainThread = isMain;
        action.threadId = threadLog;
        action.pid = mPid;
        action.threadName = getThreadName();
        mCacheLogQueue.add(action);
        if (mLogThread != null) {
            mLogThread.notifyRun();
        }
    }

    void flush() {
        if (TextUtils.isEmpty(mPath)) {
            return;
        }
        FlushAction model = new FlushAction();
        mCacheLogQueue.add(model);
        if (mLogThread != null) {
            mLogThread.notifyRun();
        }
    }

}


