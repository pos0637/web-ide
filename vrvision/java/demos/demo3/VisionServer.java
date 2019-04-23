import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class VisionServer {
    static {
        // -Djava.library.path=$PROJECT_DIR$\opencv\x64
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    private String image;
    private ConcurrentHashMap<String, Mat> mats = new ConcurrentHashMap<>();
    private WebSocketClient client;

    public VisionServer(int port) throws URISyntaxException {
        client = new WebSocketClient(new URI("ws://localhost:" + port)) {
            @Override
            public void onMessage(String message) {
                if (message.startsWith("captureResult:")) {
                    image = message.substring("captureResult:".length());
                } else if (message.startsWith("getImages")) {
                    getImages();
                } else if (message.startsWith("getImage:")) {
                    getImage(message.substring("getImage:".length()));
                }
            }

            @Override
            public void onOpen(ServerHandshake handshake) {
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
            }

            @Override
            public void onError(Exception ex) {
            }
        };
    }

    public void connect() {
        client.connect();
        while (!client.isOpen()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取图像
     *
     * @param timeout 超时时间
     * @return 图像
     */
    public byte[] capture(int timeout) {
        client.send("capture");

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

        client.send("images:" + sb.toString());
    }

    /**
     * 获取图片
     *
     * @param name 名称
     */
    private void getImage(String name) {
        if (!mats.containsKey(name)) {
            client.send("image:");
            return;
        }

        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", mats.get(name), buffer);
        String data = new String(Base64.getEncoder().encode(buffer.toArray()), StandardCharsets.ISO_8859_1);
        client.send("image:" + URLEncoder.encode(data, Charset.forName("ascii")));
    }

    private String getWebSocketId(WebSocket webSocket) {
        return webSocket.getRemoteSocketAddress().getAddress().getHostAddress() + ":" + webSocket.getRemoteSocketAddress().getPort();
    }
}
