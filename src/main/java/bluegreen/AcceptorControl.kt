package bluegreen

class AcceptorControl(override val name: String) : Acceptor {
    private val acceptors = mutableListOf<Acceptor>()
    private var state: Acceptor.State = Acceptor.State.CLOSED

    fun register(acceptor: Acceptor) {
        acceptors.add(acceptor)
    }

    fun unregister(acceptor: Acceptor) {
        acceptors.remove(acceptor)
    }

    fun getAcceptors(): List<Acceptor> = acceptors

    override fun open() {
        state = Acceptor.State.OPEN
        acceptors.forEach { it.open() }
    }

    override fun close() {
        state = Acceptor.State.CLOSED
        acceptors.forEach { it.close() }
    }

    override fun getState(): Acceptor.State = state

    override fun getEstablished(): Int = acceptors.map { it.getEstablished() }.sum()
}
