package com.xd.xdmanager

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import com.xd.xdmanager.base.BaseActivity
import com.xd.xdmanager.databinding.ActivityErrorBinding

class ErrorActivity : BaseActivity<ActivityErrorBinding>() {
    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityErrorBinding = ActivityErrorBinding.inflate(layoutInflater)

    override fun loadData() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.ivErrorBack.setOnClickListener { finish() }
    }
}