package bluegreen

class ConnectionControl(override val name: String) : ConnectionGate {
    private val gates = mutableListOf<ConnectionGate>()
    private var state: ConnectionGate.State = ConnectionGate.State.CLOSED

    fun register(connectionGate: ConnectionGate) {
        gates.add(connectionGate)
    }

    fun unregister(connectionGate: ConnectionGate) {
        gates.remove(connectionGate)
    }

    fun getGates(): List<ConnectionGate> = gates

    override fun open() {
        state = ConnectionGate.State.OPEN
        gates.forEach { it.open() }
    }

    override fun close() {
        state = ConnectionGate.State.CLOSED
        gates.forEach { it.close() }
    }

    override fun getState(): ConnectionGate.State = state

    override fun getEstablished(): Int = gates.map { it.getEstablished() }.sum()
}
