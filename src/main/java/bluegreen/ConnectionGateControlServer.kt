package bluegreen

import io.undertow.Undertow

class ConnectionGateControlServer(connectionGate: ConnectionGate, port: Int) : AutoCloseable {
    private val undertow = Undertow.builder()
            .addHttpListener(port, "")
            .setHandler { exchange ->
                when (exchange.requestPath) {
                    "/open" -> connectionGate.open()
                    "/halfClose" -> connectionGate.halfClose()
                }
                exchange.setPersistent(false)
                exchange.responseSender.send(listOf("port: ${connectionGate.port}",
                        "accepting: ${connectionGate.isAccepting()}",
                        "established: ${connectionGate.getEstablished()}")
                        .joinToString(", "))
            }
            .build()
            .apply {
                start()
            }

    override fun close() {
        undertow.stop()
    }
}