package helper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public final class Logger {
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(?i)(password\\s*=\\s*)\\w+");
    private static volatile Level level = Level.INFO;
    private static final Instant START = Instant.now();

    private Logger() {
    }

    private enum Level {
        ERROR(0),
        WARN(1),
        INFO(2),
        DEBUG(3),
        TRACE(4);

        private final int priority;

        Level(int priority) {
            this.priority = priority;
        }

        boolean isEnabled(Level current) {
            return priority <= current.priority;
        }
    }

    public static void setLevel(String level) {
        if (level == null) {
            return;
        }

        try {
            Logger.level = Level.valueOf(level);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid log level: " + level, e);
        }
    }

    public static void error(String message, Object... args) {
        log(Level.ERROR, message, args);
    }

    public static void warn(String message, Object... args) {
        log(Level.WARN, message, args);
    }

    public static void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    public static void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
    }

    public static void trace(String message, Object... args) {
        log(Level.TRACE, message, args);
    }

    private static void log(Level messageLevel, String message, Object... args) {
        String now = LocalDateTime.now().format( DateTimeFormatter.ofPattern("DDD.HHmmss.SSS") );
        //String now = LocalDateTime.now().format( DateTimeFormatter.ofPattern("DDD.HHmmss.SSS") );

        Level currentLevel = level;

        if (!messageLevel.isEnabled(currentLevel)) {
            return;
        }

                // .append(Duration.between(START, Instant.now()).toSeconds())
        StringBuilder sb = new StringBuilder(message.length() + 48);
        sb.append('[')
                .append(messageLevel)
                .append("@")
                .append(now)
                .append("] ");

        appendFormatted(sb, message, args);

        System.err.println(redact(sb));
    }

    private static String redact(CharSequence text) {
        return PASSWORD_PATTERN.matcher(text).replaceAll("$1******");
    }

    private static void appendFormatted(Appendable target, String message, Object... args) throws RuntimeException {
        if (args == null || args.length == 0) {
            try {
                target.append(message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return;
        }

        StringBuilder sb = target instanceof StringBuilder builder
                ? builder
                : new StringBuilder(message.length() + 64);

        int argIndex = 0;
        int length = message.length();

        for (int i = 0; i < length; i++) {
            char current = message.charAt(i);

            if (current == '{' && i + 1 < length && message.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(args[argIndex++]);
                } else {
                    sb.append("{}");
                }
                i++;
            } else {
                sb.append(current);
            }
        }

        if (sb != target) {
            try {
                target.append(sb);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}