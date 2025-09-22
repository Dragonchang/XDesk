package com.xd.xdcontrol.fragment

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import com.wug.framew.factory.mToast
import com.xd.xdcontrol.R
import com.xd.xdcontrol.base.BaseFragment
import com.xd.xdcontrol.databinding.FragmentSecondBinding
import com.xd.xdcontrol.net.OrderManager
import com.xd.xdcontrol.net.PowerController
import com.xd.xdcontrol.net.StatusParser
import java.util.*


class SecondFragment : BaseFragment<FragmentSecondBinding>() {
    private val mFHandler: Handler = Handler()
    private var mUpdateTask: Runnable? = null
    private var mCurrentVoltage = 0.0
    private var mLastChangeTime: Long = 0
    private val INITIAL_DELAY = 300L // 首次触发延迟
    private val NORMAL_DELAY = 100L // 常规触发间隔
    private val ACCELERATE_FACTOR = 2L // 加速系数
    private val DELTA_ADD = 0.1
    private val DELTA_JIAN = -0.1
    private val TIMEOUT = 5000L

    override fun loadData() {
    }

    override fun inflateBinding(inflater: LayoutInflater): FragmentSecondBinding = FragmentSecondBinding.inflate(inflater)

    @SuppressLint("ClickableViewAccessibility")
    override fun initView(savedInstanceState: Bundle?) {
        binding.rlSecAc.setOnClickListener(this)
        binding.rlSecDc.setOnClickListener(this)
        binding.tvSecJian.setOnClickListener(this)
        binding.tvSecAdd.setOnClickListener(this)

        StatusParser.getInstance().powerStatusLiveData.observe(this) { powerStatus: StatusParser.PowerStatus ->
            updatePowerStatusUI(powerStatus)
        }
        setupButton(R.id.tv_sec_jian, DELTA_JIAN)
        setupButton(R.id.tv_sec_add, DELTA_ADD)

        binding.switchSec220.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                OrderManager.getInstance().sendCommand(0x04, 0x00.toByte(), 0x00.toByte(), 2)
                StatusParser.getInstance().updateIsOn220AC(true)
            } else {
                OrderManager.getInstance().sendCommand(0x05, 0x00.toByte(), 0x00.toByte(), 2)
                StatusParser.getInstance().updateIsOn220AC(false)
            }
        }
        StatusParser.getInstance().isOn220ACLiveData.observe(this) { tag: Boolean ->
            binding.switchSec220.isChecked = tag
        }
    }

    private fun setupButton(buttonId: Int, delta: Double) {
        val button = if (buttonId == R.id.tv_sec_jian) {
            binding.tvSecJian
        } else {
            binding.tvSecAdd
        }
        val handler = VoltageLongPressHandler(delta)

        // 关键点：设置tag携带处理器
        button.tag = handler

        button.setOnLongClickListener {
            handler.startLongPress()
            true
        }

        // 根据图片交互需求添加触摸监听
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    (v.tag as? LongPressHandler)?.stopLongPress()
                    // 根据图片样式可能需要重置按钮状态
                    v.animate().scaleX(1f).scaleY(1f).start()
                }
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(1.4f).scaleY(1.4f).start()
                }
            }
            false
        }

    }

    private fun updatePowerStatusUI(status: StatusParser.PowerStatus) {
        binding.rlSecDc.isSelected = status.isDCon
        binding.ivSecDc.isSelected = status.isDCon
        binding.rlSecAc.isSelected = status.isACon
        binding.ivSecAc.isSelected = status.isACon
        if (status.isDCon) {
            binding.voltageText.text = status.dcVoltage.toString() + " V"
            mCurrentVoltage = status.dcVoltage.toDouble()
        } else if (status.isACon) {
            binding.voltageText.text = status.acVoltage.toString() + " V"
            mCurrentVoltage = status.acVoltage.toDouble()
        } else {
            binding.voltageText.text = "0 V"
            mCurrentVoltage = 0.0
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv_sec_jian -> {
                adjustVoltage(DELTA_JIAN)
            }
            R.id.tv_sec_add -> {
                adjustVoltage(DELTA_ADD)
            }
            R.id.rl_sec_dc -> {
                PowerController.getInstance().adjustDC(!StatusParser.getInstance().powerStatus.isDCon, 12, 0)
                StatusParser.getInstance().updatePowerStatus(false, 0f, !StatusParser.getInstance().powerStatus.isDCon, 12f)
            }
            R.id.rl_sec_ac -> {
                PowerController.getInstance().adjustAC(!StatusParser.getInstance().powerStatus.isACon, 12)
                StatusParser.getInstance().updatePowerStatus(!StatusParser.getInstance().powerStatus.isACon, 12f, false, 0f)
            }
        }
    }

    override fun disposeMsg(type: Int, obj: Any?) {
    }

    var index = 0
    var maxV = 0.0
    var minV = 0.0

    @Synchronized
    private fun adjustVoltage(delta: Double) {
        if (StatusParser.getInstance().powerStatus.isACon) {
            index = 10
            maxV = 2.0
            minV = 24.0
        }
        if (StatusParser.getInstance().powerStatus.isDCon) {
            index = 1
            maxV = 1.5
            minV = 24.0
        }
        mCurrentVoltage = (mCurrentVoltage + delta * index).coerceIn(maxV, minV)
        val voltageText: String = java.lang.String.format(Locale.US, "%.1f V", mCurrentVoltage)
        binding.voltageText.text = voltageText
        if (mCurrentVoltage == minV || mCurrentVoltage == minV) {
            binding.voltageText.setBackgroundColor(Color.parseColor("#4D000000"))
        } else {
            binding.voltageText.background = null
        }
        resetTimeoutTimer()
        mLastChangeTime = System.currentTimeMillis()
    }

    private fun resetTimeoutTimer() {
        if (mUpdateTask != null) {
            mFHandler.removeCallbacks(mUpdateTask!!)
        }
        mUpdateTask = Runnable {
            val elapsed = System.currentTimeMillis() - mLastChangeTime
            if (elapsed >= TIMEOUT) {
                if (StatusParser.getInstance().powerStatus.isACon) {
                    PowerController.getInstance().adjustAC(true, mCurrentVoltage.toInt())
                    StatusParser.getInstance().updatePowerStatus(true, mCurrentVoltage.toFloat(), false, 0f)
                    mToast("AC电压设置完成")
                }
                if (StatusParser.getInstance().powerStatus.isDCon) {
                    PowerController.getInstance().adjustDC(true, mCurrentVoltage.toInt(), ((mCurrentVoltage - mCurrentVoltage.toInt()) * 10).toInt())
                    StatusParser.getInstance().updatePowerStatus(false, 0f, true, mCurrentVoltage.toFloat())
                    mToast("DC电压设置完成")
                }
            }
        }
        mFHandler.postDelayed(mUpdateTask!!, TIMEOUT)
    }

    interface LongPressHandler {
        fun startLongPress()  // 对应图片中的长按+/-操作
        fun stopLongPress()   // 对应松开手指时的状态
        val pressAction: Runnable // 新增访问器
    }

    private inner class VoltageLongPressHandler(
        private val delta: Double
    ) : LongPressHandler {
        private var currentDelay = INITIAL_DELAY
        override val pressAction = object : Runnable {
            override fun run() {

                adjustVoltage(
                    delta * (if (StatusParser.getInstance().powerStatus.isDCon) 10
                    else 1)
                )
                currentDelay = Math.max(NORMAL_DELAY, currentDelay / ACCELERATE_FACTOR)
                mFHandler.postDelayed(this, currentDelay)
            }
        }

        override fun startLongPress() {
            mFHandler.postDelayed(pressAction, INITIAL_DELAY)
        }

        override fun stopLongPress() {
            mFHandler.removeCallbacks(pressAction)
            currentDelay = INITIAL_DELAY
        }
    }
}