package bluegreen

import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DeploymentTest {

    @Test
    fun handover() {
        val servicePort = 8000
        val request = Request.Builder().get().url("http://localhost:$servicePort").build()
        val blue = App("blue", servicePort)
        blue.connectionGate.open()
        val blueConn = OkHttpClient.Builder().build()
        assertEquals("blue", blueConn.newCall(request).execute().body()?.string())
        val green = App("green", servicePort)
        green.connectionGate.open()
        val greenConn = OkHttpClient.Builder().build()
        assertEquals("green", greenConn.newCall(request).execute().body()?.string())
        blue.connectionGate.halfClose()
        assertEquals("blue", blueConn.newCall(request).execute().body()?.string())
        blue.close()
        green.close()
    }
}
