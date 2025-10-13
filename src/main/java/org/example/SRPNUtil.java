package org.example;

import java.io.IOException;
import java.util.List;

public class SRPNUtil {


    public String stringBuilder(List<String> word) throws IOException {
        StringBuilder wordBuilder = new StringBuilder();
        for (String s : word) {
            wordBuilder.append(s);
        }
        return wordBuilder.toString();
    }
}
