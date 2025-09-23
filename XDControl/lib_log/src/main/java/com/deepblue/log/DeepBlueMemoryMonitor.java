package com.deepblue.log;

import android.os.Debug;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.system.Os;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeepBlueMemoryMonitor{
    private static String TAG = "DeepBlueMemoryMonitor";
    private static String HPROF = ".hprof";
    private static String FD = ".log";
    private static String HPROF_HEADER = "HPROF-";
    private static String FD_HEADER = "FD-";
    private static String MEMORY_PATH = "/memory_file";
    private static final long DAYS = 24 * 60 * 60 * 1000; //天
    private static final long M = 1024 * 1024; //M
    private String mPath; //hprof文件保存路径
    private long mCurrentDay;
    private long mSaveTime; //存储时间
    HandlerThread mMemoryMonitorThread;
    MemoryMonitorHandler mMemoryMonitorHandlerr;
    private SimpleDateFormat mDataFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
    private  String mFdPath;
    private int mMaxCount;
    DeepBlueMemoryMonitor(String path) {
        mPath = path + MEMORY_PATH;
        File file = new File(mPath);
        if(!file.exists()) {
            file.mkdir();
        }
        mMemoryMonitorThread = new HandlerThread("MemoryMonitor");
        mMemoryMonitorThread.start();
        mMemoryMonitorHandlerr = new MemoryMonitorHandler(mMemoryMonitorThread.getLooper());
        mCurrentDay = Util.getCurrentTime();
        mSaveTime = 2 * DAYS;
        mFdPath = "/proc/" + android.os.Process.myPid() + "/fd";
        mMaxCount = getMaxOpenFile();
        mMemoryMonitorHandlerr.sendEmptyMessageDelayed(MemoryMonitorHandler.MSG_CHECK_MEMORY, 2000);
    }

    public void asyncDumpHprof() {
        mMemoryMonitorHandlerr.sendEmptyMessage(MemoryMonitorHandler.MSG_DUMP_HPROF);
    }

    public void syncDumpHprof() {
        Object lock = new Object();
        synchronized (lock) {
            Message message = mMemoryMonitorHandlerr.obtainMessage(MemoryMonitorHandler.MSG_DUMP_HPROF);
            message.obj = lock;
            mMemoryMonitorHandlerr.sendMessage(message);
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Log.t(e);
            }
        }
    }

    public void asyncDumpFD() {
        mMemoryMonitorHandlerr.sendEmptyMessage(MemoryMonitorHandler.MSG_DUMP_FD);
    }

    public void syncDumpFD() {
        Object lock = new Object();
        synchronized (lock) {
            Message message = mMemoryMonitorHandlerr.obtainMessage(MemoryMonitorHandler.MSG_DUMP_FD);
            message.obj = lock;
            mMemoryMonitorHandlerr.sendMessage(message);
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Log.t(e);
            }
        }
    }

    private void ensureRootPath() {
        File file = new File(mPath);
        if(!file.exists()) {
            file.mkdir();
        }
    }

    private class MemoryMonitorHandler extends Handler {
        static final int MSG_CHECK_MEMORY = 1;
        static final int MSG_DUMP_HPROF = 2;
        static final int MSG_DUMP_FD = 3;
        MemoryMonitorHandler(Looper looper) {
            super(looper);

        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CHECK_MEMORY: {
                    if(outOfJavaHeap() || outOfOpenFile()) {
                        dumpFD();
                        dumpHprof();
                    } else {
                        mMemoryMonitorHandlerr.sendEmptyMessageDelayed(MemoryMonitorHandler.MSG_CHECK_MEMORY, 2000);
                    }
                }
                break;
                case MSG_DUMP_HPROF: {
                    dumpHprof();
                    Object lock = msg.obj;
                    if(lock != null) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                }
                break;
                case MSG_DUMP_FD: {
                    dumpFD();
                    Object lock = msg.obj;
                    if(lock != null) {
                        synchronized (lock) {
                            lock.notify();
                        }
                    }
                }
                break;
            }
        }
    }

    private boolean outOfJavaHeap() {
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        long totalMemory = rt.totalMemory();
        long freeMemory = rt.freeMemory();
        long availableMemory = maxMemory - totalMemory + freeMemory;
        if(availableMemory < 10*M) {
            Log.e(TAG, "outOfJavaHeap maxMemory:" + maxMemory+" freeMemory:"+freeMemory+" totalMemory:"+totalMemory+" availableMemory: "+availableMemory);
            return true;
        }
        return false;
    }
    private boolean outOfOpenFile() {
        int ret = getFDCount();
        if(ret > mMaxCount - mMaxCount/10) {
            Log.e(TAG,"getFDCount ret: "+ret );
            return true;
        }
        return false;
    }

    private int getFDCount() {
        int ret = 0;
        File file = new File(mFdPath);
        if (file.isDirectory()) {
            File[] subFiles = file.listFiles();
            if (subFiles != null) {
                ret = subFiles.length;
            }
        }
        return ret;
    }

    private int getMaxOpenFile() {
        int maxCount = 1024;
        String maxFile = "/proc/" + android.os.Process.myPid() + "/limits";
        File file = new File(maxFile);
        InputStreamReader read = null;
        try {
            if(file.exists() && file.isFile()) {
                read = new InputStreamReader(
                        new FileInputStream(file), "GBK");//考虑到编码格式
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = null;
                String strTemp = "";
                while ((lineTxt = bufferedReader.readLine()) != null) {
                    if(lineTxt.contains("Max open files")) {
                        Pattern pattern = Pattern.compile("[0-9]");
                        Matcher matcher = pattern.matcher(lineTxt);
                        int lastEnd = 0;
                        boolean first = true;
                        while (matcher.find()) {
                            if(!first && (matcher.end() - lastEnd >1)){
                                break;
                            }
                            first = false;
                            lastEnd = matcher.end();
                            strTemp = strTemp + matcher.group();
                        }
                        Log.i(TAG," strTemp: " + strTemp);
                        if(!TextUtils.isEmpty(strTemp)) {
                            try {
                                return Integer.parseInt(strTemp);
                            } catch (NumberFormatException e){
                                Log.t(e);
                            }
                        }

                   }
                }
            }
        } catch (Exception e) {
            Log.t(e);
        } finally {
            Util.closeQuietly(read);
        }
        return maxCount;
    }

    private void dumpHprof() {
        ensureRootPath();
        mCurrentDay = Util.getCurrentTime();
        deleteExpiredHprofFile(mCurrentDay - mSaveTime);
        String fileName = HPROF_HEADER + mDataFormat.format(new Date(System.currentTimeMillis()))+"-"+mCurrentDay+ HPROF;
        String hprofFile = mPath + File.separator + fileName;
        Log.e(TAG,"dumpHprof********************begin hprofFile: "+hprofFile );
        try {
            Debug.dumpHprofData(hprofFile);
        } catch (Exception e) {
            Log.t(e);
        }
        Log.e(TAG,"dumpHprof*******************end hprofFile: "+hprofFile );
    }

    private void dumpFD() {
        ensureRootPath();
        mCurrentDay = Util.getCurrentTime();
        deleteExpiredFDFile(mCurrentDay - mSaveTime);
        String fileName = FD_HEADER + mDataFormat.format(new Date(System.currentTimeMillis()))+"-"+mCurrentDay + FD;
        String fdFile = mPath + File.separator+fileName;
        Log.e(TAG,"dumpFD********************begin fdFile: "+fdFile );
        File filepath = new File(fdFile);
        FileOutputStream fos = null;
        try {
            if (!filepath.exists()) {
                filepath.createNewFile();
            }
            fos = new FileOutputStream(filepath);
            File file = new File(mFdPath);
            String writeStr = null;
            if (file.isDirectory()) {
                File[] subfiles = file.listFiles();
                if (subfiles != null) {
                    for (File f : subfiles) {
                        if (f.exists()) {
                            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                    .format(new Date(f.lastModified()));
                            writeStr = time + "    " + f.getName()+" -> "+ Os.readlink(f.getAbsolutePath()) + "\n";
                        }
                        if (writeStr != null) {
                            fos.write(writeStr.getBytes());
                        }

                    }
                }
            }
        } catch (Exception e) {
            Log.t(e);
            if (filepath.exists()) {
                filepath.delete();
            }
        } finally {
            Util.closeQuietly(fos);
        }
        Log.e(TAG,"dumpFD*******************end fdFile: "+fdFile );
    }


    private void deleteExpiredHprofFile(long deleteTime) {
        File dir = new File(mPath);
        if (dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                for (String item : files) {
                    if(!TextUtils.isEmpty(item)
                            &&item.contains(HPROF_HEADER)) {
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

    private void deleteExpiredFDFile(long deleteTime) {
        File dir = new File(mPath);
        if (dir.isDirectory()) {
            String[] files = dir.list();
            if (files != null) {
                for (String item : files) {
                    if(!TextUtils.isEmpty(item)
                            &&item.contains(FD_HEADER)) {
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
