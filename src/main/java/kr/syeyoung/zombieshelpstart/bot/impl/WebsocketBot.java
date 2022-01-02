package kr.syeyoung.zombieshelpstart.bot.impl;

import kr.syeyoung.zombieshelpstart.helpstart.GameDifficulty;
import kr.syeyoung.zombieshelpstart.bot.Bot;
import kr.syeyoung.zombieshelpstart.bot.ChatListener;
import kr.syeyoung.zombieshelpstart.websocket.WebsocketPayloadUtil;
import org.java_websocket.WebSocket;
import org.json.JSONArray;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class WebsocketBot implements Bot {
    private String username;
    private WebSocket webSocket;


    private List<ChatListener> chatListenerList = new CopyOnWriteArrayList<>();

    public WebsocketBot(String username, WebSocket webSocket) {
        this.username = username;
        this.webSocket = webSocket;
    }

    public String getName() {
        return username;
    }

    public void sendMessage(String message) {
        webSocket.send(WebsocketPayloadUtil.createCommand("chat", message).toString());
        chatListenerList.forEach(c -> c.onChat(this, "§§§ Sending... "+message));
    }

    public void tryOpenChest(int x, int y, int z) {
        webSocket.send(WebsocketPayloadUtil.createCommand("open", new JSONArray().put(x).put(y).put(z)).toString());
        chatListenerList.forEach(c -> c.onChat(this, "§§§ opening chest... "+x+","+y+","+z));
    }

    @Override
    public boolean isDisconnected() {
        return webSocket.isClosed() || webSocket.isClosing();
    }

    @Override
    public void disconnect(String reason) {
        webSocket.close(3000, reason);
    }

    public void addListener(ChatListener listener) {
        chatListenerList.add(listener);
    }

    public void removeListener(ChatListener listener) {
        chatListenerList.remove(listener);
    }

    public void onChat(final String chat) {
        chatListenerList.forEach(t -> t.onChat(this, chat));
    }

    public void selectDifficulty(GameDifficulty difficulty) {
        webSocket.send(WebsocketPayloadUtil.createCommand("difficulty", difficulty.name()).toString());
        chatListenerList.forEach(c -> c.onChat(this, "§§§ changing difficulty... "+difficulty.name()));
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof WebsocketBot && ((WebsocketBot) obj).getName().equals(this.getName());
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }
}
