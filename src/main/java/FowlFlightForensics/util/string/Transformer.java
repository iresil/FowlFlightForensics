package FowlFlightForensics.util.string;

public class Transformer {
    public static String toLowerCamelCase(String[] words) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (i == 0) {
                word = word.isEmpty() ? word : word.toLowerCase();
            } else {
                word = word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
            }
            builder.append(word);
        }
        return builder.toString();
    }

    public static String toLowerFirstChar(String s) {
        StringBuilder builder = new StringBuilder();
        if (s != null) {
            s = s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
            builder.append(s);
        }
        return builder.toString();
    }
}
