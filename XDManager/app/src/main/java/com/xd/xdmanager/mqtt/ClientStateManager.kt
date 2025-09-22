package com.xd.xdmanager.mqtt

class ClientStateManager private constructor() {
    private val states = mutableMapOf<String, ClientState>()
    private val listeners = mutableListOf<(ClientState) -> Unit>()

    companion object {
        @Volatile
        private var instance: ClientStateManager? = null

        fun getInstance(): ClientStateManager =
            instance ?: synchronized(this) {
                instance ?: ClientStateManager().also { instance = it }
            }
    }

    fun updateState(clientId: String, update: (ClientState) -> Unit) {
        val state = states.getOrPut(clientId) { ClientState(clientId) }
        update(state)
        notifyListeners(state)
    }

    fun addListener(listener: (ClientState) -> Unit) {
        listeners.add(listener)
    }

    private fun notifyListeners(state: ClientState) {
        listeners.forEach { it.invoke(state) }
    }
}