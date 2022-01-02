package kr.syeyoung.zombieshelpstart.bot;

import kr.syeyoung.zombieshelpstart.helpstart.HelpstartExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BotProvider {
    private List<Bot> existing = new ArrayList<Bot>();
    private List<Bot> available = new ArrayList<Bot>();

    private List<String> bots = new ArrayList<>();

    private static final BotProvider botProvider = new BotProvider();

    private BotProvider() {}

    public synchronized void addBot(Bot b) {
        if (b.isDisconnected()) return;
        if (!existing.contains(b)) {
            existing.add(b);
            bots.add(b.getName().toLowerCase());
        }
        if (!available.contains(b)) {
            available.add(b);
        }
        this.notifyAll();
    }

    public synchronized List<String> getAvailableBotsList() {
        return available.stream().map(Bot::getName).collect(Collectors.toList());
    }


    public synchronized void removeBot(Bot b) {
        available.remove(b);
        existing.remove(b);
        bots.remove(b.getName().toLowerCase());
    }

    public synchronized List<String> getBots() {
        return new ArrayList<>(bots);
    }

    public synchronized List<Bot> provide(int size) {
        List<Bot> b = new ArrayList<>(available.subList(0, size));
        available.removeAll(b);
        return b;
    }

    public synchronized void returnBot(List<Bot> bots) {
        for (Bot b : bots)
            if (!b.isDisconnected())
                if (available.stream().noneMatch(b2 -> b2.getName().equalsIgnoreCase(b.getName())))
                    available.add(b);
        this.notifyAll();
    }

    public synchronized void kickAll(String nickname) {
        for (Bot b : existing) {
            if (b.getName().equalsIgnoreCase(nickname)) {
                try {
                    b.disconnect("same username joined");
                } catch (Exception ignored) {}
            }
        }
    }

    public int getAvailableBots() {
        return available.size();
    }

    public static BotProvider getInstance() {
        return botProvider;
    }
}
