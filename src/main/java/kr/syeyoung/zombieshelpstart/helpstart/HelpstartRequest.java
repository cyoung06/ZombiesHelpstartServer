package kr.syeyoung.zombieshelpstart.helpstart;

import lombok.Builder;
import lombok.Data;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class HelpstartRequest {
    private GameDifficulty gameDifficulty;
    private GameMap gameMap;

    private TextChannel channel;
    private Member requestor;

    private List<String> usernames;

    private boolean bad;
    private List<String> chests;

    private List<Message> messages;

    private HelpStartSession session;

    private boolean canceled;
}
