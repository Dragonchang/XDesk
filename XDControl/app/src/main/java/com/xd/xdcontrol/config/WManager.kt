package com.xd.xdcontrol.config

import android.annotation.SuppressLint
import android.content.Context
import org.jetbrains.anko.doAsync

/**
 * @author wg
 */
class WManager private constructor(private val mContext: Context) : ConfigCallback {
    /**
     * 配置管理类
     */
    private var mConfigManager: ConfigManager

    /**
     * socket连接管理
     */
    private var isDestory = true

    init {
        mConfigManager = ConfigManager(mContext)
    }

    /**
     * 配置管理 实现方法
     */
    override fun getTcpServiceIpHost(): String {
        return mConfigManager.tcpServiceIpHost
    }

    override fun setTcpServiceIpHost(host: String) {
        mConfigManager.tcpServiceIpHost = host
    }

    override fun getTcpServiceIpPort(): Int {
        return mConfigManager.tcpServiceIpPort
    }

    override fun setTcpServiceIpPort(port: Int) {
        mConfigManager.tcpServiceIpPort = port
    }

    override fun getSafeDistance(): Float {
        return mConfigManager.safeDistance
    }

    override fun setSafeDistance(safeStr: Float) {
        mConfigManager.safeDistance = safeStr
    }

    override fun getSafeDistance2(): Float {
        return mConfigManager.safeDistance2
    }

    override fun setSafeDistance2(safeStr: Float) {
        mConfigManager.safeDistance2 = safeStr
    }

    override fun getWifiNum(): Int {
        return mConfigManager.wifiNum
    }

    override fun setWifiNum(wifi_num: Int) {
        mConfigManager.wifiNum = wifi_num
    }

    override fun getMode(): Int {
        return mConfigManager.mode
    }

    override fun setMode(i: Int) {
        mConfigManager.mode = i
    }

//    override fun getSoftVersion(): String {
//        return mConfigManager.softVersion
//    }
//
//    override fun setSoftVersion(softVersion: String) {
//        mConfigManager.softVersion = softVersion
//    }
//
//    override fun getHardVersion(): String {
//        return mConfigManager.hardVersion
//    }
//
//    override fun setHardVersion(hardVersion: String) {
//        mConfigManager.hardVersion = hardVersion
//    }

    override fun getDeviceId(): String {
        return mConfigManager.deviceId
    }


    override fun setDeviceId(deviceId: String) {
        mConfigManager.deviceId = deviceId
    }

    override fun getIP(): String {
        return mConfigManager.ip
    }

    override fun setIP(ip: String) {
        mConfigManager.ip = ip
    }

    override fun getNetType(): String {
        return mConfigManager.netType
    }

    override fun setNetType(nettype: String) {
        mConfigManager.netType = nettype
    }

    override fun getX(): Int {
        return mConfigManager.x
    }

    override fun setX(x: Int) {
        mConfigManager.x = x
    }

    override fun getY(): Int {
        return mConfigManager.y
    }

    override fun setY(y: Int) {
        mConfigManager.y = y
    }


    companion object {
        private const val TAG = "DataManager"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var mDataManager: WManager? = null

        /**
         * DataManager 单实例
         *
         * @return
         */
        fun instance(context: Context): WManager {
            if (mDataManager == null) {
                synchronized(WManager::class.java) {
                    if (mDataManager == null) {
                        mDataManager = WManager(context)
                    }
                }
            }
            return mDataManager!!
        }
    }

}