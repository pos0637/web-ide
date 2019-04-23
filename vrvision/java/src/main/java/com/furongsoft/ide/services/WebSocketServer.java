package com.furongsoft.ide.services;

import com.furongsoft.core.misc.Tracker;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

/**
 * WebSocket服务器
 *
 * @author Alex
 */
@Component
public class WebSocketServer extends org.java_websocket.server.WebSocketServer {
    // @Value("${ide.websocketserver.port}")
    private static final int port = 8887;

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
        broadcast(s);
        Tracker.info(getWebSocketId(webSocket) + ": " + s);
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

    private String getWebSocketId(WebSocket webSocket) {
        return webSocket.getRemoteSocketAddress().getAddress().getHostAddress() + ":" + webSocket.getRemoteSocketAddress().getPort();
    }
}
