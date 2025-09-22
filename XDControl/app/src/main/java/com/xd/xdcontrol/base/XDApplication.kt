package com.xd.xdcontrol.base

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.wug.framew.base.FBaseApplication
import com.xd.xdcontrol.frame.Frame
import com.xd.xdcontrol.net.OrderManager
import com.xd.xdcontrol.net.PowerController
import com.xd.xdcontrol.net.RS485Controller
import com.xd.xdcontrol.net.StatusParser
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*


class XDApplication : FBaseApplication() {
    companion object {
        lateinit var mApplication: XDApplication
        lateinit var mContext: Context
    }

    private var currentActivityRef: WeakReference<Activity>? = null
    private var activityCounter = 0

    override fun onCreate() {
        super.onCreate()
        mApplication = this
        mContext = applicationContext
        Frame.init(applicationContext)

        initHandleBug()
        StatusParser.getInstance()

        doAsync {
            PowerController.getInstance().init()
            RS485Controller.getInstance().init()
            OrderManager.getInstance().init()
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                currentActivityRef = WeakReference(activity)
                activityCounter++
            }

            override fun onActivityPaused(activity: Activity) {
                activityCounter--
                if (currentActivityRef != null && currentActivityRef!!.get() == activity) {
                    currentActivityRef!!.clear();
                    currentActivityRef = null;
                }
            }

            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun initHandleBug() {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("UncaughtException", "Unhandled exception caught!", throwable)
            saveCrashLog(throwable)
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun saveCrashLog(throwable: Throwable) {
        Log.e("SaveCrashLog", "Saving crash log")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val logFileName = "crash_log_$currentDate.txt"
        val logFile = File(getExternalFilesDir(null), logFileName)
        FileWriter(logFile, true).use { writer ->
            PrintWriter(writer).use { printWriter ->
                printWriter.println("Crash at: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}")
                throwable.printStackTrace(printWriter)
                printWriter.println()
            }
        }
        Log.e("SaveCrashLog", "Crash log saved: ${logFile.absolutePath}")
    }

    fun getCurrentActivity(): Activity? = currentActivityRef?.get()
    fun isAppForeground(): Boolean = activityCounter > 0
}