package com.xd.xdmanager

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.wug.framew.factory.mToast
import com.xd.xdmanager.base.BaseActivity
import com.xd.xdmanager.databinding.ActivityMainBinding
import com.xd.xdmanager.fragment.ViewPagerAdapter
import com.xd.xdmanager.model.DataModel
import com.xd.xdmanager.model.JsonParser
import com.xd.xdmanager.rtc.ImsCallback
import com.xd.xdmanager.rtc.SignalServerManager
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : BaseActivity<ActivityMainBinding>(), ImsCallback {
    private var indicatorTargetWidth = 0f
    private var isFirst = true
    var mTimeHander = Handler()
    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val weekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())

    override fun loadData() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        val buttons = listOf(binding.btnLift, binding.btnPower, binding.btnVideo)
        binding.mainViewpage.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        binding.mainViewpage.adapter = ViewPagerAdapter(this)

        postInitializeIndicator()
        mTimeHander.post(timeUpdater)

        binding.btnLift.setOnClickListener {
            handleViewSwitch(binding.btnLift)
            moveIndicatorToButton(binding.btnLift, 300)
            binding.mainViewpage.setCurrentItem(0, true)
        }
        binding.btnPower.setOnClickListener {
            handleViewSwitch(binding.btnPower)
            moveIndicatorToButton(binding.btnPower, 300)
            binding.mainViewpage.setCurrentItem(1, true)
        }
        binding.btnVideo.setOnClickListener {
            handleViewSwitch(binding.btnVideo)
            moveIndicatorToButton(binding.btnVideo, 300)
            binding.mainViewpage.setCurrentItem(2, true)
        }

        handleViewSwitch(binding.btnLift)
        binding.mainViewpage.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (isFirst) {
                    isFirst = false
                    return
                }
                handleViewSwitch(buttons[position])
                moveIndicatorToButton(buttons[position], 300)
            }
        })

        startImsServices()

        binding.mainWarn.setOnClickListener {
            startActivity(Intent(this, ErrorActivity::class.java))
        }

        binding.ivNavigationStart.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        binding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}
            override fun onDrawerOpened(drawerView: View) {
                binding.ivNavigationStart.setImageResource(R.drawable.ic_baseline_chevron_left_24)
            }

            override fun onDrawerClosed(drawerView: View) {
                binding.ivNavigationStart.setImageResource(R.drawable.ic_baseline_chevron_right_24)
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    override fun disposeMsg(type: Int, obj: Any) {
        super.disposeMsg(type, obj)
        when (type) {
            10001 -> {
                val model: DataModel? = JsonParser.parseJsonToDataModel(obj.toString())
                model?.takeIf {
                    it.clientIds?.isNotEmpty() == true && it.type in 80001..80099
                }?.let { data ->
                    val command = data.type - 80000
                    when (command) {
                        8 -> robotApp.mqttService?.sendCommandToAll(data.clientIds, command, value = data.value)
                        14, 15 -> robotApp.mqttService?.sendCommandToAll(data.clientIds, command, data.isEnabled, data.value)
                        17 -> {//TODO  强制回收  去除x1y2 id
                            val filteredIds = data.clientIds.filter { it != "x1y2" }
                            robotApp.mqttService?.sendCommandToAll(filteredIds, command)
                        }
                        else -> robotApp.mqttService?.sendCommandToAll(data.clientIds, command)
                    }
                }

            }
        }
    }

    override fun onClick(v: View) {
    }

    private fun handleViewSwitch(selectedButton: TextView) {
        listOf(binding.btnLift, binding.btnPower, binding.btnVideo).forEach {
            it.isSelected = it == selectedButton
        }
    }

    private fun postInitializeIndicator() {
        binding.buttonContainer.post {
            indicatorTargetWidth = binding.buttonContainer.width / 3f - 100
            binding.indicator.layoutParams.width = indicatorTargetWidth.toInt()
            val targetX = binding.btnLift.left + (binding.btnLift.width - indicatorTargetWidth) / 2
            binding.indicator.translationX = targetX
            binding.indicator.requestLayout()
        }
    }

    private fun moveIndicatorToButton(target: View, duration: Long) {
        val targetX = target.left + (target.width - indicatorTargetWidth) / 2

        binding.indicator.animate().translationX(targetX).scaleX(1.2f).setInterpolator(OvershootInterpolator()).setDuration(duration).start()
    }

    fun updateTimeDisplay() {
        val calendar = Calendar.getInstance()

        runOnUiThread {
            binding.tvMainTime.text = timeFormat.format(calendar.time)
            binding.tvMainWeek.text = weekFormat.format(calendar.time)
            binding.tvMainDate.text = dateFormat.format(calendar.time)
        }
    }

    private val timeUpdater = object : Runnable {
        override fun run() {
            updateTimeDisplay()
            mTimeHander.postDelayed(this, 60000) // 60秒间隔
        }
    }

    /**
     * 启动信令服务
     */
    private fun startImsServices() {
        SignalServerManager.INSTANCE(this).registerImsCallback(this)
        SignalServerManager.INSTANCE(this).start()
        Log.d("MainActivity", "=== startImsServices*********************SignalServer start$this")
    }


    override fun refeshClent() {
        Log.d("MainActivity", "=== refeshClent")
        runOnUiThread {
            binding.tvWarnNum.visibility = View.VISIBLE
        }
//        val clients = SignalServerManager.INSTANCE(this).clientList
//        dataList.clear()
//        dataList.addAll(clients)
//        runOnUiThread { adapter.notifyDataSetChanged() }
    }

    override fun onRemoteAnswerReceived(message: JSONObject?) {
    }

    override fun onRemoteCandidateReceived(message: JSONObject?) {
    }

    override fun onHangup(reason: String?) {
    }

    override fun onDestroy() {
        super.onDestroy()
        SignalServerManager.INSTANCE(this).unRegisterImsConnectCallBack(this)
        Log.d("MainActivity", "=== onDestroy*********************SignalServer stop$this")
        SignalServerManager.INSTANCE(this).stop()
        mTimeHander.removeCallbacks(timeUpdater)
    }
}