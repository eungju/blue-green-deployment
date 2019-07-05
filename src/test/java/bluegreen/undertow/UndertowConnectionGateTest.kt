package bluegreen.undertow

import bluegreen.ConnectionGate
import bluegreen.ConnectionGateTest
import io.undertow.Undertow
import org.xnio.OptionMap
import org.xnio.nio.ReusePortNioXnioWorker

class UndertowConnectionGateTest : ConnectionGateTest() {
    override fun server(f: (ConnectionGate) -> Unit) {
        var gate: ConnectionGate? = null
        val worker = ReusePortNioXnioWorker.createWorker(true, OptionMap.EMPTY)
        try {
            val target = Undertow.builder()
                    .addHttpListener(port, "") {
                        if (gate?.getState() == ConnectionGate.State.CLOSED) {
                            it.isPersistent = false
                        }
                        it.responseSender.send("Hello World.")
                        it.endExchange()
                    }
                    .setWorker<Nothing>(worker)
                    .build()
            gate = UndertowConnectionGate(port.toString(), target)
            f(gate)
        } finally {
            worker.shutdown()
        }
    }
}
