package bluegreen;

import rawhttp.core.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

public class RawHttpConnection implements AutoCloseable {
    private RawHttp rawHttp = new RawHttp();
    private Socket socket;
    private InputStream socketIn;
    private OutputStream socketOut;

    public RawHttpConnection(String host, int port) throws Exception {
        socket = new Socket();
        socket.setSoLinger(false, 0);
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.connect(new InetSocketAddress(host, port));
        socketIn = socket.getInputStream();
        socketOut = socket.getOutputStream();
    }

    public boolean isOpen() {
        return socket != null && socket.isConnected();
    }

    @Override
    public void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        socket = null;
    }

    public EagerHttpResponse<?> request(RawHttpRequest request) throws IOException {
        request.writeTo(socketOut);
        EagerHttpResponse<?> response = rawHttp.parseResponse(socketIn).eagerly(true);
        for (String each : response.getHeaders().get("Connection")) {
            if ("close".equals(each)) {
                close();
                break;
            }
        }
        return response;
    }

    public EagerHttpResponse<?> hello() throws IOException {
        return request(new RawHttpRequest(
                new RequestLine("GET", URI.create("/"), HttpVersion.HTTP_1_1),
                RawHttpHeaders.newBuilder()
                        .with("Host", "localhost:$port")
                        .build(),
                null, null)
        );
    }
}
