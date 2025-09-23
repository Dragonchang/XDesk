package com.deepblue.log;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * A watchdog timer thread that detects when the UI thread has frozen.
 */
@SuppressWarnings("UnusedReturnValue")
public class ANRWatchDog extends Thread {

    public interface ANRListener {
        /**
         * Called when an ANR is detected.
         *
         * @param error The error describing the ANR.
         */
        void onAppNotResponding(ANRError error);
    }

    public interface ANRInterceptor {
        /**
         * Called when main thread has froze more time than defined by the timeout.
         *
         * @param duration The minimum time (in ms) the main thread has been frozen (may be more).
         * @return 0 or negative if the ANR should be reported immediately. A positive number of ms to postpone the reporting.
         */
        long intercept(long duration);
    }

    public interface InterruptionListener {
        void onInterrupted(InterruptedException exception);
    }

    private static final int DEFAULT_ANR_TIMEOUT = 5000;

    private static final ANRInterceptor DEFAULT_ANR_INTERCEPTOR = new ANRInterceptor() {
        @Override public long intercept(long duration) {
            return 0;
        }
    };

    private static final InterruptionListener DEFAULT_INTERRUPTION_LISTENER = new InterruptionListener() {
        @Override public void onInterrupted(InterruptedException exception) {
            Log.w("ANRWatchdog", "Interrupted: " + exception.getMessage());
        }
    };

    private ANRListener _anrListener = null;
    private ANRInterceptor _anrInterceptor = DEFAULT_ANR_INTERCEPTOR;
    private InterruptionListener _interruptionListener = DEFAULT_INTERRUPTION_LISTENER;

    private final Handler _uiHandler = new Handler(Looper.getMainLooper());
    private final int _timeoutInterval;

    private volatile long _tick = 0;
    private volatile boolean _reported = false;

    private final Runnable _ticker = new Runnable() {
        @Override public void run() {
            _tick = 0;
            _reported = false;
        }
    };

    /**
     * Constructs a watchdog that checks the ui thread every given interval
     *
     * @param timeoutInterval The interval, in milliseconds, between to checks of the UI thread.
     *                        It is therefore the maximum time the UI may freeze before being reported as ANR.
     */
    public ANRWatchDog(int timeoutInterval) {
        super();
        _timeoutInterval = timeoutInterval;
    }

    /**
     * @return The interval the WatchDog
     */
    public int getTimeoutInterval() {
        return _timeoutInterval;
    }

    /**
     * Sets an interface for when an ANR is detected.
     * If not set, the default behavior is to throw an error and crash the application.
     *
     * @param listener The new listener or null
     * @return itself for chaining.
     */
    public ANRWatchDog setANRListener(ANRListener listener) {
        if (listener == null) {
            _anrListener = null;
        } else {
            _anrListener = listener;
        }
        return this;
    }

    /**
     * Sets an interface to intercept ANRs before they are reported.
     * If set, you can define if, given the current duration of the detected ANR and external context, it is necessary to report the ANR.
     *
     * @param interceptor The new interceptor or null
     * @return itself for chaining.
     */
    public ANRWatchDog setANRInterceptor(ANRInterceptor interceptor) {
        if (interceptor == null) {
            _anrInterceptor = DEFAULT_ANR_INTERCEPTOR;
        } else {
            _anrInterceptor = interceptor;
        }
        return this;
    }

    /**
     * Sets an interface for when the watchdog thread is interrupted.
     * If not set, the default behavior is to just log the interruption message.
     *
     * @param listener The new listener or null.
     * @return itself for chaining.
     */
    public ANRWatchDog setInterruptionListener(InterruptionListener listener) {
        if (listener == null) {
            _interruptionListener = DEFAULT_INTERRUPTION_LISTENER;
        } else {
            _interruptionListener = listener;
        }
        return this;
    }

    @Override
    public void run() {
        setName("WatchDog-Anr");

        long interval = _timeoutInterval;
        while (!isInterrupted()) {
            boolean needPost = _tick == 0;
            _tick += interval;
            if (needPost) {
                _uiHandler.post(_ticker);
            }

            try {
                Thread.sleep(interval);
            } catch (InterruptedException e) {
                _interruptionListener.onInterrupted(e);
                return ;
            }

            // If the main thread has not handled _ticker, it is blocked. ANR.
            if (_tick != 0 && !_reported) {
                interval = _anrInterceptor.intercept(_tick);
                if (interval > 0) {
                    continue;
                }

                final ANRError error =  new ANRError(_tick);
                _anrListener.onAppNotResponding(error);
                interval = _timeoutInterval;
                _reported = true;
            }
        }
    }

}
