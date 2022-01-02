package kr.syeyoung.zombieshelpstart.bot;

import kr.syeyoung.zombieshelpstart.helpstart.GameDifficulty;

public interface Bot {
    String getName();

    void sendMessage(String message);

    void addListener(ChatListener listener);
    void removeListener(ChatListener listener);

    void selectDifficulty(GameDifficulty difficulty);

    void tryOpenChest(int x, int y, int z);

    boolean isDisconnected();

    void disconnect(String reason);
}
