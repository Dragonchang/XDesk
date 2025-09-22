package com.xd.xdmanager.base

import android.content.Context
import android.widget.LinearLayout
import com.xd.xdmanager.base.XDApplication

open class BaseItem(context: Context) : LinearLayout(context) {
    var mApplication = XDApplication.mApplication

    init {
    }
}

