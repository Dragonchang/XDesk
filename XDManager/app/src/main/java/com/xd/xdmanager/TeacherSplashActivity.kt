package com.xd.xdmanager

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.wug.framew.factory.mToast
import com.xd.xdmanager.base.BaseActivity
import com.xd.xdmanager.databinding.ActivityTeacherSplashBinding

class TeacherSplashActivity : BaseActivity<ActivityTeacherSplashBinding>() {
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

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityTeacherSplashBinding = ActivityTeacherSplashBinding.inflate(layoutInflater)

    override fun loadData() {
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        results.entries.forEach { (permission, granted) ->
            when {
                granted -> {
                    Handler().postDelayed({
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
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

        ObjectAnimator.ofPropertyValuesHolder(binding.ivSplashLogo, scaleX, scaleY, alpha, rotationX).apply {
            duration = 2000
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
            } else {
                mToast(getString(R.string.str_permissonfail))
            }
        }
    }
}