package bluegreen

interface ConnectionControl {
    val port: Int

    val state: State

    fun addListener(listener: Listener)

    fun removeListener(listener: Listener)

    fun open()

    fun close()

    fun getEstablished(): Long

    enum class State {
        OPEN, CLOSED
    }

    interface Listener {
        fun onChange(state: State)
    }
}
