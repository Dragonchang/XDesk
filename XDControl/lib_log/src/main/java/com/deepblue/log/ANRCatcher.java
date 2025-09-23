package com.deepblue.log;

import android.os.FileObserver;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ANRCatcher {
    private static String TAG = "ANRCatcher";
    private static String mSystemAnrFile = "/data/anr";
    private static String ANR_PATH = "/anr";
    private static String ANR_HEADER = "ANR-";
    private static String PREFIX = ".log";
    TraceFileObserver mTraceFileObserver;
    ANRWatchDog mAnrWatchDog;
    int duration = 4;
    private String mPath;
    FileOutputStream mFileOutput = null;
    private SimpleDateFormat mDataFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    private long mCurrentDay;
    private long mSaveTime; //存储时间
    private static final long DAYS = 24 * 60 * 60 * 1000; //天
    private class TraceFileObserver extends FileObserver {
        public TraceFileObserver() {
            super(mSystemAnrFile, CLOSE_WRITE );
        }

        @Override
        public void onEvent(int event, String path) {
            if (path == null) {
                return;
            }
            Log.d(TAG,"TraceFileObserver : "+event+" path: "+path);
        }
    }


    public  ANRCatcher(String path) {
        mPath = path + ANR_PATH;
        File file = new File(mPath);
        if(!file.exists()) {
            file.mkdir();
        }
        mSaveTime = 5 * DAYS;
        //在6.0以上使用anr watchdog
        mAnrWatchDog = new ANRWatchDog(2000);
        mAnrWatchDog.setANRListener(new ANRWatchDog.ANRListener() {
            @Override
            public void onAppNotResponding(ANRError error) {
                Log.e(TAG, "Detected Application Not Responding!");
                ensureRootPath();
                mCurrentDay = Util.getCurrentTime();
                deleteExpiredANRFile(mCurrentDay - mSaveTime);
                String fileName = ANR_HEADER + mDataFormat.format(new Date(System.currentTimeMillis()))+"-"+mCurrentDay+PREFIX;
                String anrFile = mPath + File.separator+fileName;
                try {
                    File file = new File(anrFile);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    mFileOutput = new FileOutputStream(file);
                    error.writeStackToFile(mFileOutput);
                    Log.i(TAG, "Error was successfully write to: "+anrFile);
                } catch (IOException e) {
                    Log.t(e);
                } finally {
                    Util.closeQuietly(mFileOutput);
                }
            }
        })
        .setANRInterceptor(new ANRWatchDog.ANRInterceptor() {
            @Override
            public long intercept(long dur) {
                long ret = duration * 1000 - dur;
                if (ret > 0)
                    Log.w(TAG, "Intercepted ANR that is too short (" + dur + " ms), postponing for " + ret + " ms.");
                return ret;
            }
        });
        mAnrWatchDog.start();
        //在6.0以下使用file observer来监听anr
        if (mTraceFileObserver == null) {
            mTraceFileObserver = new TraceFileObserver();
            mTraceFileObserver.startWatching();
        }
    }

    private void ensureRootPath() {
        File file = new File(mPath);
        if(!file.exists()) {
            file.mkdir();
        }
    }

    private void deleteExpiredANRFile(long deleteTime) {
        File dir = new File(mPath);
        if (dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                for (String item : files) {
                    if(!TextUtils.isEmpty(item)
                            &&item.contains(ANR_HEADER)) {
                        String[] longStrArray = item.split("\\.");
                        if (longStrArray != null && longStrArray.length > 0) {  //小于时间就删除
                            String[] fileMillsName = longStrArray[0].split("-");
                            if(fileMillsName != null && fileMillsName.length == 3) {
                                long longItem = Long.valueOf(fileMillsName[2]);
                                if(longItem <= deleteTime) {
                                    new File(mPath, item).delete(); //删除文件
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
