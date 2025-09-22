package com.xd.xdmanager.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.nfc.Tag
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.xd.xdmanager.R
import com.xd.xdmanager.model.ListStatus
import com.xd.xdmanager.model.StatusParser
import io.moquette.broker.Server
import io.moquette.broker.subscriptions.Topic
import io.moquette.interception.InterceptHandler
import io.moquette.interception.messages.*
import io.netty.buffer.Unpooled
import io.netty.handler.codec.mqtt.MqttMessageBuilders
import io.netty.handler.codec.mqtt.MqttQoS
import io.netty.util.CharsetUtil
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.ArrayList

class MqttBrokerService : Service() {
    private var mqttServer: Server? = null
    private val binder = LocalBinder()
    private var notificationManager: NotificationManager? = null

    //    private val targetsList = java.util.concurrent.CopyOnWriteArrayList<String>()
    private val connectedClients = ConcurrentHashMap<String, Long>()

    //    private val sendThreadPool = Executors.newFixedThreadPool(10)
    private val sendThreadPool = Executors.newCachedThreadPool()
    private val TAG = "MqttBrokerService"

    inner class LocalBinder : Binder() {
        fun getService(): MqttBrokerService = this@MqttBrokerService
    }

    fun getConnectedClients(): List<String> = connectedClients.keys.toList()

    fun isClientConnected(clientId: String): Boolean = connectedClients.containsKey(clientId)

    fun clearConnectedClients() {
        connectedClients.clear()
    }

    override fun onCreate() {
        super.onCreate()
        connectedClients.clear()
        startForegroundService()
        startMqttBroker()
    }

    private fun startForegroundService() {
        val channelId = "mqtt_service_channel"
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(channelId)

        val notification =
            NotificationCompat.Builder(this, channelId).setContentTitle("锡鼎教师总控后台服务运行中").setContentText("正在监听学生消息").setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_LOW).build()
        startForeground(1, notification)
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "锡鼎教师总控", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "锡鼎教师总控后台服务运行中"
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun startMqttBroker() {
        Thread {
            try {
                mqttServer = Server()
                val props = Properties().apply {
                    val assetManager = this@MqttBrokerService.assets
                    assetManager.open("moquette.conf").use { load(it) }
                }

                mqttServer?.startServer(props)
                setupInterceptors()
                Log.d("MQTT_SERVICE", "MQTT broker started successfully")
            } catch (e: IOException) {
                Log.e("MQTT_SERVICE", "Broker start failed", e)
            }
        }.start()
    }

    private fun setupInterceptors() {
        mqttServer?.addInterceptHandler(object : InterceptHandler {

            override fun onPublish(msg: InterceptPublishMessage?) {
                msg?.let {
                    val rawData = ByteArray(it.payload.readableBytes()).apply {
                        it.payload.readBytes(this)
                    }
//                    Log.d(TAG, "收到消息: Topic=${it.topicName} Payload=${bytesToHex(rawData)}")

                    when (it.topicName) {
                        // 状态上报处理
                        "client/status" -> {
                            handleStatusMessage(it.clientID, rawData)
                        }
                        // 指令响应处理
                        "topic2" -> {}
                    }
                }
            }

            override fun onSubscribe(msg: InterceptSubscribeMessage?) {
                msg?.let {
                    Log.d(
                        TAG, "订阅请求: Client=${it.clientID} " + "Topic=${it.topicFilter} " + "QoS=${it.requestedQos}"
                    )
                    sendClientStatus(it.clientID, "3")
                }
            }

            override fun onUnsubscribe(msg: InterceptUnsubscribeMessage?) {
                msg?.let {
                    Log.d(
                        TAG, "订阅退订: Client=${it.clientID} " + "Topic=${it.topicFilter} " + "username=${it.username}"
                    )
                    sendClientStatus(it.clientID, "4", isRetained = true)
                }
            }

            override fun onMessageAcknowledged(msg: InterceptAcknowledgedMessage?) {
            }

            override fun getID(): String {
                return "Interceptor_${System.currentTimeMillis()}"
            }

            override fun getInterceptedMessageTypes(): Array<Class<*>> {
                return arrayOf(
                    InterceptConnectMessage::class.java,
                    InterceptDisconnectMessage::class.java,
                    InterceptPublishMessage::class.java,
                    InterceptSubscribeMessage::class.java,
                    InterceptUnsubscribeMessage::class.java,
                    InterceptConnectionLostMessage::class.java,
                    InterceptAcknowledgedMessage::class.java
                )
            }

            override fun onConnect(msg: InterceptConnectMessage?) {
                msg?.let {
                    Log.d(TAG, "Client connected: ${it.clientID}")
                    synchronized(connectedClients) {
                        connectedClients[it.clientID] = System.currentTimeMillis()
                    }
                    sendClientStatus(it.clientID, "0")
                }
            }

            override fun onDisconnect(msg: InterceptDisconnectMessage?) {
                try {
                    msg?.clientID?.let {
                        Log.d(TAG, "客户端断开: $it")
                        if (connectedClients.containsKey(it)) {
                            connectedClients.remove(it)
                        }
                        connectedClients.remove(it)
                        sendClientStatus(it, "1", isRetained = true)
                        ListStatus.getInstance(applicationContext).updateClientConnect(it, false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onConnectionLost(msg: InterceptConnectionLostMessage?) {
                try {
                    msg?.let {
                        Log.d(TAG, "客户端异常断开: ${it.clientID} : ${it.username}")
                        if (connectedClients.containsKey(it.clientID)) {
                            connectedClients.remove(it.clientID)
                        }
                        sendClientStatus(it.clientID, "2", isRetained = true)
                        ListStatus.getInstance(applicationContext).updateClientConnect(it.clientID, false)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        })
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return START_STICKY // 服务被终止后自动重启
    }

    override fun onDestroy() {
        super.onDestroy()
        sendThreadPool.shutdownNow()
        notificationManager?.cancelAll()
        connectedClients.clear()
        stopMqttBroker()
    }

    private fun stopMqttBroker() {
        mqttServer?.let {
            it.stopServer()
            Log.d(TAG, "MQTT broker stopped")
        }
    }

    private fun handleStatusMessage(clientID: String, rawData: ByteArray) {
        var status = StatusParser.parse(rawData) ?: return
        status.isConnected = true
        ListStatus.getInstance(application.applicationContext).updateData(clientID, status)
//        Log.d(
//            TAG, """  Client: ${status.clientId}
//        IP: ${status.ip}
//        Lock: ${status.isLock}
//        On220AC: ${status.isOn220AC}
//        AC Voltage: ${status.powerStatus.acVoltage}V
//        DC Voltage: ${status.powerStatus.dcVoltage}V
//        Errors: ${status.errors}
//        WorkStatus: ${status.workStatus}
//    """.trimIndent()
//        )
    }

    public fun sendClient(clientId: String, payload: String, qos: Int = 1) {
        try {
            var content = Unpooled.copiedBuffer(payload, CharsetUtil.UTF_8)
            mqttServer?.internalPublish(
                MqttMessageBuilders.publish().topicName("clients/$clientId")
                    .retained(false)
                    .qos(qos.toMqttQoS())
                    .payload(content)
                    .build(), "master"
            )
            Log.v(TAG, "已发送至 $clientId [$content]")
        } catch (e: Exception) {
            Log.e(TAG, "发送到 $clientId 失败", e)
            e.printStackTrace()
        }
    }

    fun sendClients(targets: List<String>, message: String) {
        val sendTask = Runnable {
            val successCount = AtomicInteger()
            val failedClients = ConcurrentLinkedQueue<String>()

            targets.parallelStream().forEach { clientId ->
                try {
                    sendClient(clientId, message)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failedClients.add(clientId)
                    Log.e(TAG, "发送失败 Client=$clientId", e)
                }
            }

            Log.d(
                TAG, """批量发送结果:
                | 总数: ${targets.size}
                | 成功: $successCount
                | 失败: ${failedClients.size}
                | 失败列表: ${failedClients.joinToString()} """.trimMargin()
            )
        }
        sendThreadPool.execute(sendTask)
    }

    private fun sendClientStatus(clientId: String, payload: String, isRetained: Boolean = false) {
        mqttServer?.internalPublish(
            MqttMessageBuilders.publish().topicName("clients/status/$clientId").retained(isRetained).qos(MqttQoS.AT_LEAST_ONCE)
                .payload(Unpooled.copiedBuffer(payload, CharsetUtil.UTF_8)).build(), "master"
        )
    }

    private fun Int.toMqttQoS() = when (this) {
        0 -> MqttQoS.AT_MOST_ONCE
        2 -> MqttQoS.EXACTLY_ONCE
        else -> MqttQoS.AT_LEAST_ONCE
    }

    private fun ByteArray.crc16CCITT(): Int {
        var crc = 0xFFFF
        for (b in this) {
            crc = crc ushr 8 xor Crc16.CRC16_TABLE[(crc xor (b.toInt() and 0xFF)) and 0xFF]
        }
        return crc and 0xFFFF
    }

    fun sendCommandToClient(
        clientId: String,
        type: Int,
        switch: Boolean,
        value: Float,
        qos: Int = 1
    ) {
        require(type in 1..40) { "Type out of range (1-40)" }
        require(value in 0.0f..50.0f) { "Value out of range (0-50)" }

        val packet = buildCommandPacket(type, switch, value)
        sendBinaryPacket(clientId, packet, qos)
    }

    fun sendCommandToAll(targets: List<String>, type: Int, switch: Boolean = false, value: Float = 0.0f) {
        val packet = buildCommandPacket(type, switch, value)
        val sendTask = Runnable {
            val successCount = AtomicInteger()
            val failedClients = ConcurrentLinkedQueue<String>()

            targets.parallelStream().forEach { clientId ->
                try {
                    sendBinaryPacket(clientId, packet, 1)
                    successCount.incrementAndGet()
                } catch (e: Exception) {
                    failedClients.add(clientId)
                    Log.e(TAG, "发送失败 Client=$clientId", e)
                }
            }

            Log.d(
                TAG, """二进制批量发送结果:
                | 总数: ${targets.size}
                | 成功: $successCount
                | 失败: ${failedClients.size}
                | 失败列表: ${failedClients.joinToString()} """.trimMargin()
            )
        }
        sendThreadPool.execute(sendTask)
    }

    private fun buildCommandPacket(type: Int, switch: Boolean, value: Float): ByteArray {
        val buffer = ByteBuffer.allocate(8).apply {
            order(ByteOrder.BIG_ENDIAN)
            put(type.toByte())            // 1字节 Type
            put(if (switch) 1 else 0)     // 1字节 Switch
            putFloat(value)               // 4字节 Float
        }

        // 计算CRC（前6字节）
        val crcData = buffer.array().copyOfRange(0, 6)
        val crc = crcData.crc16CCITT()

        // 写入CRC
        buffer.putShort(6, crc.toShort())
        return buffer.array()
    }

    private fun sendBinaryPacket(clientId: String, data: ByteArray, qos: Int) {
        try {
            val topic = "command/$clientId"
            val payload = Unpooled.wrappedBuffer(data)

            mqttServer?.internalPublish(
                MqttMessageBuilders.publish()
                    .topicName(topic)
                    .retained(false)
                    .qos(qos.toMqttQoS())
                    .payload(payload)
                    .build(),
                "master"
            )
            Log.d(TAG, "指令发送成功 [$clientId] ${bytesToHex(data)}")
        } catch (e: Exception) {
            Log.e(TAG, "指令发送失败 [$clientId]", e)
            // 可添加错误上报逻辑
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexString = StringBuilder()
        for (b in bytes) {
            hexString.append(String.format("%02X ", b))
        }
        return hexString.toString().trim()
    }
}