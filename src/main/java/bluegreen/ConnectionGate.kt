package bluegreen

interface ConnectionGate {
    val name: String

    fun open()

    fun close()

    fun getState(): State

    enum class State {
        OPEN, CLOSED
    }
}
