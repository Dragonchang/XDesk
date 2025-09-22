package com.xd.xdcontrol

import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.xd.xdcontrol.base.BaseActivity
import com.xd.xdcontrol.databinding.ActivityMainBinding
import com.xd.xdcontrol.net.OrderManager
import com.xd.xdcontrol.net.PowerController
import com.xd.xdcontrol.net.RS485Controller
import com.xd.xdcontrol.net.StatusParser


class MainActivity : BaseActivity<ActivityMainBinding>() {

    private var mSensor: RS485Controller? = null
    private var mOrderManager: OrderManager? = null
    private var mPower: PowerController? = null

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityMainBinding {
        return ActivityMainBinding.inflate(layoutInflater)
    }

    override fun loadData() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        binding.btnConnect.setOnClickListener(this)
        binding.btnWorkstatus.setOnClickListener(this)
        binding.btnError.setOnClickListener(this)
        binding.btnPosition.setOnClickListener(this)

        binding.btnEstop.setOnClickListener(this)
        binding.btnEggUp.setOnClickListener(this)
        binding.btnEggDown.setOnClickListener(this)
        binding.btnTableUp.setOnClickListener(this)
        binding.btnTableDown.setOnClickListener(this)
        binding.btnTableDown1m.setOnClickListener(this)
        binding.btnOnAc.setOnClickListener(this)
        binding.btnNoAc.setOnClickListener(this)

//        mStatusInstance = StatusParser.getInstance()

    }


    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_connect -> {
//                mOrderManager = OrderManager.getInstance().
            }
            R.id.btn_workstatus -> {
//                mSensor = RS485Controller().apply {
//                    init()
//                }


            }
            R.id.btn_error -> {
//                mPower = PowerController().apply {
//                    init()
//                }
            }
            R.id.btn_position -> {
                mPower?.adjustDC(false, 12, 5);
            }
            R.id.btn_estop -> {
                mOrderManager?.sendCommand(0x12, 0x00.toByte(), 0x00.toByte(), 3)
            }
            R.id.btn_egg_up -> {
                mOrderManager?.sendCommand(0x10, 0x00.toByte(), 0x00.toByte(), 3)
            }
            R.id.btn_egg_down -> {
                mOrderManager?.sendCommand(0x09, 0x00.toByte(), 0x00.toByte(), 3)
            }
            R.id.btn_table_up -> {
                mOrderManager?.sendCommand(0x02, 0x00.toByte(), 0x00.toByte(), 3)
            }
            R.id.btn_table_down -> {
                mOrderManager?.sendCommand(0x01, 0x00.toByte(), 0x00.toByte(), 3)
            }
            R.id.btn_table_down1m -> {
                mOrderManager?.sendCommand(0x06, 0x00.toByte(), 0x00.toByte(), 3)
            }
            R.id.btn_on_ac -> {
                mOrderManager?.sendCommand(0x04, 0x00.toByte(), 0x00.toByte(), 3)
            }
            R.id.btn_no_ac -> {
                mOrderManager?.sendCommand(0x05, 0x00.toByte(), 0x00.toByte(), 3)
            }
        }
    }

    override fun disposeMsg(type: Int, obj: Any) {
        super.disposeMsg(type, obj)
        when (type) {
            10001 -> {
//                runOnUiThread {
//                    binding.etEggTop.setText(mStatusInstance.workStatus.eggMotorOrigin.toString())
//                    binding.etEggBottom.setText(mStatusInstance.workStatus.eggMotorEnd.toString())
//                    binding.etTableTop.setText(mStatusInstance.workStatus.topMotorOrigin.toString())
//                    binding.etTableBottom.setText(mStatusInstance.workStatus.topMotorPosition.toString())
//
//                    binding.etEggMotorStop.setText(mStatusInstance.workStatus.eggMotorStopped.toString())
//                    binding.etEggMotorUping.setText(mStatusInstance.workStatus.eggMotorRaising.toString())
//                    binding.etEggMotorDowning.setText(mStatusInstance.workStatus.eggMotorFalling.toString())
//                    binding.etIsac.setText(mStatusInstance.workStatus.acOutputOn.toString())
//                    binding.etTableMotorStop.setText(mStatusInstance.workStatus.topMotorStopped.toString())
//                    binding.etTableUping.setText(mStatusInstance.workStatus.topMotorRaising.toString())
//                    binding.etTableDowning.setText(mStatusInstance.workStatus.topMotorFalling.toString())
//                }
            }
            10002 -> {
//                runOnUiThread {
//                    binding.etAcOver.setText(mStatusInstance.errorStatus.acOverCurrent.toString())
//                    binding.etEggBottomOver.setText(mStatusInstance.errorStatus.eggEndTimeout.toString())
//                    binding.etEggTopOver.setText(mStatusInstance.errorStatus.eggOriginTimeout.toString())
//                    binding.etTableOver.setText(mStatusInstance.errorStatus.topOriginTimeout.toString())
//                }
            }
            10003 -> {
//                runOnUiThread {
//                    binding.etPosition.setText(mStatusInstance.position.toString())
//                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        rs485Controller?.close()
        mOrderManager?.close()
    }
}