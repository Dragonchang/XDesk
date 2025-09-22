package com.xd.xdcontrol.base

import android.app.ProgressDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.gyf.immersionbar.ktx.hideStatusBar
import com.gyf.immersionbar.ktx.immersionBar
import com.xd.xdcontrol.R
import com.xd.xdcontrol.frame.Frame
import com.xd.xdcontrol.frame.MHandler
import com.xd.xdcontrol.net.StatusParser
import org.jetbrains.anko.toast


abstract class BaseActivity<B : ViewBinding> : AppCompatActivity(), View.OnClickListener {
    protected lateinit var binding: B

    val loadingDialog by lazy { ProgressDialog(mContext).apply { this.setMessage(getString(R.string.closing)) } }

    var handler = MHandler()
    val className = this.javaClass.simpleName
    var mContext = this
    var robotApp = XDApplication.mApplication
    private var baseHandler: Handler? = null

    @Volatile
    private var reconnecting = false

    abstract fun inflateBinding(layoutInflater: LayoutInflater): B
//    abstract fun getLayoutID(): Int

    open fun prepareDo() {}
    abstract fun loadData()
    abstract fun initView(savedInstanceState: Bundle?)
    open fun disposeMsg(type: Int, obj: Any) {
        when (type) {
            10001 -> {}
            10002 -> {}
        }
    }

    override fun onStart() {
        super.onStart()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        baseHandler = Handler()

        val decorView = window.decorView
        decorView.setOnSystemUiVisibilityChangeListener { visibility: Int ->
            // 检查系统UI是否可见
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                // 系统UI不可见，你可以在这里处理导航栏和状态栏隐藏后的逻辑
            } else {
                // 系统UI可见，你可以在这里处理导航栏和状态栏显示时的逻辑
            }
        }

        prepareDo()
//        setContentView(getLayoutID())
        loadData()
        initView(savedInstanceState)
        initImmersionBar()
        initHandler()
    }

    fun hideSystemUI() {
        // 启用沉浸式全屏模式
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    fun showSystemUI() {
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI();
    }

    /**
     * 初始化状态栏
     */
    protected open fun initImmersionBar() {
        immersionBar {
            hideStatusBar()
            transparentBar()
//            statusBarDarkFont(true, 0.2f)
//            statusBarColor(R.color.colorPrimary)
//            navigationBarColor(R.color.colorPrimary)
        }
    }

    private fun initHandler() {
        handler.setId(className)
        handler.setMsglisnener { msg ->
            when (msg.what) {
                201 -> if (msg.obj != null) this@BaseActivity.disposeMsg(msg.arg1, msg.obj)
                0 -> finish()
            }
        }
        if (Frame.HANDLES.get(className).size > 0) {
            Frame.HANDLES.get(className).forEach {
                Frame.HANDLES.remove(it)
            }
        }
        Frame.HANDLES.add(handler)
    }

    fun showLoading() {
        if (!loadingDialog.isShowing) {
            loadingDialog.setCanceledOnTouchOutside(false)
            loadingDialog.show()
        }
    }

    fun dismissLoading() {
        if (loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
        }
    }

    override fun onDestroy() {
        baseHandler?.removeCallbacksAndMessages(null)
        Frame.HANDLES.remove(this.handler)
        super.onDestroy()
    }
}