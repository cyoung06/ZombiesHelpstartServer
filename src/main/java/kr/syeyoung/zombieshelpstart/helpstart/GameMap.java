package kr.syeyoung.zombieshelpstart.helpstart;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;

@AllArgsConstructor
public enum GameMap {
    DEAD_END("/play arcade_zombies_dead_end", Arrays.asList("de", "dead_end", "deadend"), Arrays.asList("office", "gallery", "apartments", "hotel", "power_station"), EnumSet.allOf(GameDifficulty.class), new Location(16,68, 17)),
    BAD_BLOOD("/play arcade_zombies_bad_blood", Arrays.asList("bb", "bad_blood", "badblood"), Arrays.asList("mansion", "library", "dungeon", "crypts", "balcony"), EnumSet.allOf(GameDifficulty.class), new Location(21,68,12)),
    ALIEN_ARCADIUM("/play arcade_zombies_alien_arcadium", Arrays.asList("aa", "alien","alien_arcadium","alienarcadium","arcadium"), Arrays.asList(""), EnumSet.of(GameDifficulty.NORMAL), new Location(0,0,0));

    @Getter
    private String command;

    @Getter
    private List<String> aliases;

    @Getter
    private List<String> chests;

    @Getter
    private EnumSet<GameDifficulty> allowedDifficulties;

    @Getter
    private Location chestLoc;

    private static Map<String, GameMap> map = new HashMap<>();
    static {
        for (GameMap gameMap : values()) {
            for (String aliases : gameMap.aliases) {
                map.put(aliases, gameMap);
            }
        }
    }

    public static GameMap getGameMap(String arg) {
        return map.get(arg.toLowerCase());
    }
}
