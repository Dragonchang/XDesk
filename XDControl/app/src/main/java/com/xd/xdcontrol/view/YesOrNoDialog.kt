package com.xd.xdcontrol.view

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import com.xd.xdcontrol.R
import com.xd.xdcontrol.databinding.ActivityMainBinding
import com.xd.xdcontrol.databinding.ItemYesornodialogBinding

/**
 * @author wg
 */
class YesOrNODialog : Dialog {
    private lateinit var binding: ItemYesornodialogBinding

    constructor(context: Context) : this(context, 0)
    constructor(context: Context, themeResId: Int) : super(context, R.style.loadingDialogStyle) {
        setContentView(R.layout.item_yesornodialog)
        val attr: WindowManager.LayoutParams = window!!.attributes
        attr.height = ViewGroup.LayoutParams.MATCH_PARENT
        attr.width = ViewGroup.LayoutParams.MATCH_PARENT
        attr.gravity = Gravity.CENTER //设置dialog 在布局中的位置
    }

    fun setTextValue(info: String, left: String = context.resources.getString(R.string.str_chance), right: String = context.resources.getString(R.string.str_sure)) {
        binding.ivYesOrNoClose.visibility = GONE
        if (info.isNotEmpty()) binding.tvYesOrNoInfo.text = info
        if (left.isNotEmpty()) binding.tvYesOrNoLeft.text = left
        if (right.isNotEmpty()) binding.tvYesOrNoRight.text = right
        binding.tvYesOrNoLeft.visibility = VISIBLE
        binding.tvYesOrNoRight.visibility = VISIBLE
        binding.tvYesOrNoCenter.visibility = GONE
    }

    fun setSingleBtn(info: String, center: String = context.resources.getString(R.string.str_ckxq)) {
        binding.ivYesOrNoClose.visibility = VISIBLE
        if (info.isNotEmpty()) binding.tvYesOrNoInfo.text = info
        binding.tvYesOrNoLeft.visibility = GONE
        binding.tvYesOrNoRight.visibility = GONE
        binding.tvYesOrNoCenter.visibility = VISIBLE
        binding.tvYesOrNoCenter.text = center
    }

    fun setOnclickListener(listener: View.OnClickListener) {
        binding.tvYesOrNoLeft.setOnClickListener(listener)
        binding.tvYesOrNoRight.setOnClickListener(listener)
        binding.tvYesOrNoCenter.setOnClickListener(listener)
        binding.ivYesOrNoClose.setOnClickListener(listener)
    }
}
