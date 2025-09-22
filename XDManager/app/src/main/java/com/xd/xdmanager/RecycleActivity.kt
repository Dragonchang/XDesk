package com.xd.xdmanager

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.wug.framew.factory.mToast
import com.xd.xdmanager.adapter.SeatAdapter
import com.xd.xdmanager.adapter.SeatAdapterSure
import com.xd.xdmanager.base.BaseActivity
import com.xd.xdmanager.base.BaseAdapter
import com.xd.xdmanager.base.XDApplication
import com.xd.xdmanager.config.WManager
import com.xd.xdmanager.databinding.ActivityRecycleBinding
import com.xd.xdmanager.frame.Frame
import com.xd.xdmanager.frame.YesOrNODialog
import com.xd.xdmanager.model.DataModel
import com.xd.xdmanager.model.ListStatus
import com.xd.xdmanager.model.StatusData

class RecycleActivity : BaseActivity<ActivityRecycleBinding>() {
    private var mDataList = ArrayList<StatusData>()
    private lateinit var mAdapter: SeatAdapterSure
    var mHandler = Handler()
    private val adialog: YesOrNODialog by lazy {
        YesOrNODialog(mContext)
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityRecycleBinding = ActivityRecycleBinding.inflate(layoutInflater)

    override fun loadData() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.ivRecycBack.setOnClickListener(this)
        binding.btnRecycForce.setOnClickListener(this)
        binding.btnRecycSure.setOnClickListener(this)
        binding.tvRecycEstop.setOnClickListener(this)
        ListStatus.getInstance(mContext).dataLiveData.observe(this) { datalist: List<StatusData> ->
            mDataList.clear()
            mDataList.addAll(datalist)
            mAdapter.notifyDataSetChanged()
        }

        //先发80013 让client确认
        var tempList = XDApplication.mApplication.mqttService?.getConnectedClients()
        if (tempList != null && tempList.isNotEmpty()) {
            val model = DataModel(tempList, 80013)
            Frame.HANDLES.sentAll(10001, Gson().toJson(model))
        } else {
            mToast("当前没有设备连接,3秒后自动关闭页面")
            mHandler.postDelayed({ finish() }, 3000)
        }

        initRecycleView()
    }

    private fun initRecycleView() {
        val layoutManager = GridLayoutManager(mContext, 4)
        binding.recycRecyc.layoutManager = layoutManager
        initAdapter()
    }

    private fun initAdapter() {
        mAdapter = SeatAdapterSure(mContext, mDataList, R.layout.item_seat_close)
        binding.recycRecyc.adapter = mAdapter
        mAdapter.notifyDataSetChanged()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_recyc_back -> {
                var tempList = XDApplication.mApplication.mqttService?.getConnectedClients()
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, 80016)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else {
                    mToast("当前没有设备连接")
                }
                finish()
            }
            R.id.btn_recyc_force -> {
                adialog.run {
                    this.setSingleBtn2("强制执行将产生不可控因素\n建议先检查学生环境，是否需要继续强制执行", "确认")
                    this.setOnclickListener { v ->
                        if (v.id == R.id.iv_YesOrNo_close) {
                            dismiss()
                        }
                        if (v.id == R.id.tv_YesOrNo_center2) {
                            var tempList = XDApplication.mApplication.mqttService?.getConnectedClients()
                            if (tempList != null && tempList.isNotEmpty()) {
                                val model = DataModel(tempList, 80017)
                                Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                            } else mToast("当前没有设备连接")
                            dismiss()
                        }
                    }
                    this.show()
                }
            }
            R.id.tv_recyc_estop -> {
                var tempList = XDApplication.mApplication.mqttService?.getConnectedClients()
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, 80012)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else {
                    mToast("当前没有设备连接")
                }
            }
            R.id.btn_recyc_sure -> {//TODO  判断已连接设备 是否都确认，然后再发80017

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler.removeCallbacksAndMessages(null)
    }
}