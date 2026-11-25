package info.mudbourn.mmstweaks.command.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("mms_tweaks");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Pattern BOT_TOKEN = Pattern.compile("(?m)^\\s*botToken\\s*=\\s*\"([^\"]*)\"");
    private static CommandConfig current = new CommandConfig();

    private transient String token = "";

    public String bot_token = "";
    public String guild_id = "";
    public String link_file = "DiscordIntegration-Data/LinkedPlayers.json";
    public String discord_config_file = "DiscordIntegration-Data/Discord-Integration.toml";
    public Map<String, List<String>> tier_roles = defaultRoles();
    public int afk_minutes = 15;
    public int rtp_radius = 1000;
    public int rtp_min = 1000;
    public int rtp_cooldown_seconds = 30;
    public List<String> rules = List.of("Be kind to other players.", "No griefing or stealing.", "Ask a moderator if unsure.");

    public static CommandConfig get() {
        return current;
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("mms_tweaks").resolve("commands.json");
    }

    public static void load(MinecraftServer server) {
        Path file = path();
        CommandConfig loaded = null;
        if (Files.exists(file)) {
            try {
                loaded = GSON.fromJson(Files.readString(file), CommandConfig.class);
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Could not read {}, using defaults", file, e);
            }
        }
        if (loaded == null) {
            loaded = new CommandConfig();
        }
        if (loaded.tier_roles == null) {
            loaded.tier_roles = defaultRoles();
        }
        if (loaded.rules == null) {
            loaded.rules = List.of();
        }
        loaded.token = loaded.resolveToken(server);
        current = loaded;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(loaded));
        } catch (IOException e) {
            LOGGER.error("Could not write {}", file, e);
        }
    }

    public String botToken() {
        return token;
    }

    private String resolveToken(MinecraftServer server) {
        if (bot_token != null && !bot_token.isBlank()) {
            return bot_token.trim();
        }
        try {
            Path file = server.getServerDirectory().resolve(discord_config_file);
            if (Files.exists(file)) {
                Matcher matcher = BOT_TOKEN.matcher(Files.readString(file));
                if (matcher.find()) {
                    return matcher.group(1).trim();
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read the Discord Integration config", e);
        }
        return "";
    }

    private static Map<String, List<String>> defaultRoles() {
        Map<String, List<String>> roles = new LinkedHashMap<>();
        roles.put("TEMP_MOD", List.of());
        roles.put("MODERATOR", List.of());
        roles.put("ADMIN", List.of());
        roles.put("OWNER", List.of());
        return roles;
    }
}
