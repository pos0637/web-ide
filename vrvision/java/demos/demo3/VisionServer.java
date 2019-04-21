import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.opencv.core.Core;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Base64;

public class VisionServer extends WebSocketServer {
    static {
        // -Djava.library.path=$PROJECT_DIR$\opencv\x64
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private String image;

    public VisionServer(int port) throws UnknownHostException {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        webSocket.send("Welcome to the server!");
        broadcast("new connection: " + clientHandshake.getResourceDescriptor());
        System.out.println(getWebSocketId(webSocket) + " entered the room!");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        broadcast(webSocket + " has left the room!");
        System.out.println(getWebSocketId(webSocket) + " has left the room!");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        broadcast(s);
        System.out.println(getWebSocketId(webSocket) + ": " + s);

        if (s.startsWith("getImageResult:")) {
            image = s.substring("getImageResult:".length());
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Server started!");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

    /**
     * 获取图像
     *
     * @param timeout 超时时间
     * @return 图像
     */
    public byte[] getImage(int timeout) throws InterruptedException {
        broadcast("getImage");

        image = null;
        int elapsed = 0;
        while (elapsed <= timeout) {
            if (image != null) {
                break;
            }

            Thread.sleep(500);
            elapsed += 500;
        }

        return (image == null) ? null : Base64.getDecoder().decode(image);
    }

    private String getWebSocketId(WebSocket webSocket) {
        return webSocket.getRemoteSocketAddress().getAddress().getHostAddress() + ":" + webSocket.getRemoteSocketAddress().getPort();
    }
}
