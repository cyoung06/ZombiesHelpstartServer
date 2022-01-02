package kr.syeyoung.zombieshelpstart.discord.converter;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.ParameterException;

import java.util.List;

public class PlayerValidator implements IValueValidator<List<String>> {

    @Override
    public void validate(String s, List<String> strings) throws ParameterException {
        if (strings.size() < 1 || strings.size() > 3) {
            throw new ParameterException("The bot can only help start solos, duos, and trios");
        }
    }
}
