package info.mudbourn.mmstweaks.log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.AbstractFilter;

/**
 * Moves known noise out of latest.log and into logs/junk/. The rules are regexes in
 * config/mms_tweaks/log-silencer.txt (written from the bundled default on first run).
 * A rule matches anywhere in the formatted message. When Uproot is loaded, its dead hooks
 * marked harmless or covered in uproot/dead-hooks.txt are silenced too.
 */
public final class LogSilencer extends AbstractFilter {

    private static final int KEEP_JUNK_FILES = 10;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter LINE_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static volatile boolean installed;

    private record Rule(String source, Pattern pattern, AtomicLong hits) {}

    private final List<Rule> rules;
    private final Pattern any;
    private final List<String[]> deadHooks;
    private final AtomicLong deadHookHits = new AtomicLong();
    private final BufferedWriter junk;

    private LogSilencer(List<Rule> rules, List<String[]> deadHooks, Path junkPath, BufferedWriter junk) {
        super(Result.DENY, Result.NEUTRAL);
        this.rules = rules;
        this.deadHooks = deadHooks;
        this.junk = junk;
        StringBuilder all = new StringBuilder();
        for (Rule r : rules) {
            if (!all.isEmpty()) all.append('|');
            all.append("(?:").append(r.pattern.pattern()).append(')');
        }
        this.any = all.isEmpty() ? null : Pattern.compile(all.toString());
    }

    /** Installs the filter once. Safe to call from several entrypoints. Never throws. */
    public static synchronized void install() {
        if (installed) return;
        installed = true;
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir();
            List<Rule> rules = loadRules(FabricLoader.getInstance().getConfigDir().resolve("mms_tweaks/log-silencer.txt"));
            List<String[]> deadHooks = loadDeadHooks();

            Path junkDir = gameDir.resolve("logs/junk");
            Files.createDirectories(junkDir);
            prune(junkDir);
            Path junkPath = junkDir.resolve(LocalDateTime.now().format(FILE_TIME) + ".log");
            BufferedWriter out = Files.newBufferedWriter(junkPath, StandardCharsets.UTF_8);

            LogSilencer filter = new LogSilencer(rules, deadHooks, junkPath, out);
            filter.start();

            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            config.getRootLogger().addFilter(filter);
            for (LoggerConfig lc : config.getLoggers().values()) {
                if (lc != config.getRootLogger()) lc.addFilter(filter);
            }
            ctx.updateLoggers();

            Runtime.getRuntime().addShutdownHook(new Thread(filter::close, "mms-log-silencer-close"));
            LogManager.getLogger("MMS Log Silencer").info(
                "Log silencer on: {} rules, {} Uproot dead hooks; silenced lines go to {}",
                rules.size(), deadHooks.size(), gameDir.relativize(junkPath));
        } catch (Throwable t) {
            LogManager.getLogger("MMS Log Silencer").warn("Log silencer could not start, nothing is filtered", t);
        }
    }

    @Override
    public Result filter(LogEvent event) {
        String msg;
        try {
            msg = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
        } catch (Throwable t) {
            return Result.NEUTRAL;
        }
        if (msg == null) return Result.NEUTRAL;
        if (matches(msg)) {
            write(event, msg);
            return Result.DENY;
        }
        return Result.NEUTRAL;
    }

    private boolean matches(String msg) {
        if (msg.startsWith("Dead hook: ")) {
            for (String[] hook : deadHooks) {
                if (msg.contains(hook[0]) && ("*".equals(hook[1]) || msg.contains(hook[0] + "." + hook[1] + " "))) {
                    deadHookHits.incrementAndGet();
                    return true;
                }
            }
        }
        if (any == null || !any.matcher(msg).find()) return false;
        for (Rule r : rules) {
            if (r.pattern.matcher(msg).find()) {
                r.hits.incrementAndGet();
                break;
            }
        }
        return true;
    }

    private synchronized void write(LogEvent event, String msg) {
        try {
            String time = LocalDateTime.ofInstant(Instant.ofEpochMilli(event.getTimeMillis()), ZoneId.systemDefault()).format(LINE_TIME);
            junk.write("[" + time + "] [" + event.getThreadName() + "/" + event.getLevel() + "] [" + event.getLoggerName() + "]: " + msg);
            junk.newLine();
            Throwable thrown = event.getThrown();
            if (thrown != null) {
                StringWriter sw = new StringWriter();
                thrown.printStackTrace(new PrintWriter(sw));
                junk.write(sw.toString());
            }
            junk.flush();
        } catch (IOException ignored) {
            // The junk file is best effort; the line is still kept out of latest.log.
        }
    }

    private synchronized void close() {
        try {
            junk.newLine();
            junk.write("--- silenced line counts ---");
            junk.newLine();
            if (deadHookHits.get() > 0) {
                junk.write(String.format(Locale.ROOT, "%8d  Uproot dead hooks (harmless/covered)%n", deadHookHits.get()));
            }
            List<Rule> sorted = new ArrayList<>(rules);
            sorted.sort(Comparator.comparingLong((Rule r) -> r.hits.get()).reversed());
            for (Rule r : sorted) {
                if (r.hits.get() > 0) junk.write(String.format(Locale.ROOT, "%8d  %s%n", r.hits.get(), r.source));
            }
            junk.close();
        } catch (IOException ignored) {
        }
    }

    private static List<Rule> loadRules(Path file) throws IOException {
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            try (InputStream in = LogSilencer.class.getResourceAsStream("/mms_tweaks/log-silencer.default.txt")) {
                if (in != null) Files.copy(in, file);
            }
        }
        List<Rule> rules = new ArrayList<>();
        if (!Files.exists(file)) return rules;
        for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            try {
                rules.add(new Rule(line, Pattern.compile(line), new AtomicLong()));
            } catch (Exception e) {
                LogManager.getLogger("MMS Log Silencer").warn("Skipping bad silencer rule: {} ({})", line, e.getMessage());
            }
        }
        return rules;
    }

    /** Uproot dead hooks whose reason starts with "harmless" or "covered", as {owner/with/slashes, handler or *}. */
    private static List<String[]> loadDeadHooks() {
        List<String[]> hooks = new ArrayList<>();
        try (InputStream in = LogSilencer.class.getResourceAsStream("/uproot/dead-hooks.txt")) {
            if (in == null) return hooks;
            for (String raw : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String line = raw.strip();
                int bar = line.indexOf('|');
                if (line.startsWith("#") || bar < 0) continue;
                String reason = line.substring(bar + 1).strip().toLowerCase(Locale.ROOT);
                if (!reason.startsWith("harmless") && !reason.startsWith("covered")) continue;
                String[] p = line.substring(0, bar).strip().split("\\s+");
                if (p.length == 2) hooks.add(p);
            }
        } catch (IOException ignored) {
        }
        return hooks;
    }

    private static void prune(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            List<Path> logs = files.filter(p -> p.getFileName().toString().endsWith(".log")).sorted().toList();
            for (int i = 0; i < logs.size() - (KEEP_JUNK_FILES - 1); i++) Files.deleteIfExists(logs.get(i));
        }
    }
}
