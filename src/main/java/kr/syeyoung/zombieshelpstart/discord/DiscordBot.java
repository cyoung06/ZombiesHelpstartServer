package kr.syeyoung.zombieshelpstart.discord;

import com.beust.jcommander.JCommander;
import kr.syeyoung.zombieshelpstart.bot.BotProvider;
import kr.syeyoung.zombieshelpstart.helpstart.GameDifficulty;
import kr.syeyoung.zombieshelpstart.helpstart.GameMap;
import kr.syeyoung.zombieshelpstart.helpstart.HelpstartExecutor;
import kr.syeyoung.zombieshelpstart.helpstart.HelpstartRequest;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DiscordBot extends ListenerAdapter {
    private JDA jda;
    public DiscordBot() throws LoginException, InterruptedException {
        this.jda = JDABuilder.createDefault("NzQ0MTEyMTIxNzMzODQwOTU3.Xzeeag.JZR3K8WMMaL-YwvhFEQiydoWMjQ") // No server uses this bot, because apparently ZL got raided.
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setStatus(OnlineStatus.DO_NOT_DISTURB).addEventListeners(this).setActivity(Activity.watching("Bots")).build();
        jda.awaitReady();
    }

    private static final List<Long> allowedChannels = Arrays.asList(344145510132744192L, 746168787073499176L);

    private static final Map<Long, HelpstartRequest> requests = new HashMap<>();

    private static final Map<Long, String> bans_discord = new HashMap<Long, String>() {{
    }};
    private static final Map<String, String> bans_mc = new HashMap<String, String>() {{
    }};

    private Set<Role> blacklistRoles = new HashSet<>();


    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {

        if (blacklistRoles == null) {
            blacklistRoles = jda.getGuildById("344145105868947457").getRoles().stream().filter(r -> r.getName().toLowerCase().contains("blacklist")).collect(Collectors.toSet());
        }

        if (event.getMessage().getContentRaw().equals("!reloadRoles")) {
            Member u = event.getMember();
            if (u.getRoles().contains(u.getGuild().getRoleById("361187594324934657")) || u.getIdLong() == 332836587576492033L) {
                blacklistRoles = jda.getGuildById("344145105868947457").getRoles().stream().filter(r -> r.getName().toLowerCase().contains("blacklist")).collect(Collectors.toSet());
                event.getTextChannel().sendMessage("Reloaded blacklist role list :: "+blacklistRoles.stream().map(Role::getName).collect(Collectors.joining(", "))).queue(msg -> {
                    event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(1, TimeUnit.MINUTES);
                });
            } else {
                event.getTextChannel().sendMessage("Est-ce que vous avez une role Staff ou vous etes un developer de le bot?").queue(msg -> {
                    event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(1, TimeUnit.MINUTES);
                });
            }
        }

        if (event.getMessage().getContentRaw().equalsIgnoreCase("!botinfo")) {
            String enrolled = String.join(", ", BotProvider.getInstance().getBots());
            String available = String.join(", ", BotProvider.getInstance().getAvailableBotsList());
            String queue =  String.join(", ", HelpstartExecutor.getInstance().getRequests());
            event.getTextChannel().sendMessage("connected to server: "+enrolled+ "\navailable for use "+available+"\n requests: "+queue).queue(msg -> {
                event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(15, TimeUnit.SECONDS);
            });
            return;
        }
        if (event.getMessage().getContentRaw().equals("!cancelAll")) {
            Member u = event.getMember();
            if (u.getRoles().contains(u.getGuild().getRoleById("361187594324934657")) || u.getIdLong() == 332836587576492033L) {
                HelpstartExecutor.getInstance().cancelAll();
                event.getTextChannel().sendMessage("Allo requestos haveo beeno canceloedo, @herre").queue(msg -> {
                    event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(1, TimeUnit.MINUTES);
                });
            } else {
                event.getTextChannel().sendMessage("Est-ce que vous avez une role Staff ou vous etes un developer de le bot?").queue(msg -> {
                    event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(1, TimeUnit.MINUTES);
                });
            }
        }

        if (allowedChannels.contains(event.getChannel().getIdLong())) {
            String content = event.getMessage().getContentRaw();

            Member author = Objects.requireNonNull(event.getMember());

            if (!content.startsWith("!helpstart")) return;

            if (bans_discord.containsKey(author.getIdLong())) {
                event.getTextChannel().sendMessage("You're banned from using the bot for reason: " +bans_discord.get(author.getIdLong())).queue(msg -> {
                    event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(5, TimeUnit.SECONDS);
                });
                return;
            }

            if (author.getRoles().stream().anyMatch(blacklistRoles::contains)) {
                event.getTextChannel().sendMessage("You're banned from using the bot for reason: you have been blacklisted from the bot by the moderators of zombies league").queue(msg -> {
                    event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(5, TimeUnit.SECONDS);
                });
                return;
            }


            String[] params = content.split(" ");
            String[] realParams = new String[params.length - 1];
            System.arraycopy(params, 1, realParams, 0, realParams.length);

            HelpstartArgument hsa = new HelpstartArgument();
            try {
                JCommander.newBuilder().addObject(hsa).build().parse(realParams);
            } catch (Exception e) {
                event.getTextChannel().sendMessage(
                        author.getAsMention()+", "+e.getMessage()+"\n\n" +
                        "**» How to use the command:**\n" +
                        "\n" +
                        "`!helpstart <-map <de/bb/aa>> [-difficulty <normal/hard/rip>] <-players <IGN_1> [IGN_2] [IGN_3]> [-badchests/-goodchests location_1 [location_2] [location_3] [...]]`\n" +
                        "<argument> = Required\n" +
                        "[argument] = Optional\n" +
                        "IGN = In Game Name, the people that will play in the game (1 is Solo, 2 names for a Duo and 3 for a Trio)\n" +
                        "\n" +
                        "Examples:\n" +
                        "➦ `!helpstart -map de -difficulty hard -players syeyoung`\n" +
                        "➦ `!helpstart -map de -players syeyoung -goodchests power`\n" +
                        "➦ `!helpstart -map bb -difficulty rip -players syeyoung Antek -badchests mansion library dungeon`\n" +
                        "➦ `!helpstart -map aa -difficulty normal -players syeyoung Antek Antimony`\n" +
                        "\n\nI did everything well, why doesn't it work? -> https://pastebin.com/AJX7cQLf\n" +
                        "*System coded by syeyoung and accounts provided by the community and Antek__.*").queue(msg -> {
                            event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(60, TimeUnit.SECONDS);
                });
                return;
            }

            boolean badchests = hsa.badchests != null;
            boolean goodchests = hsa.goodchests != null;
            if (badchests && goodchests) {
                event.getTextChannel().sendMessage(author.getAsMention()+" Please only specify badchest or only specify goodchest option").queue(msg -> {
                    event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(10, TimeUnit.SECONDS);
                });
                return;
            }

            String str;
            if ((str = validateChest(hsa.badchests, hsa.map)) != null || (str = validateChest(hsa.goodchests, hsa.map)) != null) {
                event.getTextChannel().sendMessage(author.getAsMention()+", "+str+" is not valid chest for "+hsa.map+".\nValid chest locations: "+hsa.map.getChests().stream().collect(Collectors.joining(", "))).queue(msg -> {
                    event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(10, TimeUnit.SECONDS);
                });
                return;
            }

            if (!hsa.map.getAllowedDifficulties().contains(hsa.gameDifficulty)) {
                event.getTextChannel().sendMessage(author.getAsMention()+", "+hsa.gameDifficulty+" is not valid difficulty for "+hsa.map+".\nValid difficulties: "+hsa.map.getAllowedDifficulties().stream().map(GameDifficulty::name).collect(Collectors.joining(", "))).queue(msg -> {
                    event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(10, TimeUnit.SECONDS);
                });;
                return;
            }

            for (String pl: hsa.players) {
                if (bans_mc.containsKey(pl.toLowerCase())) {
                    event.getTextChannel().sendMessage("The player "+pl+" is banned from using the bot for reason :" +bans_mc.get(pl.toLowerCase())).queue(msg -> {
                        event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(5, TimeUnit.SECONDS);
                    });
                    return;
                }

                if (blacklistRoles.size() != 0 && event.getGuild().getMembersWithRoles(blacklistRoles).stream().anyMatch(m -> m.getEffectiveName().toLowerCase().contains(pl.toLowerCase()))) {
                    event.getTextChannel().sendMessage("The player "+pl+" is banned from using the bot for reason : "+pl+" have been blacklisted from the bot by the moderators of zombies league").queue(msg -> {
                        event.getTextChannel().deleteMessages(Arrays.asList(msg, event.getMessage())).queueAfter(5, TimeUnit.SECONDS);
                    });
                    return;
                }
            }

            event.getTextChannel().sendMessage(author.getAsMention()+", your request has been added to the queue. The bot will ping you when it's going to invite you").queue(msg -> {
                msg.addReaction("❌").queue();
                HelpstartRequest hr = HelpstartRequest.builder()
                        .channel(event.getTextChannel())
                        .requestor(author)
                        .gameDifficulty(hsa.gameDifficulty)
                        .gameMap(hsa.map)
                        .usernames(hsa.players)
                        .chests(goodchests ? hsa.goodchests : badchests ? hsa.badchests : new ArrayList<>())
                        .bad(!goodchests)
                        .messages(new ArrayList<>())
                        .build();

                hr.getMessages().add(event.getMessage());
                hr.getMessages().add(msg);

                HelpstartExecutor.getInstance().addToQueue(hr);

                requests.put(msg.getIdLong(), hr);
            });
        }
    }

    @Override
    public void onMessageDelete(@Nonnull MessageDeleteEvent event) {
        requests.remove(event.getMessageIdLong());
    }

    @Override
    public void onMessageBulkDelete(@Nonnull MessageBulkDeleteEvent event) {
        event.getMessageIds().stream().map(Long::parseLong).forEach(requests::remove);
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        HelpstartRequest hr = requests.get(event.getMessageIdLong());
        if (hr == null) return;
        if (!Objects.requireNonNull(event.getUser()).isBot() && hr.getRequestor().getIdLong() != event.getUserIdLong()) {
            event.getReaction().removeReaction(event.getUser()).queue();
            return;
        }

        if (event.getReaction().getReactionEmote().getEmoji().equals("❌") && hr.getRequestor().getIdLong() == event.getUserIdLong()) {
            hr.setCanceled(true);
            if (hr.getSession() != null) {
                hr.getSession().cancel();
            } else {
                event.getChannel().sendMessage(hr.getRequestor().getAsMention()+", your helpstart request has been canceled").queue(m -> {
                    hr.getMessages().add(m);
                    event.getTextChannel().deleteMessages(hr.getMessages()).queueAfter(5, TimeUnit.SECONDS);
                });

                synchronized (BotProvider.getInstance()) {
                    BotProvider.getInstance().notifyAll();
                }
            }
        }
    }

    public String validateChest(List<String> chests, GameMap gameMap) {
        if (chests == null) return null;
        for (String str:chests) {
            if (!gameMap.getChests().contains(str)) return str;
        }
        return null;
    }
}
