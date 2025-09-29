package com.xd.xdmanager.config

import android.content.Context
import com.xd.xdmanager.config.Const.DEFAULT_IP
import com.xd.xdmanager.config.Const.DEFAULT_PORT
import com.xd.xdmanager.config.Const.DEFAULT_SAFE_DISTANCE
import com.xd.xdmanager.config.Const.DEFAULT_SAFE_DISTANCE2
import com.xd.xdmanager.base.getShareP
import com.xd.xdmanager.base.putShareP
import com.xd.xdmanager.config.Const.DEFAULT_HEIGHT1
import com.xd.xdmanager.config.Const.DEFAULT_HEIGHT2
import com.xd.xdmanager.config.Const.DEFAULT_HEIGHT3
import com.xd.xdmanager.config.Const.DEFAULT_X_NUM
import com.xd.xdmanager.config.Const.DEFAULT_Y_NUM

/**
 * @author wg
 */
class ConfigManager(private val mContext: Context) : ConfigCallback {

//    init {
//    }

    override fun getTcpServiceIpHost(): String {
        val sp_ip = getShareP(mContext, PREFER_TCP_IP_HOST_KEY) ?: ""
        return sp_ip.ifEmpty { DEFAULT_IP }
    }

    override fun setTcpServiceIpHost(host: String) {
        putShareP(mContext, PREFER_TCP_IP_HOST_KEY, host)
    }

    override fun getTcpServiceIpPort(): Int {
        return getShareP(mContext, PREFER_TCP_IP_PORT_KEY, DEFAULT_PORT)
    }

    override fun setTcpServiceIpPort(port: Int) {
        putShareP(mContext, PREFER_TCP_IP_PORT_KEY, port)
    }

    override fun getSafeDistance(): Float {
        return getShareP(mContext, PREFER_SAFE_DISTANCE_KEY, DEFAULT_SAFE_DISTANCE)
    }

    override fun setSafeDistance(safeDistance: Float) {
        putShareP(mContext, PREFER_SAFE_DISTANCE_KEY, safeDistance)
    }

    override fun getSafeDistance2(): Float {
        return getShareP(mContext, PREFER_SAFE_DISTANCE_KEY2, DEFAULT_SAFE_DISTANCE2)
    }

    override fun setSafeDistance2(safeDistance: Float) {
        putShareP(mContext, PREFER_SAFE_DISTANCE_KEY2, safeDistance)
    }

    override fun getSoftVersion(): String {
        return getShareP(mContext, PREFER_SOFTVERION) ?: "-"
    }

    override fun setSoftVersion(softVersion: String) {
        putShareP(mContext, PREFER_SOFTVERION, softVersion)
    }

    override fun getHardVersion(): String {
        return getShareP(mContext, PREFER_HARDVERSION) ?: "-"
    }

    override fun setHardVersion(hardVersion: String) {
        putShareP(mContext, PREFER_HARDVERSION, hardVersion)
    }

    override fun getDeviceId(): String {
        return getShareP(mContext, PREFER_DEVICEID) ?: "-"
    }

    override fun setDeviceId(deviceId: String?) {
    }

    /****************************************************************************/

    override fun getXNum(): Int {
        return getShareP(mContext, PREFER_X_NUM, DEFAULT_X_NUM)
    }

    override fun setXNum(xNum_: Int) {
        putShareP(mContext, PREFER_X_NUM, xNum_)
    }

    override fun setYNum(yNum: Int) {
        putShareP(mContext, PREFER_Y_NUM, yNum)
    }

    override fun getYNum(): Int {
        return getShareP(mContext, PREFER_Y_NUM, DEFAULT_Y_NUM)
    }

    override fun getHeight1(): Float = getShareP(mContext, PREFER_HEIGHT1, DEFAULT_HEIGHT1)

    override fun setHeight1(height1: Float) {
        putShareP(mContext, PREFER_HEIGHT1, height1)
    }

    override fun getHeight2(): Float = getShareP(mContext, PREFER_HEIGHT2, DEFAULT_HEIGHT2)

    override fun setHeight2(height2: Float) {
        putShareP(mContext, PREFER_HEIGHT2, height2)
    }

    override fun getHeight3(): Float = getShareP(mContext, PREFER_HEIGHT3, DEFAULT_HEIGHT3)

    override fun setHeight3(height3: Float) {
        putShareP(mContext, PREFER_HEIGHT3, height3)
    }

    override fun getAllDeskInfo(): String {
        return getShareP(mContext, PREFER_All_Desk_Info) ?: ""
    }

    override fun setAllDeskInfo(desks: String) {
        putShareP(mContext, PREFER_All_Desk_Info, desks)
    }


    companion object {
        private const val TAG = "ConfigManager"
        private const val PREFER_HEIGHT1 = "height1"
        private const val PREFER_HEIGHT2 = "height2"
        private const val PREFER_HEIGHT3 = "height3"
        private const val PREFER_X_NUM = "x_num"
        private const val PREFER_Y_NUM = "y_num"

        private const val PREFER_TCP_IP_HOST_KEY = "tcp_ip_host"
        private const val PREFER_TCP_IP_PORT_KEY = "tcp_ip_port"
        private const val PREFER_SAFE_DISTANCE_KEY = "safe_distance"
        private const val PREFER_SAFE_DISTANCE_KEY2 = "safe_distance2"
        private const val PREFER_SOFTVERION = "softversion"
        private const val PREFER_HARDVERSION = "hardversion"
        private const val PREFER_DEVICEID = "deviceid"
        private const val PREFER_All_Desk_Info = "all_desk_info"
    }
}