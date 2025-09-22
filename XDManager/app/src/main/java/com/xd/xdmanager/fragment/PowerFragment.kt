package com.xd.xdmanager.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.wug.framew.factory.mToast
import com.xd.xdmanager.R
import com.xd.xdmanager.RecycleActivity
import com.xd.xdmanager.adapter.SeatAdapter
import com.xd.xdmanager.base.BaseAdapter
import com.xd.xdmanager.base.BaseFragment
import com.xd.xdmanager.base.XDApplication
import com.xd.xdmanager.config.WManager
import com.xd.xdmanager.databinding.FragmentPowerBinding
import com.xd.xdmanager.frame.DecimalDigitsInputFilter
import com.xd.xdmanager.frame.Frame
import com.xd.xdmanager.frame.NumberRangeValidator
import com.xd.xdmanager.frame.YesOrNODialog
import com.xd.xdmanager.model.DataModel
import com.xd.xdmanager.model.ListStatus
import com.xd.xdmanager.model.StatusData
import java.util.ArrayList


class PowerFragment : BaseFragment<FragmentPowerBinding>() {
    var acOrDc = 0   // 0 没选，  1 ac  ，2  dc
    var validator: NumberRangeValidator? = null
    var minV = 0.0
    var maxV = 0.0
    private var mDataList = ArrayList<StatusData>()
    private lateinit var mAdapter: SeatAdapter

    override fun inflateBinding(inflater: LayoutInflater): FragmentPowerBinding = FragmentPowerBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        binding.tvPowerAc.setOnClickListener(this)
        binding.tvPowerDc.setOnClickListener(this)
        binding.etPowerV.isEnabled = false
        binding.llLowvDo.setOnClickListener(this)
        binding.tvPowerOpen220.setOnClickListener(this)
        binding.tvPowerClose220.setOnClickListener(this)
        binding.llPowerLock.setOnClickListener(this)
        binding.llPowerClose.setOnClickListener(this)
        binding.etPowerV.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter())
        binding.checkboxSelectAll2power.setOnCheckedChangeListener { buttonView, isChecked ->
            ListStatus.getInstance(mContext).updateAllDataCheck(isChecked)
        }
        validator = NumberRangeValidator(binding.etPowerV)

        ListStatus.getInstance(mContext).dataLiveData.observe(this) { datalist: List<StatusData> ->
            mDataList.clear()
            mDataList.addAll(datalist)
            mAdapter.notifyDataSetChanged()
        }
        ListStatus.getInstance(context).allLockLiveData.observe(this) { isLocked: Boolean ->
            if (isLocked) {
                binding.tvPowerLockText.text = "一键解锁"
            } else {
                binding.tvPowerLockText.text = "一键锁屏"
            }
        }
        initRecycleView()
    }

    private fun initRecycleView() {
        val layoutManager = GridLayoutManager(mContext, WManager.instance(mContext).yNum)
        binding.recycPower.layoutManager = layoutManager
        initAdapter()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initAdapter() {
        mAdapter = SeatAdapter(mContext, mDataList, R.layout.item_seat)
        binding.recycPower.adapter = mAdapter
        mAdapter.notifyDataSetChanged()
        mAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                if (mDataList[position].isConnected) {
                    mDataList[position].isCheck = true
                    mAdapter.notifyDataSetChanged()
                }
            }
        })
    }

    override fun loadData() {
    }

    override fun disposeMsg(type: Int, obj: Any?) {
    }

    //TODO 模拟数据
    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv_power_ac -> {
                binding.tvPowerAc.isSelected = true
                binding.tvPowerDc.isSelected = false
                binding.etPowerV.isEnabled = true
                acOrDc = 1
                minV = 2.0
                maxV = 24.0
                validator?.setRange(minV, maxV)
            }
            R.id.tv_power_dc -> {
                binding.tvPowerAc.isSelected = false
                binding.tvPowerDc.isSelected = true
                binding.etPowerV.isEnabled = true
                acOrDc = 2
                minV = 1.5
                maxV = 24.0
                validator?.setRange(minV, maxV)
            }
            R.id.ll_lowv_do -> {
                if (acOrDc == 0) {
                    mToast("请选择交流电还是直流电")
                    return
                }
                var lowPowerV = binding.etPowerV.text.toString()
                if (lowPowerV.isEmpty()) {
                    mToast("请输入低压电压")
                    return
                }
                var temp = lowPowerV.toFloat()
                if (temp !in minV..maxV) {
                    mToast("请输入正确低压电压")
                    return
                }

                var tempList = ListStatus.getInstance(mContext).checkClientids
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, if (acOrDc == 1) 80015 else 80014, true, temp)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else mToast("请选择设备")
            }
            R.id.tv_power_open220 -> {
                var tempList = ListStatus.getInstance(mContext).checkClientids
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, 80006)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else mToast("请选择设备")
            }
            R.id.tv_power_close220 -> {
                var tempList = ListStatus.getInstance(mContext).checkClientids
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, 80007)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else mToast("请选择设备")
            }
            R.id.ll_power_close -> {
                startActivity(Intent(mContext, RecycleActivity::class.java))
            }
            R.id.ll_power_lock -> {
                var tempList = XDApplication.mApplication.mqttService?.getConnectedClients()
                if (tempList != null && tempList.isNotEmpty()) {

                    var isAllLock = ListStatus.getInstance(context).allLock
                    val model = DataModel(tempList, if (isAllLock) 80002 else 80001)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                    ListStatus.getInstance(context).allLock = !isAllLock
                } else mToast("当前没有设备连接")

            }
        }
    }

}