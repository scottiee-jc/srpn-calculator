package org.example;

import java.io.IOException;
import java.util.List;

public class SRPNUtil {

    public boolean isPowEquals(char[] chars) {
        for (int i = chars.length-1; i > 0; i--) {
            if (chars[i] == '=' && chars[i-1] == '^') {
                return true;
            }
        }
        return false;
    }

    public String stringBuilder(List<String> word) throws IOException {
        StringBuilder wordBuilder = new StringBuilder();
        for (String s : word) {
            wordBuilder.append(s);
        }
        return wordBuilder.toString();
    }
}
