package bluegreen

interface ConnectionGate {
    val port: Int

    val state: State

    fun open()

    fun halfClose()

    fun close()

    fun getEstablished(): Long

    enum class State {
        OPEN, HALF_CLOSED, CLOSING, CLOSED
    }
}
