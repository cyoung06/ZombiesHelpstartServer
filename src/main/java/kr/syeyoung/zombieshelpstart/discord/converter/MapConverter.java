package kr.syeyoung.zombieshelpstart.discord.converter;

import com.beust.jcommander.IStringConverter;
import kr.syeyoung.zombieshelpstart.helpstart.GameMap;

public class MapConverter implements IStringConverter<GameMap> {

    @Override
    public GameMap convert(String s) {
        GameMap gm = GameMap.getGameMap(s);
        if (gm == null) throw new IllegalArgumentException(s+" is not valid game");
        return gm;
    }
}
