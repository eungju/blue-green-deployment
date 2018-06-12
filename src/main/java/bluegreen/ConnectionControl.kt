package bluegreen

interface ConnectionControl {
    val port: Int

    val state: State

    fun open()

    fun close()

    fun getEstablished(): Long

    enum class State {
        OPEN, CLOSING, CLOSED
    }
}
