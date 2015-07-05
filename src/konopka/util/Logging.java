package konopka.util;

public class Logging {
    public static String prepare(String method, String... args) {
        StringBuilder sb = new StringBuilder(method);

        sb.append("(");
        for (String arg : args) {
            sb.append(arg);
            sb.append(", ");
        }
        if (args.length > 0) {
            sb.replace(sb.length() - 2, sb.length(), "");
        }
        sb.append(")");

        return sb.toString();
    }

    public static String prepare(String method) {
        return method + "()";
    }

    public static String prepareWithPart(String method, String part, String... args) {
        return prepare(method, args) + ":" + part;
    }
    public static String prepareWithPart(String method, String part) {
        return prepare(method) + ":" + part;
    }
}
