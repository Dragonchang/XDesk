package com.deepblue.log;

import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

public class DeepBlueLogThread extends Thread{
    private static final int MINUTE = 60 * 1000;
    private static final long DAY = 24 * 60 * 60 * 1000;

    // 发送缓存队列
    private ConcurrentLinkedQueue<Action> mCacheLogQueue;
    private final Object sync = new Object();
    private volatile boolean mIsRun = true;
    private boolean mIsWorking;

    private String mCachePath; // 缓存文件路径
    private String mPath; //文件路径
    private long mSaveTime; //存储时间
    private long mMaxLogFile;//最大文件大小
    private long mMinSDCard;
    private SimpleDateFormat mDataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private DeepBlueNativeLog mDeepBlueNativeLog;

    private int mInitCode;
    private int mOpenCode;

    private long mCurrentDay;
    private boolean mIsSDCard;
    private long mLastTime;
    private int mFileSection;

    DeepBlueLogThread(
            ConcurrentLinkedQueue<Action> cacheLogQueue, String cachePath,
            String path, long saveTime, long maxLogFile, long minSDCard) {
        mCacheLogQueue = cacheLogQueue;
        mCachePath = cachePath;
        mPath = path;
        mSaveTime = saveTime;
        mMaxLogFile = maxLogFile;
        mMinSDCard = minSDCard;
        mLastTime = System.currentTimeMillis();
        mIsSDCard = true;
    }


    void notifyRun() {
        if (!mIsWorking) {
            synchronized (sync) {
                sync.notify();
            }
        }
    }

    @Override
    public void run() {
        super.run();
        while (mIsRun) {
            synchronized (sync) {
                mIsWorking = true;
                try {
                    Action model = mCacheLogQueue.poll();
                    if (model == null) {
                        mIsWorking = false;
                        sync.wait();
                        mIsWorking = true;
                    } else {
                        action(model);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    mIsWorking = false;
                }
            }
        }
    }

    private void action(Action action) {
        if (action == null || !action.isValid()) {
            return;
        }
        if (mDeepBlueNativeLog == null || !(mInitCode == ConstantCode.CLOGAN_INIT_SUCCESS_MMAP
                || mInitCode == ConstantCode.CLOGAN_INIT_SUCCESS_MEMORY
                || mInitCode == ConstantCode.CLOGAN_INIT_SUCESS_ALREADY)) {
            mDeepBlueNativeLog = DeepBlueNativeLog.newInstance();
            mInitCode = mDeepBlueNativeLog.logan_init(mCachePath, mPath, (int) mMaxLogFile);
        }
        if(mInitCode == ConstantCode.CLOGAN_INIT_SUCCESS_MMAP
            || mInitCode == ConstantCode.CLOGAN_INIT_SUCCESS_MEMORY
            || mInitCode == ConstantCode.CLOGAN_INIT_SUCESS_ALREADY) {
            if (action.mAction == Action.WRITE) {
                doWriteLog2File((WriteAction)action);
            } else if (action.mAction == Action.FLUSH) {
                doFlushLog2File();
            }
        } else {
            Log.e("DeepBlueLogThread", "init failed with code: "+mInitCode);
        }
    }


    private void doWriteLog2File(WriteAction action) {
        if (!isDay()) {
            mFileSection = 0;
            long tempCurrentDay = Util.getCurrentTime();
            //save时间
            long deleteTime = tempCurrentDay - mSaveTime;
            deleteExpiredFile(deleteTime);//删除5天之前的log
            mCurrentDay = tempCurrentDay;
            mOpenCode = mDeepBlueNativeLog.logan_open(Util.getDateStr(mCurrentDay)+"-"+mCurrentDay + "-" + mFileSection);
        }
        if(mOpenCode != ConstantCode.CLOGAN_OPEN_SUCCESS) {
            mOpenCode = mDeepBlueNativeLog.logan_open(Util.getDateStr(mCurrentDay)+"-"+mCurrentDay + "-" + mFileSection);
            if(mOpenCode != ConstantCode.CLOGAN_OPEN_SUCCESS) {
                Log.e("DeepBlueLogThread", "logan_open failed with code: "+mOpenCode);
                return;
            }
        }
        long currentTime = System.currentTimeMillis(); //每隔2分钟判断一次
        if (currentTime - mLastTime > (2*MINUTE)) {
            doFlushLog2File();
            mIsSDCard = isCanWriteSDCard();
            mLastTime = System.currentTimeMillis();
        }

        if (!mIsSDCard) { //如果大于50M 不让再次写入
            Log.e("DeepBlueLogThread","sd card 小于50M！");
            return;
        }
        String logPattern = mDataFormat.format(new Date(action.localTime))
                + " [" + action.pid +"/"+ action.threadId + ": " + action.threadName
                + "] " + Util.getLogType(action.flag) + " ";
        if(!TextUtils.isEmpty(action.log)) {
            action.log = logPattern + action.log;
            write(action);

        } else if(action.exception != null) {
            action.log = logPattern + action.exception.toString();
            write(action);
            for(StackTraceElement traceElement :action.exception.getStackTrace()) {
                action.log = logPattern + traceElement.toString();
                write(action);
            }
        }
    }

    private void write(WriteAction action) {
        int code = mDeepBlueNativeLog.logan_write(action.flag, action.log, action.localTime, action.threadName,
                action.threadId, action.isMainThread);
        if(code == ConstantCode.CLOAGN_WRITE_FAIL_MAXFILE) {
            do {
                Log.e("DeepBlueLogThread","write file more than 10M mFileSection:"+mFileSection);
                mFileSection = mFileSection + 1;
                mOpenCode = mDeepBlueNativeLog.logan_open(Util.getDateStr(mCurrentDay) + "-" + mCurrentDay + "-" + mFileSection);
                code = mDeepBlueNativeLog.logan_write(action.flag, action.log, action.localTime, action.threadName,
                        action.threadId, action.isMainThread);
            } while (code == ConstantCode.CLOAGN_WRITE_FAIL_MAXFILE);
        }
    }
    /*
     *是否在一天之内
     * return ture 是在一天之内/false 超过一天或者第一次打开
     */
    private boolean isDay() {
        long currentTime = System.currentTimeMillis();
        return mCurrentDay < currentTime && mCurrentDay + DAY > currentTime;
    }

    private void deleteExpiredFile(long deleteTime) {
        File dir = new File(mPath);
        if (dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                for (String item : files) {
                    try {
                        if (TextUtils.isEmpty(item)) {
                            continue;
                        }
                        String[] longStrArray = item.split("\\.");
                        if (longStrArray.length > 0) {  //小于时间就删除
                            String[] fileMillsName = longStrArray[0].split("-");
                            if(fileMillsName != null && fileMillsName.length>1){
                                long longItem = Long.valueOf(fileMillsName[1]);
                                if (longItem <= deleteTime) {
                                    new File(mPath, item).delete(); //删除文件
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean isCanWriteSDCard() {
        boolean item = false;
        try {
            StatFs stat = new StatFs(mPath);
            long blockSize = stat.getBlockSizeLong();
            long availableBlocks = stat.getAvailableBlocksLong();
            long total = availableBlocks * blockSize;
            if (total > mMinSDCard) { //判断SDK卡
                item = true;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return item;
    }

    private void doFlushLog2File() {
        if (mDeepBlueNativeLog != null) {
            mDeepBlueNativeLog.logan_flush();
        }
    }
}
