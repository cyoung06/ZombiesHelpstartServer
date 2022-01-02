package kr.syeyoung.zombieshelpstart.helpstart;

import java.util.regex.Pattern;

public class Patterns {
    public static final Pattern INVITE_REQ = Pattern.compile("§9§m-----------------------------\\n(.+) §ehas invited you to join their party!\\n§eYou have §c60 §eseconds to accept\\. §6Click here to join!\\n§9§m-----------------------------");

    public static final Pattern JOINED = Pattern.compile("(.+) §ejoined the party\\.");

    public static final Pattern CANT_INV = Pattern.compile("§cYou cannot invite that player since they're not online\\.");

    public static final Pattern EXPIRED = Pattern.compile("§eThe party invite to (.+) §ehas expired");

    public static final Pattern LEFT_PARTY = Pattern.compile("(.+) §ehas left the party\\.");

    public static final Pattern LEFT_PARTY_SERVER = Pattern.compile("(.+) §ehas disconnected, they have §c5 §eminutes to rejoin before they are removed from the party\\.");

    public static final Pattern JOINED_GAME = Pattern.compile("§e§lYou joined as the party leader! Use the §5§lParty Options Menu §e§lto change game settings\\.");

    public static final Pattern QUIT_GAME = Pattern.compile("(.+)§e has quit!");

    public static final Pattern DIFFICULTY_SELECTED = Pattern.compile("(.+) §eset Difficulty §eto (.+)§e.");

    public static final Pattern CHEST_LOCATION = Pattern.compile("§cThis Lucky Chest is not active right now! Find the active Lucky Chest in the (.+)!");

    public static final Pattern INVITED = Pattern.compile("(.+) §einvited (.+) §eto the party! They have §c60 §eseconds to accept.");

    public static final Pattern JOINED_GAME2 = Pattern.compile("(.+)§e has joined \\(§b.§e/§b.§e\\)!");

    public static String getRealName(String fancyName) {
        String noColor = fancyName.replaceAll("§.", "");
        String[] parts = noColor.split(" ");
        return parts[parts.length - 1];
    }
}
