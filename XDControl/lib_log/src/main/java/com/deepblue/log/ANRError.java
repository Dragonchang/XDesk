package com.deepblue.log;

import android.os.Looper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

public class ANRError {

    final Thread mMainThread = Looper.getMainLooper().getThread();
    final Map<Thread, StackTraceElement[]> mStackTraces = new TreeMap<Thread, StackTraceElement[]>(new Comparator<Thread>() {
        @Override
        public int compare(Thread lhs, Thread rhs) {
            if (lhs == rhs)
                return 0;
            if (lhs == mMainThread)
                return -1;
            if (rhs == mMainThread)
                return -1;
            return rhs.getName().compareTo(lhs.getName());
        }
    });


    /**
     * The minimum duration, in ms, for which the main thread has been blocked. May be more.
     */
    @SuppressWarnings("WeakerAccess")
    public final long duration;

    public ANRError(long duration) {
        this.duration = duration;

        for (Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet()) {
            if (entry.getKey() == mMainThread
                    || (entry.getValue().length > 0)) {
                mStackTraces.put(entry.getKey(), entry.getValue());
            }
        }

        // Sometimes main is not returned in getAllStackTraces() - ensure that we list it
        if (!mStackTraces.containsKey(mMainThread)) {
            mStackTraces.put(mMainThread, mMainThread.getStackTrace());
        }
    }

    public void writeStackToFile( FileOutputStream mFileOutput) throws IOException{
        try {
            String writeStr = "----- pid "+android.os.Process.myPid() +" at "+ new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date(System.currentTimeMillis()))+" -----\n";
            mFileOutput.write(writeStr.getBytes());
            writeStr = "main thread block cost :"+duration+"\n";
            mFileOutput.write(writeStr.getBytes());
            for (Map.Entry<Thread, StackTraceElement[]> entry : mStackTraces.entrySet()) {
                writeStr = "   \n";
                mFileOutput.write(writeStr.getBytes());
                Thread thread = entry.getKey();
                if(thread != null) {
                    writeStr = "\"" + thread.getName() + "\" "
                            + " prio=" +thread.getPriority()
                            +" tid=" +thread.getId()
                            +" status="+thread.getState()+"\n";
                    mFileOutput.write(writeStr.getBytes());
                    for(StackTraceElement traceElement :entry.getValue()) {
                        writeStr = "  at "+traceElement.toString()+"\n";
                        mFileOutput.write(writeStr.getBytes());
                    }
                    writeStr = "   \n";
                    mFileOutput.write(writeStr.getBytes());
                }
            }
        } catch (IOException e) {
            throw e;
        }
    }
}