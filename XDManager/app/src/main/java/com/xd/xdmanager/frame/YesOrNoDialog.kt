package com.xd.xdmanager.frame

import android.app.Dialog
import android.content.Context
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.view.isVisible
import com.wug.framew.factory.mToast
import com.xd.xdmanager.R
import com.xd.xdmanager.databinding.ItemYesornodialogBinding

/**
 * @author wg
 */
class YesOrNODialog : Dialog {
    private var binding: ItemYesornodialogBinding

    private var confirmListener: OnConfirmListener? = null

    fun interface OnConfirmListener {
        fun onConfirm(value1: Float, value2: Float, value3: Float)
    }

    fun setOnConfirmListener(block: (Float, Float, Float) -> Unit) {
        this.confirmListener = OnConfirmListener(block)
    }

    constructor(context: Context) : this(context, 0)
    constructor(context: Context, themeResId: Int) : super(context, R.style.loadingDialogStyle) {
        binding = ItemYesornodialogBinding.inflate(LayoutInflater.from(context))
        setContentView(binding.root)
        val attr: WindowManager.LayoutParams = window!!.attributes
        attr.height = ViewGroup.LayoutParams.MATCH_PARENT
        attr.width = ViewGroup.LayoutParams.MATCH_PARENT
        attr.gravity = Gravity.CENTER //设置dialog 在布局中的位置
    }

    fun setEtLayout(height1: Float, height2: Float, height3: Float) {
        binding.llYesornoEt.visibility = VISIBLE
        binding.etYesorno1.setText(height1.toString())
        binding.etYesorno2.setText(height2.toString())
        binding.etYesorno3.setText(height3.toString())
        setSingleBtn("", context.resources.getString(R.string.str_sure))
        binding.tvYesOrNoInfo.visibility = GONE
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

    fun setSingleBtn2(info: String, center: String = context.resources.getString(R.string.str_ckxq)) {
        binding.ivYesOrNoClose.visibility = VISIBLE
        if (info.isNotEmpty()) binding.tvYesOrNoInfo.text = info
        binding.tvYesOrNoLeft.visibility = GONE
        binding.tvYesOrNoRight.visibility = GONE
        binding.tvYesOrNoCenter2.visibility = VISIBLE
        binding.tvYesOrNoCenter2.text = center
    }

    fun setOnclickListener(listener: View.OnClickListener) {
        binding.tvYesOrNoLeft.setOnClickListener(listener)
        binding.tvYesOrNoRight.setOnClickListener(listener)
        binding.ivYesOrNoClose.setOnClickListener(listener)
        binding.tvYesOrNoCenter.setOnClickListener { v ->
            try {
                val value1 = binding.etYesorno1.text.toString().toFloat()
                val value2 = binding.etYesorno2.text.toString().toFloat()
                val value3 = binding.etYesorno3.text.toString().toFloat()
                if (value1 < 0 || value1 > 3) {
                    mToast("请检查预设高度1数值是否正确")
                    return@setOnClickListener
                }
                if (value2 < 0 || value2 > 3) {
                    mToast("请检查预设高度2数值是否正确")
                    return@setOnClickListener
                }
                if (value3 < 0 || value3 > 3) {
                    mToast("请检查预设高度3数值是否正确")
                    return@setOnClickListener
                }
                confirmListener?.onConfirm(value1, value2, value3)
                listener.onClick(v)
            } catch (e: Exception) {
                mToast("请检查三个预设高度数值是否正确")
                e.printStackTrace()
            }
        }
        binding.tvYesOrNoCenter2.setOnClickListener(listener)

    }
}
