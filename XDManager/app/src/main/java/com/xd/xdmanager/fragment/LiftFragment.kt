package com.xd.xdmanager.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.Gson
import com.wug.framew.factory.mToast
import com.xd.xdmanager.ErrorActivity
import com.xd.xdmanager.R
import com.xd.xdmanager.RecycleActivity
import com.xd.xdmanager.adapter.SeatAdapter
import com.xd.xdmanager.base.BaseAdapter
import com.xd.xdmanager.base.BaseFragment
import com.xd.xdmanager.base.XDApplication
import com.xd.xdmanager.config.WManager
import com.xd.xdmanager.databinding.FragmentLiftBinding
import com.xd.xdmanager.frame.Frame
import com.xd.xdmanager.frame.YesOrNODialog
import com.xd.xdmanager.model.DataModel
import com.xd.xdmanager.model.ListStatus
import com.xd.xdmanager.model.StatusData


class LiftFragment : BaseFragment<FragmentLiftBinding>() {
    var lazyHeight1 = 0f
    var lazyHeight2 = 0f
    var lazyHeight3 = 0f
    private var mDataList = ArrayList<StatusData>()
    private lateinit var mAdapter: SeatAdapter

    private val adialog: YesOrNODialog by lazy {
        YesOrNODialog(mContext)
    }
    private val bdialog: YesOrNODialog by lazy {
        YesOrNODialog(mContext)
    }

    override fun loadData() {
    }

    override fun inflateBinding(inflater: LayoutInflater): FragmentLiftBinding = FragmentLiftBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        updateDownUI(WManager.instance(mContext).height1, WManager.instance(mContext).height2, WManager.instance(mContext).height3)

        binding.llLiftEstop.setOnClickListener(this)
        binding.llLiftUp.setOnClickListener(this)
        binding.llLiftDown.setOnClickListener(this)
        binding.tvLiftTop.setOnClickListener(this)
        binding.tvLiftDown1.setOnClickListener(this)
        binding.tvLiftDown2.setOnClickListener(this)
        binding.tvLiftDown3.setOnClickListener(this)
        binding.llLiftClose.setOnClickListener(this)
        binding.llLiftLock.setOnClickListener(this)
        binding.tvLiftSetting.setOnClickListener(this)

        binding.checkboxSelectAll2.setOnCheckedChangeListener { buttonView, isChecked ->
            ListStatus.getInstance(mContext).updateAllDataCheck(isChecked)
        }

        ListStatus.getInstance(mContext).dataLiveData.observe(this) { datalist: List<StatusData> ->
            mDataList.clear()
            mDataList.addAll(datalist)
            mAdapter.notifyDataSetChanged()
        }

        ListStatus.getInstance(context).allLockLiveData.observe(this) { isLocked: Boolean ->
            if (isLocked) {
                binding.tvLiftLockText.text = "一键解锁"
            } else {
                binding.tvLiftLockText.text = "一键锁屏"
            }
        }

        initRecycleView()
    }

    private fun initRecycleView() {
        val layoutManager = GridLayoutManager(mContext, WManager.instance(mContext).yNum)
        binding.recycLift.layoutManager = layoutManager
        initAdapter()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initAdapter() {
        mAdapter = SeatAdapter(mContext, mDataList, R.layout.item_seat)
        binding.recycLift.adapter = mAdapter
        mAdapter.notifyDataSetChanged()
        mAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                if (mDataList[position].isConnected) {
                    mDataList[position].isCheck = !mDataList[position].isCheck
                    mAdapter.notifyDataSetChanged()
                }
            }
        })
    }

    override fun disposeMsg(type: Int, obj: Any?) {
    }


    //TODO 模拟数据
    override fun onClick(v: View) {
        when (v.id) {
            R.id.tv_lift_setting -> {
                adialog.run {
                    this.setEtLayout(lazyHeight1, lazyHeight2, lazyHeight3)
                    this.setOnConfirmListener { v1, v2, v3 ->
                        WManager.instance(mContext).setHeight1(v1)
                        WManager.instance(mContext).setHeight2(v2)
                        WManager.instance(mContext).setHeight3(v3)

                        updateDownUI(v1, v2, v3)
                    }
                    this.setOnclickListener { v ->
                        if (v.id == R.id.tv_YesOrNo_center) {
                            dismiss()
                        }
                    }
                    this.show()
                }
            }
            R.id.ll_lift_estop -> {
                var tempList = ListStatus.getInstance(mContext).checkClientids
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, 80005)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else mToast("请选择设备")
            }
            R.id.ll_lift_up -> {
                var tempList = ListStatus.getInstance(mContext).checkClientids
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, 80004)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else mToast("请选择设备")
            }
            R.id.ll_lift_down -> {
                var tempList = ListStatus.getInstance(mContext).checkClientids
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, 80003)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else mToast("请选择设备")
            }
            R.id.tv_lift_top -> {
                var tempList = ListStatus.getInstance(mContext).checkClientids
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, 80004)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else mToast("请选择设备")
            }
            R.id.tv_lift_down1 -> {
                var tempList = ListStatus.getInstance(mContext).checkClientids
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, 80008, null, lazyHeight1)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else mToast("请选择设备")
            }
            R.id.tv_lift_down2 -> {
                var tempList = ListStatus.getInstance(mContext).checkClientids
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, 80008, null, lazyHeight2)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else mToast("请选择设备")
            }
            R.id.tv_lift_down3 -> {
                var tempList = ListStatus.getInstance(mContext).checkClientids
                if (tempList != null && tempList.isNotEmpty()) {
                    val model = DataModel(tempList, 80008, null, lazyHeight3)
                    Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                } else mToast("请选择设备")
            }
            R.id.ll_lift_close -> {
                startActivity(Intent(mContext, RecycleActivity::class.java))
            }
            R.id.ll_lift_lock -> {
                var tempList = XDApplication.mApplication.mqttService?.getConnectedClients()
                if (tempList != null && tempList.isNotEmpty()) {
                    bdialog.run {
                        this.setSingleBtn2("是否确认全部学生设备将锁屏/解锁操作", "确定")
                        this.setOnclickListener { v ->
                            if (v.id == R.id.iv_YesOrNo_close) {
                                dismiss()
                            } else if (v.id == R.id.tv_YesOrNo_center2) {

                                var isAllLock = ListStatus.getInstance(context).allLock
                                val model = DataModel(tempList, if (isAllLock) 80002 else 80001)
                                Frame.HANDLES.sentAll(10001, Gson().toJson(model))
                                ListStatus.getInstance(context).allLock = !isAllLock
                                dismiss()
                            }
                        }
                        this.show()
                    }
                } else mToast("当前没有设备连接")

            }

        }
    }

    private fun updateDownUI(v1: Float, v2: Float, v3: Float) {
        lazyHeight1 = v1
        lazyHeight2 = v2
        lazyHeight3 = v3

        binding.tvLiftDown1.text = "降至 $lazyHeight1 m"
        binding.tvLiftDown2.text = "降至 $lazyHeight2 m"
        binding.tvLiftDown3.text = "降至 $lazyHeight3 m"
    }
}