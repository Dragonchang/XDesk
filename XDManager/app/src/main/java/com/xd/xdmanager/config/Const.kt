package com.xd.xdmanager.config


object Const {

    const val DEFAULT_SERIAL_NAME = "/dev/ttyS4"
    const val DEFAULT_RATE = 9600

    /*****************************************************************/
    const val DEFAULT_HEIGHT1 = 1f
    const val DEFAULT_HEIGHT2 = 1.5f
    const val DEFAULT_HEIGHT3 = 2f

    const val DEFAULT_X_NUM = 0
    const val DEFAULT_Y_NUM = 0

    /*****************************************************************/
    const val DEFAULT_IP = "10.42.0.1"
    const val DEFAULT_PORT = 9090
    const val DEFAULT_SAFE_DISTANCE = 3.0f
    const val DEFAULT_SAFE_DISTANCE2 = 4.0f

    const val CONNECT_TIMEOUT = 6 * 1000

    const val HEART_ORDER = "13050"


    val DEFAULT_OUT_TIME: Long = 30
    var baseUrl = "http://api.qingyunke.com"
    var token: String = ""
    fun setUrl(u: String) {
        baseUrl = u
    }
}