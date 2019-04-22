import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class VisionServer extends WebSocketServer {
    static {
        // -Djava.library.path=$PROJECT_DIR$\opencv\x64
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private String image;
    private ConcurrentHashMap<String, Mat> mats = new ConcurrentHashMap<>();

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

        if (s.startsWith("captureResult:")) {
            image = s.substring("captureResult:".length());
        } else if (s.startsWith("getImages")) {
            getImages();
        } else if (s.startsWith("getImage:")) {
            getImage(s.substring("getImage:".length()));
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
    public byte[] capture(int timeout) {
        broadcast("capture");

        image = null;
        int elapsed = 0;
        while (elapsed <= timeout) {
            if (image != null) {
                break;
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            elapsed += 500;
        }

        return (image == null) ? null : Base64.getDecoder().decode(image);
    }

    /**
     * 显示图片
     *
     * @param name 名称
     * @param mat  图片
     */
    public void showImage(String name, Mat mat) {
        mats.put(name, mat);
    }

    /**
     * 清空图片列表
     */
    public void clearImages() {
        mats.clear();
    }

    /**
     * 获取图片列表
     */
    private void getImages() {
        StringBuilder sb = new StringBuilder();
        for (String key : mats.keySet()) {
            if (sb.length() > 0) {
                sb.append(",");
            }

            sb.append(key);
        }

        broadcast("images:" + sb.toString());
    }

    /**
     * 获取图片
     *
     * @param name 名称
     */
    private void getImage(String name) {
        if (!mats.containsKey(name)) {
            broadcast("image:");
            return;
        }

        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mats.get(name), buffer);
        String data = new String(Base64.getEncoder().encode(buffer.toArray()), StandardCharsets.ISO_8859_1);
        broadcast("image:" + URLEncoder.encode(data, Charset.forName("ascii")));
    }

    private String getWebSocketId(WebSocket webSocket) {
        return webSocket.getRemoteSocketAddress().getAddress().getHostAddress() + ":" + webSocket.getRemoteSocketAddress().getPort();
    }
}
