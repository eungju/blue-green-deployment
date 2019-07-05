package bluegreen

import java.time.Clock
import java.time.Instant

class ConnectionControl(private val clock: Clock) {
    private val gates = mutableListOf<ConnectionGate>()
    private var state: State = State.CLOSED(clock.instant())

    fun register(connectionGate: ConnectionGate) {
        if (state is State.OPEN) {
            connectionGate.open()
        }
        gates.add(connectionGate)
    }

    fun unregister(connectionGate: ConnectionGate) {
        gates.remove(connectionGate)
    }

    fun getGates(): List<ConnectionGate> = gates

    fun open() {
        state = State.OPEN(clock.instant())
        gates.forEach { it.open() }
    }

    fun close() {
        state = State.CLOSED(clock.instant())
        gates.forEach { it.close() }
    }

    fun getState(): State = state

    sealed class State(open val timestamp: Instant) {
        data class OPEN(override val timestamp: Instant) : State(timestamp)
        data class CLOSED(override val timestamp: Instant) : State(timestamp)
    }
}
