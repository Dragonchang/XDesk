package com.xd.xdmanager.model

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StatusParser {
    companion object {
        private const val HEADER = 0xAA55.toShort()
        private const val VERSION = 0x01.toByte()

        fun parse(rawData: ByteArray): StatusData? {
            try {
                val buffer = ByteBuffer.wrap(rawData).order(ByteOrder.BIG_ENDIAN)

                // 1. 校验头
                if (buffer.getShort() != HEADER) {
                    Log.w("StatusParser", "Invalid packet header")
                    return null
                }

                // 2. 版本检查
                val version = buffer.get()
                if (version != VERSION) {
                    Log.w("StatusParser", "Unsupported version: $version")
                    return null
                }

                // 3. 获取数据长度
                val dataLength = buffer.short.toInt() and 0xFFFF
                val dataBuffer = buffer.slice().order(ByteOrder.BIG_ENDIAN)

                // 4. CRC校验
                val crcData = ByteArray(dataLength - 2) // 扣除CRC自身长度
                buffer.get(crcData, 0, dataLength - 2)
                val calculatedCrc = calculateCRC16(crcData)
                val receivedCrc = buffer.short.toInt() and 0xFFFF
//                if (receivedCrc != calculatedCrc) {
//                    Log.w("StatusParser", "CRC check failed")
//                    return null
//                }

                return parseData(dataBuffer, dataLength)
            } catch (e: Exception) {
                Log.e("StatusParser", "Parse error: ${e.message}")
                return null
            }
        }

        private fun parseData(buffer: ByteBuffer, length: Int): StatusData {
            // 读取clientID
            val clientIdLen = buffer.get().toInt() and 0xFF
            val clientIdBytes = ByteArray(clientIdLen)
            buffer.get(clientIdBytes)
            val clientId = String(clientIdBytes, Charsets.UTF_8)

            // 解析IP地址
            val ip = buildString {
                append(buffer.get().toInt() and 0xFF)
                append('.')
                append(buffer.get().toInt() and 0xFF)
                append('.')
                append(buffer.get().toInt() and 0xFF)
                append('.')
                append(buffer.get().toInt() and 0xFF)
            }

            // 解析布尔字段
            val flags1 = buffer.get()
            val flags2 = buffer.get()

            val status = StatusData(
                clientId = clientId,
                ip = ip,
                isLock = (flags1.toInt() and 0x01) != 0,
                isOn220AC = (flags1.toInt() and 0x02) != 0,
                powerStatus = PowerStatus(
                    isACon = (flags1.toInt() and 0x04) != 0,
                    acVoltage = buffer.float,
                    isDCon = (flags1.toInt() and 0x08) != 0,
                    dcVoltage = buffer.float
                ),
                errors = ErrorStatus(
                    acOverCurrent = (flags1.toInt() and 0x10) != 0,
                    eggEndTimeout = (flags1.toInt() and 0x20) != 0,
                    eggOriginTimeout = (flags1.toInt() and 0x40) != 0,
                    topOriginTimeout = (flags1.toInt() and 0x80) != 0
                ),
                workStatus = WorkStatus(
                    eggMotorOrigin = (flags2.toInt() and 0x01) != 0,
                    eggMotorEnd = (flags2.toInt() and 0x02) != 0,
                    topMotorOrigin = (flags2.toInt() and 0x04) != 0,
                    topMotorPosition = (flags2.toInt() and 0x08) != 0,
                    acOutputOn = (flags2.toInt() and 0x10) != 0
                ),
                isConnected = false,
                isCheck = false
            )

            // 跳过CRC字段
            buffer.position(buffer.position() + 2)

            return status
        }

        fun calculateCRC16(data: ByteArray): Int {
            var crc = 0xFFFF
            for (b in data) {
                crc = crc xor (b.toInt() and 0xFF shl 8)
                repeat(8) {
                    crc = if (crc and 0x8000 != 0) {
                        (crc shl 1) xor 0x1021
                    } else {
                        crc shl 1
                    }
                    crc = crc and 0xFFFF // 保持16位
                }
            }
            return crc
        }

        fun String.toChineseSeat(): String {
            val pattern = Regex("x(\\d+)y(\\d+)")
            return pattern.replace(this) { match ->
                val (row, col) = match.destructured
                "${row}排${col}列"
            }
        }
    }
}

// 数据模型
data class StatusData(
    val clientId: String,
    val ip: String,
    val isLock: Boolean,
    val isOn220AC: Boolean,
    val powerStatus: PowerStatus,
    val errors: ErrorStatus,
    val workStatus: WorkStatus,
    var isConnected: Boolean,
    var isCheck: Boolean
) {
    constructor(clientId_: String, sConnected_: Boolean) : this(
        clientId_,
        "",
        isLock = false,
        isOn220AC = false,
        powerStatus = PowerStatus(false, 0f, false, 0f),
        errors = ErrorStatus(acOverCurrent = false, eggEndTimeout = false, eggOriginTimeout = false, topOriginTimeout = false),
        workStatus = WorkStatus(eggMotorOrigin = false, eggMotorEnd = false, topMotorOrigin = false, topMotorPosition = false, acOutputOn = false),
        isConnected = sConnected_,
        isCheck = false
    )
}

data class PowerStatus(
    val isACon: Boolean,
    val acVoltage: Float,
    val isDCon: Boolean,
    val dcVoltage: Float
)

data class ErrorStatus(
    val acOverCurrent: Boolean,
    val eggEndTimeout: Boolean,
    val eggOriginTimeout: Boolean,
    val topOriginTimeout: Boolean
)

data class WorkStatus(
    val eggMotorOrigin: Boolean,
    val eggMotorEnd: Boolean,
    val topMotorOrigin: Boolean,
    val topMotorPosition: Boolean,
    val acOutputOn: Boolean
)