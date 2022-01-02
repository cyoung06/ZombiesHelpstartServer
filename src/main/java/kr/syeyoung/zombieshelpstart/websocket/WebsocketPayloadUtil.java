package kr.syeyoung.zombieshelpstart.websocket;

import org.json.JSONObject;

public class WebsocketPayloadUtil {
    public static JSONObject createCommand(String command, Object data) {
        return new JSONObject().put("command", command).put("data", data);
    }
}
