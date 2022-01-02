package kr.syeyoung.zombieshelpstart.websocket;

import kr.syeyoung.zombieshelpstart.bot.BotProvider;
import kr.syeyoung.zombieshelpstart.bot.impl.WebsocketBot;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import java.net.InetSocketAddress;

public class HelpstartWebsocketServer extends WebSocketServer {

    public HelpstartWebsocketServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("closed " + conn.getRemoteSocketAddress() + " with exit code " + code + " additional info: " + reason);

        if (conn.<WebsocketBot>getAttachment() == null) {
            return;
        }
        WebsocketBot wb = conn.<WebsocketBot>getAttachment();
        BotProvider.getInstance().removeBot(wb);
        wb.onChat("§§§§§§§§§§§§§§§§§§§§DISCONNECTED");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        JSONObject object = new JSONObject(message);
        String command = object.getString("command");

        if ("connect".equals(command)) {
            if (conn.<WebsocketBot>getAttachment() != null) {
                conn.close();
                return;
            }
            String username = object.getString("data");

            if (BotProvider.getInstance().getAvailableBotsList().contains(username.toLowerCase())) {
                BotProvider.getInstance().kickAll(username);
                conn.close();
                return;
            }


            WebsocketBot wb = new WebsocketBot(username, conn);
            wb.sendMessage("/p leave");
            wb.sendMessage("/lobby");
            conn.setAttachment(wb);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BotProvider.getInstance().addBot(wb);
        } else if ("disconnect".equals(command)) {
            if (conn.<WebsocketBot>getAttachment() == null) {
                conn.close();
                return;
            }
            WebsocketBot wb = conn.<WebsocketBot>getAttachment();
            conn.close();
            BotProvider.getInstance().removeBot(wb);
            wb.onChat("§§§§§§§§§§§§§§§§§§§§DISCONNECTED");
            conn.setAttachment(null);
        } else if ("chatReceived".equals(command)) {
            if (conn.<WebsocketBot>getAttachment() == null) {
                conn.close();
                return;
            }
            WebsocketBot wb = conn.<WebsocketBot>getAttachment();
            wb.onChat(object.getString("data"));
        }

        System.out.println("received message from "	+ conn.getRemoteSocketAddress() + ": " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        ex.printStackTrace();
        System.err.println("an error occurred on connection " + conn.getRemoteSocketAddress()  + ":" + ex);
    }

    @Override
    public void onStart() {
        System.out.println("server started successfully");
    }
}