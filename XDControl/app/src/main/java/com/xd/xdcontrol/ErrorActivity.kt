package com.xd.xdcontrol

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import com.xd.xdcontrol.base.BaseActivity
import com.xd.xdcontrol.databinding.ActivityErrorBinding
import com.xd.xdcontrol.databinding.ActivityMainBinding

class ErrorActivity : BaseActivity<ActivityErrorBinding>() {
    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityErrorBinding {
        return ActivityErrorBinding.inflate(layoutInflater)
    }

    override fun loadData() {
    }

    override fun initView(savedInstanceState: Bundle?) {}
}