package com.xd.xdmanager.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import com.xd.xdmanager.frame.Frame
import com.xd.xdmanager.frame.MHandler

abstract class BaseFragment<B : ViewBinding> : Fragment(), View.OnClickListener {
    protected lateinit var binding: B
    private var fragmentHandler: MHandler? = null
    protected val className by lazy { this.javaClass.simpleName }
    protected val mContext by lazy { requireContext() }

    abstract fun inflateBinding(inflater: LayoutInflater): B
    abstract fun initView(savedInstanceState: Bundle?)
    abstract fun loadData()
    open fun prepareDo() {}

    // 消息处理方法（子类需要实现）
    abstract fun disposeMsg(type: Int, obj: Any?)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = inflateBinding(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareDo()
        loadData()
        initView(savedInstanceState)
        initFragmentHandler()
    }

    private fun initFragmentHandler() {
        fragmentHandler = MHandler().apply {
            id = "${className}Fragment" // 添加 Fragment 后缀避免 ID 冲突
            setMsglisnener { msg ->
                when (msg.what) {
                    201 -> disposeMsg(msg.arg1, msg.obj)
                    0 -> activity?.finish()
                }
            }
        }

        // 清理可能存在的旧 Handler
        Frame.HANDLES.get(className)?.forEach {
            Frame.HANDLES.remove(it)
        }

        fragmentHandler?.let {
            Frame.HANDLES.add(it)
        }
    }

    override fun onDestroyView() {
        fragmentHandler?.let {
            Frame.HANDLES.remove(it)
        }
        fragmentHandler = null
        super.onDestroyView()
    }

}