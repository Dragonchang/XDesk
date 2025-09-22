package com.xd.xdcontrol.base

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

/**
 * Created by RaphetS on 2016/9/28.
 * 普通的万能Adapter
 * 支持onItemClick
 * 支持onLongItemClick
 */
abstract class BaseAdapter<T>(
    private val mContext: Context,
    private val mDatas: MutableList<T>,
    private val mLayoutId: Int
) : RecyclerView.Adapter<BaseViewHolder>() {
    private var mItemClickListener: OnItemClickListener? = null
    private var mLongItemClickListener: onLongItemClickListener? = null

    fun updateData(data: List<T>) {
        mDatas.clear()
        mDatas.addAll(data)
        notifyDataSetChanged()
    }

    fun addAll(data: List<T>) {
        mDatas.addAll(data)
        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val view = LayoutInflater.from(mContext).inflate(mLayoutId, parent, false)
        return BaseViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mDatas.size
    }


    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        convert(mContext, holder, mDatas[position], position)
        if (mItemClickListener != null) {
            holder.mItemView.setOnClickListener { v ->
                mItemClickListener!!.onItemClick(v, position)
            }
        }
        if (mLongItemClickListener != null) {
            holder.itemView.setOnLongClickListener { v ->
                mLongItemClickListener!!.onLongItemClick(v, position)
                true
            }
        }
    }

    protected abstract fun convert(
        mContext: Context,
        holder: BaseViewHolder,
        t: T,
        select_position: Int
    )


    interface OnItemClickListener {
        fun onItemClick(view: View, position: Int)
    }

    interface onLongItemClickListener {
        fun onLongItemClick(view: View, postion: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.mItemClickListener = listener
    }

    fun setonLongItemClickListener(listener: onLongItemClickListener) {
        this.mLongItemClickListener = listener
    }
}