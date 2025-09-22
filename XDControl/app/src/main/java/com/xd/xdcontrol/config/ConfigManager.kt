package com.xd.xdcontrol.config

import android.content.Context
import com.xd.xdcontrol.base.getShareP
import com.xd.xdcontrol.base.putShareP
import com.xd.xdcontrol.config.Const.DEFAULT_IP
import com.xd.xdcontrol.config.Const.DEFAULT_PORT
import com.xd.xdcontrol.config.Const.DEFAULT_SAFE_DISTANCE
import com.xd.xdcontrol.config.Const.DEFAULT_SAFE_DISTANCE2
import com.xd.xdcontrol.config.Const.DEFAULT_WIFI_NUM
import com.xd.xdcontrol.config.Const.DEFAULT_X
import com.xd.xdcontrol.config.Const.DEFAULT_Y

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

    override fun getWifiNum(): Int {
        return getShareP(mContext, PREFER_WIFI_NUM, DEFAULT_WIFI_NUM)
    }

    override fun setWifiNum(wifi_num: Int) {
        putShareP(mContext, PREFER_WIFI_NUM, wifi_num)
    }

    override fun getMode(): Int {
        return getShareP(mContext, PREFER_MODE_KEY, 0)
    }

    override fun setMode(i: Int) {
        putShareP(mContext, PREFER_MODE_KEY, i)
    }

//    override fun getSoftVersion(): String {
//        return getShareP(mContext, PREFER_SOFTVERION) ?: "-"
//    }
//
//    override fun setSoftVersion(softVersion: String) {
//        putShareP(mContext, PREFER_SOFTVERION, softVersion)
//    }
//
//    override fun getHardVersion(): String {
//        return getShareP(mContext, PREFER_HARDVERSION) ?: "-"
//    }
//
//    override fun setHardVersion(hardVersion: String) {
//        putShareP(mContext, PREFER_HARDVERSION, hardVersion)
//    }

    override fun getDeviceId(): String {
        return getShareP(mContext, PREFER_DEVICEID) ?: "-"
    }

    override fun setDeviceId(deviceId: String) {
        putShareP(mContext, PREFER_DEVICEID, deviceId)
    }

    override fun getIP(): String {
        return getShareP(mContext, PREFER_IP) ?: ""
    }

    override fun setIP(ip: String) {
        putShareP(mContext, PREFER_IP, ip)
    }

    override fun getNetType(): String {
        return getShareP(mContext, PREFER_NETTYPE) ?: ""
    }

    override fun setNetType(nettype: String) {
        putShareP(mContext, PREFER_NETTYPE, nettype)
    }

    override fun getX(): Int {
        return getShareP(mContext, PREFER_TCP_X_KEY, DEFAULT_X)
    }

    override fun setX(x: Int) {
        putShareP(mContext, PREFER_TCP_X_KEY, x)
    }

    override fun getY(): Int {
        return getShareP(mContext, PREFER_TCP_Y_KEY, DEFAULT_Y)
    }

    override fun setY(y: Int) {
        putShareP(mContext, PREFER_TCP_Y_KEY, y)
    }


    companion object {
        private const val TAG = "ConfigManager"
        private const val PREFER_TCP_X_KEY = "tcp_x"
        private const val PREFER_TCP_Y_KEY = "tcp_y"
        private const val PREFER_IP = "ip"
        private const val PREFER_NETTYPE = "nettype"

        private const val PREFER_TCP_IP_HOST_KEY = "tcp_ip_host"
        private const val PREFER_TCP_IP_PORT_KEY = "tcp_ip_port"
        private const val PREFER_SAFE_DISTANCE_KEY = "safe_distance"
        private const val PREFER_SAFE_DISTANCE_KEY2 = "safe_distance2"
        private const val PREFER_WIFI_NUM = "wifi_num"
        private const val PREFER_MODE_KEY = "mode"
        private const val PREFER_DEVICEID = "deviceid"
    }
}