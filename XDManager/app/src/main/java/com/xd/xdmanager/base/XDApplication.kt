package com.xd.xdmanager.base

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.StrictMode
import android.util.Log
import com.wug.framew.base.FBaseApplication
import com.xd.xdmanager.BuildConfig
import com.xd.xdmanager.frame.Frame
import com.xd.xdmanager.service.MqttBrokerService
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
    var mqttService: MqttBrokerService? = null
        private set
    private var serviceConnection: ServiceConnection? = null
    private var bindCount = 0 // 绑定计数器

    override fun onCreate() {
        super.onCreate()
        mApplication = this
        mContext = applicationContext
        Frame.init(applicationContext)

        initHandleBug()

        startService(Intent(this, MqttBrokerService::class.java))
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

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()  // 仅记录日志，不崩溃
                    .build()
            )
        }
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

    fun bindMqttService(callback: (MqttBrokerService?) -> Unit) {
        if (serviceConnection == null) {
            serviceConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val localBinder = binder as MqttBrokerService.LocalBinder
                    mqttService = localBinder.getService()
                    callback(mqttService)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    mqttService = null
                }
            }
        }

        bindCount++
        // 绑定服务（如果未绑定）
        if (mqttService == null) {
            val intent = Intent(this, MqttBrokerService::class.java)
            bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
        } else {
            callback(mqttService)
        }
    }

    fun unbindMqttService() {
        bindCount--
        if (bindCount == 0 && serviceConnection != null) {
            unbindService(serviceConnection!!)
        }
    }

}