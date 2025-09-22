package com.xd.xdcontrol.base

import android.content.Context
import android.widget.LinearLayout

open class BaseItem(context: Context) : LinearLayout(context) {
    var mApplication = XDApplication.mApplication

    init {
    }
}

