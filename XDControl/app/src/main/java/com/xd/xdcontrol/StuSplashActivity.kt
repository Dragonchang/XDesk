package com.xd.xdcontrol

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.wug.framew.factory.mToast
import com.xd.xdcontrol.base.BaseActivity
import com.xd.xdcontrol.config.WManager
import com.xd.xdcontrol.databinding.ActivityStuSplashBinding
import com.xd.xdcontrol.net.OrderManager
import com.xd.xdcontrol.net.PowerController
import com.xd.xdcontrol.net.RS485Controller
import java.net.Inet4Address


class StuSplashActivity : BaseActivity<ActivityStuSplashBinding>() {
    private val REQUEST_CODE = 1001
    val permissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.READ_PHONE_STATE)
    }.toTypedArray()

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityStuSplashBinding = ActivityStuSplashBinding.inflate(layoutInflater)

    override fun loadData() {
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        results.entries.forEach { (permission, granted) ->
            when {
                granted -> {
                    Handler().postDelayed({
//                        if (OrderManager.getInstance().isConnected && PowerController.getInstance().isConnected) {
//                            startActivity(Intent(this, HomeActivity::class.java))
//                            finish()
//                        } else {
//                            mToast("串口连接失败")
//                            Log.e("StuSplashActivity", "串口连接失败")
//                        }
                        if (OrderManager.getInstance().isConnected && PowerController.getInstance().isConnected && RS485Controller.getInstance().isConnected) {
                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        } else {
                            mToast("串口连接失败")
                            Log.e("StuSplashActivity", "串口连接失败")
                        }
                    }, 3000)
                }
                shouldShowRequestPermissionRationale(permission) -> {
                    mToast(getString(R.string.str_permissonfail))
                }
                else -> { /* 用户勾选"不再询问"，引导去设置页 */
                }
            }
        }
    }

    override fun initView(savedInstanceState: Bundle?) {
        requestPermissionLauncher.launch(permissions)

        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 0.2f, 1.5f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.2f, 1.5f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f)
        val rotationX = PropertyValuesHolder.ofFloat(View.ROTATION_X, -30f, 0f)

        //TODO 写死数据 后期维护
        WManager.instance(mContext).setX(1)
        WManager.instance(mContext).setY(4)
        WManager.instance(mContext).setIP(getCurrentIP(mContext))
        WManager.instance(mContext).setNetType(getNetworkType(mContext))
        Log.e("StuSplashActivity", "IP:  " + getCurrentIP(mContext))
        Log.e("StuSplashActivity", "type:  " + getNetworkType(mContext))

        ObjectAnimator.ofPropertyValuesHolder(binding.ivSplashLogo, scaleX, scaleY, alpha, rotationX).apply {
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }


//        Handler().postDelayed({
//            PowerController.getInstance().adjustAC(true, 20)
//        }, 5000)
//        Handler().postDelayed({
//            PowerController.getInstance().adjustDC(true, 12, 0)
//        }, 5000)
    }

    fun getCurrentIP(context: Context): String {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val activeNetwork = connectivityManager.activeNetwork ?: return "0.0.0.0"
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork)

        // 优先获取IPv4地址
        linkProperties?.linkAddresses?.forEach { addr ->
            if (addr.address is Inet4Address) {
                return addr.address.hostAddress
            }
        }
        return "0.0.0.0"
    }

    // 判断网络类型
    fun getNetworkType(context: Context): String {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val caps = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
            else -> "Unknown"
        }
    }

    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                ) {
                } else {
                    Toast.makeText(this, "需要摄像头权限", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}