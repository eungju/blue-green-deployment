package bluegreen

import io.undertow.Undertow

class AppControlServer(acceptorControl: ConnectionControl, port: Int) : AutoCloseable {
    private val undertow = Undertow.builder()
        .addHttpListener(port, "")
        .setHandler { exchange ->
            when (exchange.requestPath) {
                "/open" -> acceptorControl.open()
                "/close" -> acceptorControl.close()
            }
            exchange.isPersistent = false
            exchange.responseSender.send(acceptorControl.getGates()
                .map {
                    listOf("name: ${it.name}",
                        "state: ${it.getState()}")
                        .joinToString(", ")
                }
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
