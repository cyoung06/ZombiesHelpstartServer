package kr.syeyoung.zombieshelpstart.helpstart;

import kr.syeyoung.zombieshelpstart.bot.BotProvider;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class HelpstartExecutor extends Thread {
    private Queue<HelpstartRequest> prioritizedHelpStartSessionQueue = new PriorityQueue<>(Comparator.<HelpstartRequest>comparingInt(o -> o.getUsernames().size()).reversed());
    private Queue<HelpstartRequest> normalHelpStartSessionQueue = new PriorityQueue<>(Comparator.<HelpstartRequest>comparingInt(o -> o.getUsernames().size()).reversed());

    private static final HelpstartExecutor helpstartExecutor = new HelpstartExecutor();

    static {
        helpstartExecutor.start();
    }

    public List<String> getRequests() {
        return normalHelpStartSessionQueue.stream().filter(h -> !h.isCanceled()).map(h -> h.getUsernames().size()+" players requested by "+h.getRequestor().getEffectiveName()).collect(Collectors.toList());
    }

    public void cancelAll() {
        synchronized (BotProvider.getInstance()) {
            normalHelpStartSessionQueue.forEach(hr -> hr.setCanceled(true));
            BotProvider.getInstance().notifyAll();
        }
    }

    public static HelpstartExecutor getInstance() {
        return helpstartExecutor;
    }



    public void addToQueue(HelpstartRequest helpstartRequest) {
        synchronized (BotProvider.getInstance()) {
            normalHelpStartSessionQueue.add(Objects.requireNonNull(helpstartRequest));
            prioritizedHelpStartSessionQueue.add(Objects.requireNonNull(helpstartRequest));
            BotProvider.getInstance().notifyAll();
        }
    }

    @Override
    public void run() {
        synchronized (BotProvider.getInstance()) {
            while(true) {
                try {
                    while (normalHelpStartSessionQueue.isEmpty()) {
                        try {
                            BotProvider.getInstance().wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    HelpstartRequest hr;
                    if (BotProvider.getInstance().getAvailableBots() < 3) {
                        while (prioritizedHelpStartSessionQueue.peek().getUsernames().size() + BotProvider.getInstance().getAvailableBots() < 4 && !prioritizedHelpStartSessionQueue.peek().isCanceled()) {
                            try {
                                BotProvider.getInstance().wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        hr = Objects.requireNonNull(prioritizedHelpStartSessionQueue.poll());
                        normalHelpStartSessionQueue.remove(hr);
                    } else {
                        while (normalHelpStartSessionQueue.peek().getUsernames().size() + BotProvider.getInstance().getAvailableBots() < 4 && !normalHelpStartSessionQueue.peek().isCanceled()) {
                            try {
                                BotProvider.getInstance().wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        hr = Objects.requireNonNull(normalHelpStartSessionQueue.poll());
                        prioritizedHelpStartSessionQueue.remove(hr);
                    }


                    if (!hr.isCanceled())
                        hr.setSession(new HelpStartSession(hr));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
