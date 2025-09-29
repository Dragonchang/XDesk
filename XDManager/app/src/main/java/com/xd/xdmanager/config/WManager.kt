package com.xd.xdmanager.config

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

    override fun getSoftVersion(): String {
        return mConfigManager.softVersion
    }

    override fun setSoftVersion(softVersion: String) {
        mConfigManager.softVersion = softVersion
    }

    override fun getHardVersion(): String {
        return mConfigManager.hardVersion
    }

    override fun setHardVersion(hardVersion: String) {
        mConfigManager.hardVersion = hardVersion
    }

    override fun getDeviceId(): String {
        return mConfigManager.deviceId
    }

    override fun setDeviceId(deviceId: String) {
        mConfigManager.deviceId = deviceId
    }

    /******************************************************************************/

    override fun getXNum(): Int {
        return mConfigManager.xNum
    }

    override fun setXNum(xNum_: Int) {
        mConfigManager.xNum = xNum_
    }

    override fun setYNum(yNum: Int) {
        mConfigManager.yNum = yNum
    }

    override fun getYNum(): Int {
        return mConfigManager.yNum
    }

    override fun getHeight1(): Float {
        return mConfigManager.height1
    }

    override fun setHeight1(height1: Float) {
        mConfigManager.height1 = height1
    }

    override fun getHeight2(): Float {
        return mConfigManager.height2
    }

    override fun setHeight2(height2: Float) {
        mConfigManager.height2 = height2
    }

    override fun getHeight3(): Float {
        return mConfigManager.height3
    }

    override fun setHeight3(height3: Float) {
        mConfigManager.height2 = height3
    }

    override fun getAllDeskInfo(): String {
        return mConfigManager.allDeskInfo
    }

    override fun setAllDeskInfo(allDeskInfo: String?) {
        if (allDeskInfo != null) {
            mConfigManager.allDeskInfo = allDeskInfo
        } else {
            mConfigManager.allDeskInfo = "";
        }
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