package com.xd.xdcontrol.fragment

import android.animation.*
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.widget.SwitchCompat
import com.wug.framew.factory.mToast
import com.xd.xdcontrol.R
import com.xd.xdcontrol.base.BaseFragment
import com.xd.xdcontrol.databinding.FragmentFirstBinding
import com.xd.xdcontrol.net.OrderManager
import com.xd.xdcontrol.net.PowerController
import com.xd.xdcontrol.net.StatusParser
import com.xd.xdcontrol.net.StatusParser.PowerStatus
import com.xd.xdcontrol.net.StatusParser.WorkStatus


class FirstFragment : BaseFragment<FragmentFirstBinding>() {
    override fun inflateBinding(inflater: LayoutInflater): FragmentFirstBinding = FragmentFirstBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        startBasketballBounce(binding.ivLight)

        binding.rlStuUp.setOnClickListener(this)
        binding.ivStuStop.setOnClickListener(this)
        binding.rlStuDown.setOnClickListener(this)

        StatusParser.getInstance().isOn220ACLiveData.observe(this) { tag: Boolean ->
            binding.ivFirstAc220.isSelected = tag
        }
        StatusParser.getInstance().powerStatusLiveData.observe(this) { powerStatus: PowerStatus ->
            updatePowerStatusUI(powerStatus)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updatePowerStatusUI(status: PowerStatus) {
        if (StatusParser.getInstance().powerStatus.isACon) {
            binding.tvFirstAcdc.text = "AC"
            binding.tvFirstVoltage.text = StatusParser.getInstance().powerStatus.acVoltage.toString() + " V"
        } else if (StatusParser.getInstance().powerStatus.isDCon) {
            binding.tvFirstAcdc.text = "DC"
            binding.tvFirstVoltage.text = StatusParser.getInstance().powerStatus.dcVoltage.toString() + " V"
        } else {
            binding.tvFirstAcdc.text = "OFF"
        }
    }

    override fun loadData() {
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.rl_stu_up -> {
                OrderManager.getInstance().sendCommand(0x10, 0x00.toByte(), 0x00.toByte(), 3)
            }
            R.id.iv_stu_stop -> {
                OrderManager.getInstance().sendCommand(0x11, 0x00.toByte(), 0x00.toByte(), 3)
            }
            R.id.rl_stu_down -> {
                OrderManager.getInstance().sendCommand(0x09, 0x00.toByte(), 0x00.toByte(), 3)
            }
        }

    }

    override fun disposeMsg(type: Int, obj: Any?) {
        when (type) {
            10001 -> {
            }
            10002 -> {

            }
        }
    }

    fun startBasketballBounce(imageView: ImageView) {
        // 基础参数
        val startY = 0f
        val groundY = 30f

        // 单次弹跳动画生成器
        fun createBounceAnimator(peakHeight: Float): AnimatorSet {
            // 下落阶段（加速）
            val dropAnim = ObjectAnimator.ofFloat(imageView, "translationY", startY, groundY).apply {
                duration = 300 * 3
                interpolator = AccelerateInterpolator(2f)
            }

            // 触地挤压（横向拉伸）
            val squeezeX = ObjectAnimator.ofFloat(imageView, "scaleX", 1f, 1.2f)
            val squeezeY = ObjectAnimator.ofFloat(imageView, "scaleY", 1f, 0.8f)
            val squeezeSet = AnimatorSet().apply {
                playTogether(squeezeX, squeezeY)
                duration = 80 * 3
            }

            // 弹起阶段（减速）
            val bounceAnim = ObjectAnimator.ofFloat(imageView, "translationY", peakHeight, groundY).apply {
                duration = 400 * 3
                interpolator = DecelerateInterpolator(2f)
            }

            // 恢复形状
            val resetX = ObjectAnimator.ofFloat(imageView, "scaleX", 1.2f, 1f)
            val resetY = ObjectAnimator.ofFloat(imageView, "scaleY", 0.8f, 1f)
            val resetSet = AnimatorSet().apply {
                playTogether(resetX, resetY)
                duration = 100 * 3
            }

            return AnimatorSet().apply {
                playSequentially(dropAnim, squeezeSet, bounceAnim, resetSet)
            }
        }

        // 组合四次弹跳
        AnimatorSet().apply {
            playSequentially(
                createBounceAnimator(groundY), createBounceAnimator(groundY), createBounceAnimator(groundY), createBounceAnimator(groundY), createBounceAnimator(groundY)
            )
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 最终复位
                    imageView.translationY = startY
                    imageView.scaleX = 1f
                    imageView.scaleY = 1f
                }
            })
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}