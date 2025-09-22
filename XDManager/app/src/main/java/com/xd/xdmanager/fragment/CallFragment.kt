package com.xd.xdmanager.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.xd.xdmanager.ChatActivity
import com.xd.xdmanager.R
import com.xd.xdmanager.adapter.SeatAdapterCall
import com.xd.xdmanager.base.BaseAdapter
import com.xd.xdmanager.base.BaseFragment
import com.xd.xdmanager.config.WManager
import com.xd.xdmanager.databinding.FragmentCallBinding
import com.xd.xdmanager.frame.YesOrNODialog
import com.xd.xdmanager.model.ListStatus
import com.xd.xdmanager.model.StatusData
import com.xd.xdmanager.model.StatusParser.Companion.toChineseSeat
import java.util.ArrayList


class CallFragment : BaseFragment<FragmentCallBinding>() {
    private var mDataList = ArrayList<StatusData>()
    private lateinit var mAdapter: SeatAdapterCall
    private val adialog: YesOrNODialog by lazy {
        YesOrNODialog(mContext)
    }
    private var mType = 0

    override fun inflateBinding(inflater: LayoutInflater): FragmentCallBinding = FragmentCallBinding.inflate(inflater)

    override fun initView(savedInstanceState: Bundle?) {
        ListStatus.getInstance(mContext).dataLiveData.observe(this) { datalist: List<StatusData> ->
            mDataList.clear()
            mDataList.addAll(datalist)
            mAdapter.notifyDataSetChanged()
        }
        initRecycleView()
        binding.switchJiankong.setOnCheckedChangeListener { _, isChecked ->
            mType = if (isChecked) {
                0
            } else {
                1
            }
        }
    }

    private fun initRecycleView() {
        val layoutManager = GridLayoutManager(mContext, WManager.instance(mContext).yNum)
        binding.recycLift.layoutManager = layoutManager
        initAdapter()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initAdapter() {
        mAdapter = SeatAdapterCall(mContext, mDataList, R.layout.item_call_seat)
        binding.recycLift.adapter = mAdapter
        mAdapter.notifyDataSetChanged()
        mAdapter.setOnItemClickListener(object : BaseAdapter.OnItemClickListener {
            override fun onItemClick(view: View, position: Int) {
                if (mDataList[position].isConnected) {
                    var targetClientId = mDataList[position].clientId
                    adialog.run {
                        this.setSingleBtn2("是否与 ${targetClientId.toChineseSeat()} 进行视频通话", "发起视频通话")
                        this.setOnclickListener { v ->
                            if (v.id == R.id.iv_YesOrNo_close) {
                                dismiss()
                            }
                            if (v.id == R.id.tv_YesOrNo_center2) {
                                Log.d("CallFragment", "=== onItemClick client: $targetClientId")
                                dismiss()
                                ChatActivity.openActivity(mContext, true, "laoshi", targetClientId, null, mType)
                            }
                        }
                        this.show()
                    }
                }
            }
        })
    }

    override fun loadData() {
    }

    override fun disposeMsg(type: Int, obj: Any?) {
    }

    override fun onClick(v: View?) {
    }
}