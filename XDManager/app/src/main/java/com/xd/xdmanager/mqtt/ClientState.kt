package com.xd.xdmanager.mqtt

data class ClientState(
    val clientId: String,
    var isOnline: Boolean = false,
    var powerStatus: PowerStatus = PowerStatus.UNKNOWN,
    var motorA: MotorState = MotorState(),
    var motorB: MotorState = MotorState(),
    var lastUpdate: Long = System.currentTimeMillis()
)

enum class PowerStatus { ON, OFF, STANDBY, UNKNOWN }

data class MotorState(
    var speed: Int = 0,
    var direction: Direction = Direction.STOP
)

enum class Direction { FORWARD, REVERSE, STOP }