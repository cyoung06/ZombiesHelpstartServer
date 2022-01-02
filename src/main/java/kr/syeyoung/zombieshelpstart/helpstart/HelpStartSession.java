package kr.syeyoung.zombieshelpstart.helpstart;

import kr.syeyoung.zombieshelpstart.bot.Bot;
import kr.syeyoung.zombieshelpstart.bot.BotProvider;
import kr.syeyoung.zombieshelpstart.bot.ChatListener;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class HelpStartSession extends CompletableFuture<Void> implements ChatListener {
    private static ScheduledExecutorService  threadPoolExecutor = Executors.newSingleThreadScheduledExecutor();

    private List<String> playerNames;
    private List<Bot> bots;

    private volatile Stage stage = Stage.INVITE;
    private volatile IntSupplier nextTask;

    private GameMap map;
    private GameDifficulty gameDifficulty;

    private HelpstartRequest helpstartRequest;

    private Map<String, Object> state = new HashMap<>();

    private int tries = 0;

    private Instant timeout;
    private Instant botGotChat;

    private List<String> logs = new ArrayList<>();

    public HelpStartSession(HelpstartRequest helpstartRequest) {
        this.helpstartRequest = helpstartRequest;
        this.playerNames = helpstartRequest.getUsernames().stream().map(String::toLowerCase).collect(Collectors.toList());
        if (playerNames.size() > 3) throw new IllegalArgumentException("too many players");

        for (String str : playerNames) {
            if (BotProvider.getInstance().getBots().contains(str.toLowerCase())) {
                helpstartRequest.getChannel().sendMessage(helpstartRequest.getRequestor().getAsMention()+", The bot couldn't help start because One of the players is bot now :/").queue(msg -> {helpstartRequest.getMessages().add(msg);
                helpstartRequest.getChannel().deleteMessages(helpstartRequest.getMessages()).queueAfter(10, TimeUnit.SECONDS);});
                this.completeExceptionally(new RuntimeException("One of the players is bot now :/"));
                return;
            }
        }


        bots = BotProvider.getInstance().provide(4-playerNames.size());
        bots.forEach(b -> b.addListener(this));

        this.map = helpstartRequest.getGameMap();
        this.gameDifficulty = helpstartRequest.getGameDifficulty();

        state.put("players", playerNames);
        state.put("bots", bots);
        state.put("map", map);
        state.put("request", helpstartRequest);
        state.put("difficulty", gameDifficulty);

        timeout = Instant.now().plusSeconds(60);
        botGotChat = Instant.now().plusSeconds(3);
        helpstartRequest.getChannel().sendMessage(helpstartRequest.getRequestor().getAsMention() + ", "+bots.get(0).getName()+" will invite you and your allies to the party").queue(m -> {
            helpstartRequest.getMessages().add(m);
            inviteThemAll();
        });

        threadPoolExecutor.schedule(this::check, 4, TimeUnit.SECONDS);
        threadPoolExecutor.schedule(this::check, 60, TimeUnit.SECONDS);
    }

    public int inviteThemAll() {
        System.out.println("inviting all");
        playerNames.forEach(s -> {
            bots.get(0).sendMessage("/p invite "+s);
        });
        bots.subList(1, bots.size()).forEach(b -> {
            b.sendMessage("/p leave");
            bots.get(0).sendMessage("/p invite "+b.getName());
        });
        stage = Stage.INVITE;
        nextTask = this::gotoLobby;
        return 60;
    }

    public int gotoLobby() {
        System.out.println("going to lobby");
        bots.get(0).sendMessage("/lobby");
        stage = Stage.GOTO_LOBBY;
        nextTask = this::warpOut;
        return 10;
    }

    public int warpOut() {
        System.out.println("warping out");
        bots.get(0).sendMessage("/p warp");
        stage = Stage.WARP_OUT;
        nextTask = this::joinGame;
        return 10;
    }

    public int joinGame() {
        System.out.println("joining game");
        state.put("bugged", false);
        threadPoolExecutor.schedule(() -> {
            bots.get(0).sendMessage(map.getCommand());
        }, 5, TimeUnit.SECONDS);
        stage = Stage.JOIN_GAME;
        nextTask = this::warpInToMakeSure;
        return 15;
    }

    public int warpInToMakeSure() {
        System.out.println("warp in to make sure");
        if ((boolean)state.get("bugged")) {
            bots.get(0).sendMessage("/pchat Hey, I rejoined other's game. Trying to type the warp command in 5 seconds.");
            return joinGame();
        }
        stage = Stage.WARP_IN_TO_MAKE_SURE;
        bots.get(0).sendMessage("/p warp");

        if (gameDifficulty == GameDifficulty.NORMAL) {
            nextTask = this::start;
        } else {
            nextTask = this::changeDifficulty;
        }
        return 10;
    }

    public int changeDifficulty() {
        System.out.println("changing difficulty");
        bots.get(0).selectDifficulty(gameDifficulty);
        stage = Stage.SELECT_DIFFICULTY;
        nextTask = this::start;
        return 10;
    }

    public int start() {
        System.out.println("waiting for start");
        stage = Stage.START;
        nextTask = this::checkChest;
        return 25;
    }

    public int checkChest() {
        System.out.println("checking chest");
        stage = Stage.CHECK_CHEST;
        if (helpstartRequest.getChests().size() == 0) {
            cleanUp();
        } else {
            nextTask = this::checkChest2;
            Location loc = helpstartRequest.getGameMap().getChestLoc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
            bots.get(0).tryOpenChest(loc.getX(), loc.getY(), loc.getZ());
        }
        return 20;
    }

    public void cancel() {
        error("Canceled by user");
    }

    public int checkChest2() {
        System.out.println("really checking chest");
        String chest = (String) state.get("chest");
        boolean contains = helpstartRequest.getChests().contains(chest);
        boolean bad = helpstartRequest.isBad();
        System.out.println("chest: "+chest + " / chests: "+ String.join(", ", helpstartRequest.getChests()) + " / contains " + contains + " / bad "+bad + " / xored " + (contains ^ bad) + " / tries "+tries);

        tries++;
        if (tries == 5) {
            bots.get(0).sendMessage("/pchat sry, it's bad chest but you're too unlucky getting bad chest 5 times in a row. I'm going to disband");
            error("Bad Chest but you're too lucky getting bad chest 5 times in a row, so just play");
            return 1;
        }

        // yes yes - re
        // yes no - not re
        // no yes - not re
        // no no - re

        if (bad ^ contains) {
            return cleanUp();
        } else {
            return gotoLobby();
        }
    }

    private static final List<String> advertisements = Arrays.asList(
            "Hope you do well! If you appreciate the bot and want to contribute alt accounts, go to #applications"
    );

    public int cleanUp() {
        System.out.println("cleaning up");
        stage = Stage.EVACUATE;
        nextTask = this::onDone;
        bots.get(0).sendMessage("/pchat Hope you do well! If you appreciate the bot and want to contribute alt accounts, go to #applications");
        bots.get(0).sendMessage("/p disband");
        for (Bot bot : bots) {
            try {
                bot.sendMessage("/lobby");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 20;
    }


    public int onDone() {
        System.out.println("done");
        stage = Stage.DONE;

        threadPoolExecutor.schedule(() -> {
            bots.forEach(b -> b.removeListener(this));
            BotProvider.getInstance().returnBot(bots);
        }, 5, TimeUnit.SECONDS);
        try {helpstartRequest.getChannel().deleteMessages(helpstartRequest.getMessages()).queueAfter(10, TimeUnit.SECONDS);
        } catch (Exception e) {e.printStackTrace();}
        this.complete(null);
        saveLogs(false);
        return 30;
    }

    public UUID saveLogs(boolean err) {
        UUID newUID = UUID.randomUUID();
        File parent  = new File("logs/"+(err ? "errors" : "plains"));
        parent.mkdirs();
        try {
            Files.write(new File(parent, newUID.toString()+".log").toPath(), logs);
            return newUID;
        } catch (IOException e) {
            return null;
        }
    }

    public void error(String error) {
        System.out.println("error - "+error);
        if (stage == Stage.DONE) return;
        stage = Stage.DONE;
        try {
            bots.get(0).sendMessage("/p disband");
        } catch (Exception e) {e.printStackTrace();}
            for (Bot bot : bots) {
                try {
                    bot.sendMessage("/p leave");
                    bot.sendMessage("/lobby");
                } catch (Exception e) {e.printStackTrace();}
            }
        UUID uid = saveLogs(true);
        helpstartRequest.getChannel().sendMessage(helpstartRequest.getRequestor().getAsMention()+", The bot couldn't help start because "+error+".\n\nThe log id for this request was "+uid.toString()+". If you believe the bot was bugged, send <@!332836587576492033> this log id").mention().queue(msg -> {helpstartRequest.getMessages().add(msg);
            helpstartRequest.getChannel().deleteMessages(helpstartRequest.getMessages()).queueAfter(10, TimeUnit.SECONDS);});

        helpstartRequest.getChannel().getJDA().openPrivateChannelById(332836587576492033L).queue(pc -> {
            pc.sendMessage("Error: "+uid.toString()).queue();
        });

        threadPoolExecutor.schedule(() -> {
            bots.forEach(b -> b.removeListener(this));
            BotProvider.getInstance().returnBot(bots);
        }, 5, TimeUnit.SECONDS);
        this.completeExceptionally(new RuntimeException(error));
    }

    Set<Bot> gotChat = new HashSet<>();

    public boolean check() {
        if (this.isDone()) return true;
        if (botGotChat != null && botGotChat.isBefore(Instant.now())) {

            System.out.print("Checking if they got chats");

            botGotChat = null;

            List<Bot> copy = new ArrayList<>(bots);
            copy.removeAll(gotChat);



            String error=  "";
            if (copy.contains(bots.get(0))) {
                try { bots.get(0).disconnect("not following directions"); } catch (Exception ignored) {}
                error("Main invite bot is not sending commands or sending responses to server");
                return true;
            } else {
                copy.forEach(bot -> {try {bot.disconnect("not following directions bot");} catch (Exception ignored) {}});
                error += "\nAt least one of the bots are not sending commands or sending responses to server - "+copy.stream().map(Bot::getName).collect(Collectors.joining(", "));
            }

            if (!copy.isEmpty())
                error(error);
            return true;
        }

        if (timeout.isBefore(Instant.now())) {
            error("Timed out");
            return true;
        }
        return false;
    }

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    @Override
    public void onChat(Bot b, String chat) {
        if (stage == null) return;
        if (stage == Stage.DONE) return;

        this.logs.add(sdf.format(new Date()) + " | "+b.getName()+": "+chat);
        checkExceptionalSituation(b, chat);
        if (chat.startsWith("§§§")) return;
        gotChat.add(b);

        if (check()) return;
        try {
            if (stage.isComplete(state, b, chat)) {
                int len = 0;
                timeout = Instant.now().plusSeconds(len = nextTask.getAsInt());
                threadPoolExecutor.schedule(this::check, len+1, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    public void checkExceptionalSituation(Bot b, String chat) {
        Matcher matcher;
        if ((matcher = Patterns.EXPIRED.matcher(chat)).matches()) {
            error("Party invite to "+Patterns.getRealName(matcher.group(1))+" expired");
        }  else if ((matcher = Patterns.LEFT_PARTY.matcher(chat)).matches()) {
            error(Patterns.getRealName(matcher.group(1))+" left the party");
        } else if ((matcher = Patterns.LEFT_PARTY_SERVER.matcher(chat)).matches()) {
            error(Patterns.getRealName(matcher.group(1))+" left the server");
        } else if ((matcher = Patterns.QUIT_GAME.matcher(chat)).matches()) {
            error(Patterns.getRealName(matcher.group(1))+" quit the game");
        } else if (chat.equalsIgnoreCase("§§§§§§§§§§§§§§§§§§§§DISCONNECTED")) {
            error("one of the bots disconnected");
        }
    }


    public static enum Stage implements CompleteChecker {
        INVITE {
            @Override
            public boolean isComplete(Map<String, Object> state, Bot b,  String chat) {
                List<Bot> bots = ((List<Bot>)state.get("bots"));


                Matcher m = Patterns.INVITE_REQ.matcher(chat);
                if (m.matches()) {
                    if (Patterns.getRealName(m.group(1)).equalsIgnoreCase(bots.get(0).getName())) {
                        b.sendMessage("/p accept " + bots.get(0).getName());
                        return false;
                    } else if (b == bots.get(0)) {
                        throw new RuntimeException("Someone named "+Patterns.getRealName(m.group(1)) +" tried to glitch the bot");
                    }
                }

                if (b != bots.get(0)) return false;


                m = Patterns.INVITED.matcher(chat);
                List<String> invited = (List<String>) state.getOrDefault("invited", new ArrayList<>());
                int invCount = (Integer) state.getOrDefault("invcount", 0);
                if (m.matches()) {
                    String inviter = Patterns.getRealName(m.group(1));
                    String target = Patterns.getRealName(m.group(2));

                    invited.add(target.toLowerCase());
                    state.put("invited", invited);
                    invCount++;
                }
                if (chat.equalsIgnoreCase("§cYou cannot invite that player since they have ignored you.")) {
                    invCount++;
                } else if (chat.equalsIgnoreCase("§cYou cannot invite that player.")) {
                    invCount++;
                } else if (chat.equalsIgnoreCase("§cCouldn't find a player with that name!")) {
                    invCount++;
                } else if (Patterns.CANT_INV.matcher(chat).matches()) {
                    invCount++;
                }
                state.put("invcount", invCount);

                if (invCount == 3) {
                    List<Bot> couldntinvbots = new ArrayList<>(bots).stream().filter(bot -> !invited.contains(bot.getName().toLowerCase()) && bot != bots.get(0)).collect(Collectors.toList());
                    List<String> couldntinvplayers = ((List<String>)state.get("players")).stream().filter(s -> !invited.contains(s)).collect(Collectors.toList());
                    if (!couldntinvbots.isEmpty() || !couldntinvplayers.isEmpty()) {
                        couldntinvbots.forEach(bot -> {
                            try {
                                bot.disconnect("Couldn't get invited");
                            } catch (Exception e) {}
                        });
                        throw new RuntimeException("Couldn't invite bots - "+couldntinvbots.stream().map(Bot::getName).collect(Collectors.joining(", ")) +"\nCoudln't invite players - "+String.join(", ",couldntinvplayers));
                    }
                    state.put("invcount", 100);
                    return false;
                }

                int accepts = (int) state.getOrDefault("accepts", 0);
                m = Patterns.JOINED.matcher(chat);
                if (m.matches()) {
                    System.out.println("MATCHES!");
                    String realName = Patterns.getRealName(m.group(1)).toLowerCase();
                    if (((List<String>)state.get("players")).contains(realName)) {
                        accepts ++;
                    } else if (bots.stream().anyMatch( b2 -> b2.getName().equalsIgnoreCase(realName))) {
                        accepts ++;
                    }
                    System.out.println("MATCHES! - "+accepts);
                }
                state.put("accepts", accepts);
                return accepts == 3;
            }
        }, GOTO_LOBBY {
            @Override
            public boolean isComplete(Map<String, Object> state, Bot b,  String chat) {
                List<Bot> bots = ((List<Bot>)state.get("bots"));
                if (b == bots.get(0)) {
                    if (chat.equalsIgnoreCase("§cYou are already connected to this server")) return true;
                    if (chat.replaceAll("§.", "").trim().isEmpty()) return true;
                }
                return false;
            }
        }, WARP_OUT {
            @Override
            public boolean isComplete(Map<String, Object> state, Bot b,  String chat) {
                List<Bot> bots = ((List<Bot>)state.get("bots"));
                if (b == bots.get(0)) {
                    if (chat.equalsIgnoreCase("§eYou summoned your party of §c3 §eto your server.")) return true;
                }
                return false;
            }
        }, JOIN_GAME {
            @Override
            public boolean isComplete(Map<String, Object> state, Bot b,  String chat) {
                List<Bot> bots = ((List<Bot>)state.get("bots"));
                if (b == bots.get(0)) {
                    if (chat.equals("§e§lTo leave Zombies, type /lobby")) {
                        state.put("bugged", true);
                        return true;
                    }
                    return Patterns.JOINED_GAME2.matcher(chat).matches();
//                    if (state.get("map") != GameMap.ALIEN_ARCADIUM) return chat.equals("§e§lYou joined as the party leader! Use the §5§lParty Options Menu §e§lto change game settings.");
//                    else return Patterns.JOINED_GAME2.matcher(chat).matches();
                }
                return false;
            }
        }, WARP_IN_TO_MAKE_SURE {
            @Override
            public boolean isComplete(Map<String, Object> state, Bot b,  String chat) {
                List<Bot> bots = ((List<Bot>)state.get("bots"));
                if (b == bots.get(0)) {
//                    if (Patterns.JOINED_GAME.matcher(chat).matches()) return true;
                    return chat.equals("§eYou summoned your party of §c3 §eto your server.");
                }
                return false;
            }
        }, SELECT_DIFFICULTY {
            @Override
            public boolean isComplete(Map<String, Object> state, Bot b,  String chat) {
                Matcher m = Patterns.DIFFICULTY_SELECTED.matcher(chat);
                if (m.matches()) {
                    String difficulty = m.group(2).replaceAll("§.", "").toUpperCase();
                    if (GameDifficulty.valueOf(difficulty) == state.get("difficulty")) {
                        return true;
                    } else {
                        throw new RuntimeException("the bots selected wrong difficulty :/ pls try again");
                    }
                }
                if (chat.trim().equalsIgnoreCase("§f§lZombies")) {
                    throw new RuntimeException("the bots didn't select difficulty :/ pls try again");
                }
                return false;
            }
        }, START {
            @Override
            public boolean isComplete(Map<String, Object> state, Bot b,  String chat) {
                return chat.trim().equalsIgnoreCase("§f§lZombies");
            }
        }, CHECK_CHEST {
            @Override
            public boolean isComplete(Map<String, Object> state, Bot b,  String chat) {
                List<Bot> bots = ((List<Bot>)state.get("bots"));
                if (b == bots.get(0)) {
                    Matcher m = Patterns.CHEST_LOCATION.matcher(chat);
                    if (!m.matches()) return false;
                    String chest = m.group(1);
                    state.put("chest", chest.toLowerCase().replace(" ", "_"));
                    return true;
                }
                return false;
            }
        }, EVACUATE {
            @Override
            public boolean isComplete(Map<String, Object> state, Bot b,  String chat) {
                List<Bot> bots = ((List<Bot>)state.get("bots"));
                if (b == bots.get(0)) {
                    if (chat.equalsIgnoreCase("§cYou are already connected to this server")) return true;
                    if (chat.replaceAll("§.", "").trim().isEmpty()) return true;
                }
                return false;
            }
        }, DONE {
            @Override
            public boolean isComplete(Map<String, Object> state, Bot b,  String chat) {
                return false;
            }
        }
    }

    public static interface CompleteChecker {
        boolean isComplete(Map<String,Object> state, Bot b, String chat);
    }
}
