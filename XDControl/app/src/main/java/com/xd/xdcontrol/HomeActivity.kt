package com.xd.xdcontrol

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.Observer
import androidx.viewpager2.widget.ViewPager2
import com.wug.framew.factory.mToast
import com.xd.xdcontrol.base.BaseActivity
import com.xd.xdcontrol.config.WManager
import com.xd.xdcontrol.databinding.ActivityHomeBinding
import com.xd.xdcontrol.fragment.ViewPagerAdapter
import com.xd.xdcontrol.model.JsonParser
import com.xd.xdcontrol.model.MasterOrder
import com.xd.xdcontrol.mqtt.MqttManager
import com.xd.xdcontrol.net.OrderManager
import com.xd.xdcontrol.net.PowerController
import com.xd.xdcontrol.net.RS485Controller
import com.xd.xdcontrol.net.StatusParser
import com.xd.xdcontrol.rtc.SignalClientManager


class HomeActivity : BaseActivity<ActivityHomeBinding>() {
    private val TAG = "HomeActivity"
    private var isBigMotorDown: Boolean = true
    private var mqttManager: MqttManager? = null
    private var x = 0
    private var y = 0
    private var mClient: SignalClientManager? = null
    private var canStop = false

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityHomeBinding {
        return ActivityHomeBinding.inflate(layoutInflater)
    }

    override fun loadData() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        x = WManager.instance(mContext).x
        y = WManager.instance(mContext).y
        StatusParser.getInstance().updateClientId("x${x}y${y}")
        StatusParser.getInstance().updateIP(WManager.instance(mContext).ip)

        binding.homeViewpage.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.homeViewpage.adapter = ViewPagerAdapter(this)

        binding.homeViewpage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
            }
        })
        StatusParser.getInstance().workStatusLiveData.observe(this) { workStatus: StatusParser.WorkStatus ->
            isBigMotorDown = workStatus.topMotorFalling
        }

        initMqtt()
        initSignal()
    }

    private fun initSignal() {
        /** ---------开始连接信令服务-----------  */
        mClient = SignalClientManager.INSTANCE(this)
        mClient?.let {
            it.connect()
            binding.ivHomeCall.setOnClickListener { v: View? ->
                Log.d("HomeActivity", "呼叫老师 ${it.isConnected}")
                if (!it.isConnected) {
                    return@setOnClickListener
                }
                ChatActivity.openActivity(this, true, StatusParser.getInstance().clientId, "laoshi", null)
            }
        }
    }

    override fun disposeMsg(type: Int, obj: Any) {
        super.disposeMsg(type, obj)
        try {
            when (type) {
                90004 -> {
                    var temp = obj.toString().toInt()
                    when (temp) {
                        0 -> mToast("mqtt disconnect")
                        1 -> {
                            mToast("mqtt connected")
                            mqttManager?.let {
                                it.subscribe("command/${StatusParser.getInstance().clientId}")
                            }
                        }
                        2 -> mToast("mqtt error")
                    }
                }
                10004 -> {
                    if (canStop)
                        OrderManager.getInstance().sendCommand(0x03.toByte(), 0x00.toByte(), 0x00.toByte())
                    canStop = false
                }
                80001 -> {//锁屏触发
                    if (robotApp.isAppForeground()) {
                        val currentActivity = robotApp.getCurrentActivity()

                        if (currentActivity != null && currentActivity !is ScreenSaverActivity && !StatusParser.getInstance().lock) {
                            StatusParser.getInstance().updateLockStatus(true)
                            Intent(mContext, ScreenSaverActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                mContext.startActivity(this)
                            }
                        }
                    }
                }
                80003 -> {
                    canStop = true
                    OrderManager.getInstance().sendCommand(0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
                }
                80004 -> {
                    OrderManager.getInstance().sendCommand(0x02.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
                }
                80005 -> {
                    OrderManager.getInstance().sendCommand(0x03.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
                }
                80006 -> {
                    OrderManager.getInstance().sendCommand(0x04.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
                    StatusParser.getInstance().updateIsOn220AC(true)
                }
                80007 -> {
                    OrderManager.getInstance().sendCommand(0x05.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
                    StatusParser.getInstance().updateIsOn220AC(false)
                }
                80008 -> {
//                    try {
//                        OrderManager.getInstance().sendCommand06(obj.toString().toFloat(), 3)
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
                    canStop = true
                    val model: MasterOrder? = JsonParser.parseJsonToDataModel(obj.toString())
                    model?.let {
                        OrderManager.getInstance().sendCommand06(it.value, 3)
                    }
                }
                80009 -> {
                    OrderManager.getInstance().sendCommand(0x09.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
                }
                80010 -> {
                    OrderManager.getInstance().sendCommand(0x10.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
                }
                80011 -> {
                    OrderManager.getInstance().sendCommand(0x11.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
                }
                80012 -> {
                    OrderManager.getInstance().sendCommand(0x12.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
                }
                80013 -> {//TODO  回收确认
//                    OrderManager.getInstance().sendCommand(0x10.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
//                    val tempObserver = object : Observer<StatusParser.WorkStatus> {
//                        override fun onChanged(workStatus: StatusParser.WorkStatus) {
//                            if (workStatus.eggMotorOrigin) {
//                                OrderManager.getInstance().sendCommand(0x02.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
//                                StatusParser.getInstance().workStatusLiveData.removeObserver(this)
//                            }
//                        }
//                    }
//                    StatusParser.getInstance().workStatusLiveData.observe(this, tempObserver)
                }
                80014 -> {
                    val model: MasterOrder? = JsonParser.parseJsonToDataModel(obj.toString())
                    model?.let {
                        PowerController.getInstance().adjustDC(it.isEnabled == 1, it.value.toInt(), ((it.value - it.value.toInt()) * 10).toInt())
                        StatusParser.getInstance().updatePowerStatus(false, 0f, it.isEnabled == 1, it.value)
                    }
                }
                80015 -> {
                    val model: MasterOrder? = JsonParser.parseJsonToDataModel(obj.toString())
                    model?.let {
                        PowerController.getInstance().adjustAC(it.isEnabled == 1, it.value.toInt())
                        StatusParser.getInstance().updatePowerStatus(it.isEnabled == 1, it.value, false, 0f)
                    }
                }
                80016 -> {

                }
                80017 -> {//强制回收
                    OrderManager.getInstance().sendCommand(0x10.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
                    val tempObserver = object : Observer<StatusParser.WorkStatus> {
                        override fun onChanged(workStatus: StatusParser.WorkStatus) {
                            if (workStatus.eggMotorOrigin) {
                                OrderManager.getInstance().sendCommand(0x02.toByte(), 0x00.toByte(), 0x00.toByte(), 3)
                                StatusParser.getInstance().workStatusLiveData.removeObserver(this)
                            }
                        }
                    }
                    StatusParser.getInstance().workStatusLiveData.observe(this, tempObserver)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun initMqtt() {
        mqttManager = MqttManager.getInstance(applicationContext, StatusParser.getInstance().clientId, StatusParser.getInstance().ip)
        mqttManager?.connect()
    }


    override fun onDestroy() {
        super.onDestroy()
//        rs485Controller?.close()
        mClient!!.close()
        mqttManager?.disconnect()
        OrderManager.getInstance().close()
        RS485Controller.getInstance().close()
        PowerController.getInstance().close()
    }
}