package kr.syeyoung.zombieshelpstart;

import kr.syeyoung.zombieshelpstart.discord.DiscordBot;
import kr.syeyoung.zombieshelpstart.websocket.HelpstartWebsocketServer;
import org.java_websocket.server.WebSocketServer;

import javax.security.auth.login.LoginException;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws LoginException, InterruptedException {
        String host = "0.0.0.0";
        int port = 25560;

        WebSocketServer server = new HelpstartWebsocketServer(new InetSocketAddress(host, port));
        server.start();

        DiscordBot discordBot = new DiscordBot();
    }
}
