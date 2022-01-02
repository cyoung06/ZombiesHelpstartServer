package kr.syeyoung.zombieshelpstart.helpstart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
public class Location {
    private int x;
    private int y;
    private int z;
}
