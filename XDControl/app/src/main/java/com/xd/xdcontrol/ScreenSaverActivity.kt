package com.xd.xdcontrol

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import com.xd.xdcontrol.base.BaseActivity
import com.xd.xdcontrol.databinding.ActivityScreenSaverBinding
import com.xd.xdcontrol.net.StatusParser

class ScreenSaverActivity : BaseActivity<ActivityScreenSaverBinding>() {

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityScreenSaverBinding = ActivityScreenSaverBinding.inflate(layoutInflater)

    override fun loadData() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        StatusParser.getInstance().isOn220ACLiveData.observe(this) { tag: Boolean ->
            binding.ivScreenAc220.isSelected = tag
        }
        StatusParser.getInstance().powerStatusLiveData.observe(this) { powerStatus: StatusParser.PowerStatus ->
            updatePowerStatusUI(powerStatus)
        }
    }

    private fun updatePowerStatusUI(status: StatusParser.PowerStatus) {
        if (StatusParser.getInstance().powerStatus.isACon) {
            binding.tvScreenAcdc.text = "AC"
            binding.tvScreenVoltage.text = StatusParser.getInstance().powerStatus.acVoltage.toString() + " V"
        } else if (StatusParser.getInstance().powerStatus.isDCon) {
            binding.tvScreenAcdc.text = "DC"
            binding.tvScreenVoltage.text = StatusParser.getInstance().powerStatus.dcVoltage.toString() + " V"
        } else {
            binding.tvScreenAcdc.text = "OFF"
        }
    }

    override fun disposeMsg(type: Int, obj: Any) {
        super.disposeMsg(type, obj)
        when (type) {
            80002 -> {
                StatusParser.getInstance().updateLockStatus(false)
                finish()
            }
        }
    }
}