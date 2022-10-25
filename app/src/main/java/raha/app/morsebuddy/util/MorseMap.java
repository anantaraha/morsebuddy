package raha.app.morsebuddy.util;

import androidx.annotation.Nullable;

import java.util.HashMap;

public class MorseMap {
    public static final String SIGN_DOT = "●";
    public static final String SIGN_DASH = "―";
    public static final String SIGN_SPACE_1U = "";
    public static final String SIGN_SPACE_3U = "  ";
    public static final String SIGN_SPACE_7U = "    ";

    private static final HashMap<String, Character> morseTable;
    private static final HashMap<Character, String> letterTable;

    static {
        morseTable = new HashMap<>();
        morseTable.put("●―", 'A');
        morseTable.put("―●●●", 'B');
        morseTable.put("―●―●", 'C');
        morseTable.put("―●●", 'D');
        morseTable.put("●", 'E');
        morseTable.put("●●―●", 'F');
        morseTable.put("――●", 'G');
        morseTable.put("●●●●", 'H');
        morseTable.put("●●", 'I');
        morseTable.put("●―――", 'J');
        morseTable.put("―●―", 'K');
        morseTable.put("●―●●", 'L');
        morseTable.put("――", 'M');
        morseTable.put("―●", 'N');
        morseTable.put("―――", 'O');
        morseTable.put("●――●", 'P');
        morseTable.put("――●―", 'Q');
        morseTable.put("●―●", 'R');
        morseTable.put("●●●", 'S');
        morseTable.put("―", 'T');
        morseTable.put("●●―", 'U');
        morseTable.put("●●●―", 'V');
        morseTable.put("●――", 'W');
        morseTable.put("―●●―", 'X');
        morseTable.put("―●――", 'Y');
        morseTable.put("――●●", 'Z');
        morseTable.put("―――――", '0');
        morseTable.put("●――――", '1');
        morseTable.put("●●―――", '2');
        morseTable.put("●●●――", '3');
        morseTable.put("●●●●―", '4');
        morseTable.put("●●●●●", '5');
        morseTable.put("―●●●●", '6');
        morseTable.put("――●●●", '7');
        morseTable.put("―――●●", '8');
        morseTable.put("――――●", '9');
        letterTable = new HashMap<>();
        letterTable.put('A', "●―");
        letterTable.put('B', "―●●●");
        letterTable.put('C', "―●―●");
        letterTable.put('D', "―●●");
        letterTable.put('E', "●");
        letterTable.put('F', "●●―●");
        letterTable.put('G', "――●");
        letterTable.put('H', "●●●●");
        letterTable.put('I', "●●");
        letterTable.put('J', "●―――");
        letterTable.put('K', "―●―");
        letterTable.put('L', "●―●●");
        letterTable.put('M', "――");
        letterTable.put('N', "―●");
        letterTable.put('O', "―――");
        letterTable.put('P', "●――●");
        letterTable.put('Q', "――●―");
        letterTable.put('R', "●―●");
        letterTable.put('S', "●●●");
        letterTable.put('T', "―");
        letterTable.put('U', "●●―");
        letterTable.put('V', "●●●―");
        letterTable.put('W', "●――");
        letterTable.put('X', "―●●―");
        letterTable.put('Y', "―●――");
        letterTable.put('Z', "――●●");
        letterTable.put('0', "―――――");
        letterTable.put('1', "●――――");
        letterTable.put('2', "●●―――");
        letterTable.put('3', "●●●――");
        letterTable.put('4', "●●●●―");
        letterTable.put('5', "●●●●●");
        letterTable.put('6', "―●●●●");
        letterTable.put('7', "――●●●");
        letterTable.put('8', "―――●●");
        letterTable.put('9', "――――●");
    }

    @Nullable
    public static Character morseToLetter(String morse) {
        return morseTable.get(morse);
    }

    @Nullable
    public static String letterToMorse(char c) {
        return letterTable.get(c);
    }
}
