package kr.syeyoung.zombieshelpstart.discord.converter;

import com.beust.jcommander.IStringConverter;
import kr.syeyoung.zombieshelpstart.helpstart.GameDifficulty;

public class DifficultyConverter implements IStringConverter<GameDifficulty> {

    @Override
    public GameDifficulty convert(String s) {
        return GameDifficulty.valueOf(s.toUpperCase());
    }
}
