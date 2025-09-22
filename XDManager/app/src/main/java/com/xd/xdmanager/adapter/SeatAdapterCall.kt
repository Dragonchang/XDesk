package com.xd.xdmanager.adapter

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.xd.xdmanager.R
import com.xd.xdmanager.base.BaseAdapter
import com.xd.xdmanager.base.BaseViewHolder
import com.xd.xdmanager.model.StatusData
import com.xd.xdmanager.model.StatusParser.Companion.toChineseSeat

class SeatAdapterCall(mContext: Context, mDatas: MutableList<StatusData>, mLayoutId: Int) : BaseAdapter<StatusData>(mContext, mDatas, mLayoutId) {
    override fun convert(mContext: Context, holder: BaseViewHolder, t: StatusData, select_position: Int) {
        holder.setText(R.id.tv_item_call_clientid, t.clientId.toChineseSeat())

        val viewLock = holder.getView<ImageView>(R.id.iv_item_seat_call_lock)
        val view220 = holder.getView<TextView>(R.id.tv_item_seat_call_220)
        val viewClientid = holder.getView<TextView>(R.id.tv_item_call_clientid)
        val viewAcdc = holder.getView<TextView>(R.id.tv_item_seat_call_addc)

        if (t.isConnected) {
            viewClientid.setTextColor(Color.WHITE)
            viewLock.visibility = View.VISIBLE
            viewLock.isSelected = t.isLock
            view220.visibility = if (t.isOn220AC) View.VISIBLE else View.INVISIBLE
            viewAcdc.visibility = if (t.powerStatus.isACon || t.powerStatus.isDCon) View.VISIBLE else View.INVISIBLE
            if (t.powerStatus.isACon) holder.setText(R.id.tv_item_seat_call_addc, "${t.powerStatus.acVoltage}V AC")
            if (t.powerStatus.isDCon) holder.setText(R.id.tv_item_seat_call_addc, "${t.powerStatus.dcVoltage}V DC")
        } else {
            viewClientid.setTextColor(Color.GRAY)
            viewLock.visibility = View.INVISIBLE
            view220.visibility = View.INVISIBLE
            viewAcdc.visibility = View.INVISIBLE
        }
    }

//    fun String.toChineseSeat(): String {
//        val pattern = Regex("x(\\d+)y(\\d+)")
//        return pattern.replace(this) { match ->
//            val (row, col) = match.destructured
//            "${row}排${col}列"
//        }
//    }
}