package bluegreen

import io.undertow.Undertow

class ConnectionControlServer(connectionControl: ConnectionControl, port: Int) : AutoCloseable {
    private val undertow = Undertow.builder()
            .addHttpListener(port, "")
            .setHandler { exchange ->
                when (exchange.requestPath) {
                    "/open" -> connectionControl.open()
                    "/close" -> connectionControl.close()
                }
                exchange.isPersistent = false
                exchange.responseSender.send(listOf("port: ${connectionControl.port}",
                        "state: ${connectionControl.state}",
                        "established: ${connectionControl.getEstablished()}")
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
