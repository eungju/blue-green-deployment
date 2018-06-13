package bluegreen

interface ConnectionGate {
    val name: String

    fun open()

    fun close()

    fun getState(): State

    fun getEstablished(): Int

    enum class State {
        OPEN, CLOSED
    }
}
