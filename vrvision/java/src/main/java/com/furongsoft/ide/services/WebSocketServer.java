package com.furongsoft.ide.services;

import com.furongsoft.core.misc.Tracker;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务器
 *
 * @author Alex
 */
@Component
public class WebSocketServer extends org.java_websocket.server.WebSocketServer {
    // @Value("${ide.websocketserver.port}")
    private static final int port = 8887;

    /**
     * 显示图片列表
     */
    private ConcurrentHashMap<String, String> images = new ConcurrentHashMap<>();

    public WebSocketServer() {
        super(new InetSocketAddress(port));
        this.start();
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        Tracker.info(getWebSocketId(webSocket) + " open");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        Tracker.info("close");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        Tracker.info(getWebSocketId(webSocket) + ": " + s);
        if (s.startsWith("getImages")) {
            getImages();
        } else if (s.startsWith("getImage:")) {
            getImage(s.substring("getImage:".length()));
        } else if (s.startsWith("saveImage:")) {
            saveImage(s.substring("saveImage:".length()));
        } else if (s.startsWith("clearImages")) {
            images.clear();
        } else {
            broadcast(s);
        }
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        Tracker.error(e);
    }

    @Override
    public void onStart() {
        Tracker.info("Server started!");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

    /**
     * 获取网络标识
     *
     * @param webSocket
     * @return 网络标识
     */
    private String getWebSocketId(WebSocket webSocket) {
        return webSocket.getRemoteSocketAddress().getAddress().getHostAddress() + ":" + webSocket.getRemoteSocketAddress().getPort();
    }

    /**
     * 获取图片列表
     */
    private void getImages() {
        StringBuilder sb = new StringBuilder();
        for (String key : images.keySet()) {
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
        if (!images.containsKey(name)) {
            broadcast("image:");
            return;
        }

        broadcast("image:" + images.get(name));
    }

    /**
     * 保存图片
     *
     * @param image 图片
     */
    private void saveImage(String image) {
        int pos = image.indexOf(',');
        if (pos < 0) {
            return;
        }

        images.put(image.substring(0, pos - 1), image.substring(pos + 1));
    }
}
