package bluegreen

interface ConnectionGate {
    val port: Int

    fun open()

    fun halfClose()

    fun isAccepting(): Boolean

    fun getEstablished(): Long
}
