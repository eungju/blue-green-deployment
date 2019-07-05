package bluegreen.undertow

import bluegreen.AbstractConnectionGate
import io.undertow.Undertow

class UndertowConnectionGate(name: String, private val undertow: Undertow) : AbstractConnectionGate(name) {
    override fun doOpen() {
        undertow.start()
    }

    override fun doClose() {
        undertow.stop()
    }
}
