package FowlFlightForensics.util.string;

public class CaseTransformer {
    public static String toLowerFirstChar(String s) {
        StringBuilder builder = new StringBuilder();
        if (s != null) {
            s = s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
            builder.append(s);
        }
        return builder.toString();
    }
}
