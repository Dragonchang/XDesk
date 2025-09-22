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

class SeatAdapterSure(mContext: Context, mDatas: MutableList<StatusData>, mLayoutId: Int) : BaseAdapter<StatusData>(mContext, mDatas, mLayoutId) {
    override fun convert(mContext: Context, holder: BaseViewHolder, t: StatusData, select_position: Int) {
        holder.setText(R.id.tv_item_close_clientid, t.clientId.toChineseSeat())

        val viewClientid = holder.getView<TextView>(R.id.tv_item_close_clientid)

        //TODO
        if (t.isConnected) {
            viewClientid.setTextColor(Color.WHITE)
        } else {
            viewClientid.setTextColor(Color.GRAY)
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