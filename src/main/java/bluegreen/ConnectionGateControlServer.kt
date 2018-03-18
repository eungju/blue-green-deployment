package bluegreen

import io.undertow.Undertow

class ConnectionGateControlServer(connectionGate: ConnectionGate, port: Int) : AutoCloseable {
    private val undertow = Undertow.builder()
            .addHttpListener(port, "")
            .setHandler { exchange ->
                when (exchange.requestPath) {
                    "/open" -> connectionGate.open()
                    "/halfClose" -> connectionGate.halfClose()
                    "/close" -> connectionGate.close()
                }
                exchange.isPersistent = false
                exchange.responseSender.send(listOf("port: ${connectionGate.port}",
                        "state: ${connectionGate.state}",
                        "established: ${connectionGate.getEstablished()}")
                        .joinToString("\r\n"))
            }
            .build()
            .apply {
                start()
            }

    override fun close() {
        undertow.stop()
    }
}
