package info.mudbourn.mmstweaks.command.tier;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import info.mudbourn.mmstweaks.command.config.CommandConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DiscordTiers {

    public record Reading(Tier tier, String source) {
    }

    private record Result(Tier tier, String source, boolean fresh) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("mms_tweaks");
    private static final Gson GSON = new Gson();
    private static final int REFRESH_TICKS = 6000;
    private static final Map<UUID, Reading> READINGS = new ConcurrentHashMap<>();
    private static final Map<UUID, Tier> CACHE = new ConcurrentHashMap<>();
    private static HttpClient http;
    private static ExecutorService pool;

    private DiscordTiers() {
    }

    public static void start() {
        CACHE.clear();
        READINGS.clear();
        readCache();
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        pool = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "mms-discord-tiers");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static void stop() {
        if (pool != null) {
            pool.shutdownNow();
            pool = null;
        }
    }

    public static Tier tier(UUID uuid) {
        return reading(uuid).tier();
    }

    public static Reading reading(UUID uuid) {
        Reading reading = READINGS.get(uuid);
        if (reading != null) {
            return reading;
        }
        Tier cached = CACHE.get(uuid);
        return cached == null ? new Reading(Tier.PLAYER, "none") : new Reading(cached, "cache");
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % REFRESH_TICKS == 0) {
            refreshAll(server);
        }
    }

    public static void refreshAll(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            refresh(server, player);
        }
    }

    public static void refresh(MinecraftServer server, ServerPlayer player) {
        UUID uuid = player.getUUID();
        CommandConfig config = CommandConfig.get();
        String token = config.botToken();
        String guild = config.guild_id == null ? "" : config.guild_id.trim();
        if (token.isEmpty() || guild.isEmpty()) {
            apply(server, uuid, new Result(Tier.PLAYER, "off", false));
            return;
        }
        if (pool == null) {
            return;
        }
        Path linkFile = server.getServerDirectory().resolve(config.link_file);
        Map<String, List<String>> roles = config.tier_roles;
        pool.execute(() -> {
            Result result = fetch(uuid, token, guild, linkFile, roles);
            server.execute(() -> apply(server, uuid, result));
        });
    }

    private static void apply(MinecraftServer server, UUID uuid, Result result) {
        Reading old = reading(uuid);
        Tier tier = result.tier();
        String source = result.source();
        if (!result.fresh() && "error".equals(source)) {
            Tier cached = CACHE.get(uuid);
            tier = cached == null ? Tier.PLAYER : cached;
            source = cached == null ? "error" : "cache";
        }
        READINGS.put(uuid, new Reading(tier, source));
        if (result.fresh() && CACHE.put(uuid, tier) != tier) {
            writeCache();
        }
        if (old.tier() != tier) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                server.getCommands().sendCommands(player);
            }
        }
    }

    private static Result fetch(UUID uuid, String token, String guild, Path linkFile, Map<String, List<String>> roles) {
        try {
            String discordId = linkedDiscordId(linkFile, uuid);
            if (discordId == null) {
                return new Result(Tier.PLAYER, "unlinked", true);
            }
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                "https://discord.com/api/v10/guilds/" + guild + "/members/" + discordId
            ))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bot " + token)
                .GET()
                .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return new Result(Tier.PLAYER, "not in guild", true);
            }
            if (response.statusCode() != 200) {
                LOGGER.warn("Discord answered {} for {}", response.statusCode(), uuid);
                return new Result(Tier.PLAYER, "error", false);
            }
            return new Result(tierOf(response.body(), roles), "discord", true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(Tier.PLAYER, "error", false);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Discord tier lookup failed for {}: {}", uuid, e.toString());
            return new Result(Tier.PLAYER, "error", false);
        }
    }

    private static Tier tierOf(String body, Map<String, List<String>> roles) {
        JsonObject member = JsonParser.parseString(body).getAsJsonObject();
        JsonArray owned = member.has("roles") ? member.getAsJsonArray("roles") : new JsonArray();
        for (Tier tier : new Tier[] {Tier.OWNER, Tier.ADMIN, Tier.MODERATOR, Tier.TEMP_MOD}) {
            List<String> wanted = roles.get(tier.name());
            if (wanted == null) {
                continue;
            }
            for (JsonElement role : owned) {
                if (wanted.contains(role.getAsString())) {
                    return tier;
                }
            }
        }
        return Tier.PLAYER;
    }

    private static String linkedDiscordId(Path linkFile, UUID uuid) throws IOException {
        if (!Files.exists(linkFile)) {
            return null;
        }
        return findLink(JsonParser.parseString(Files.readString(linkFile)), uuid.toString().replace("-", ""));
    }

    private static String findLink(JsonElement element, String wanted) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("mcPlayerUUID") && object.has("discordID")) {
                String uuid = object.get("mcPlayerUUID").getAsString().replace("-", "");
                return uuid.equalsIgnoreCase(wanted) ? object.get("discordID").getAsString() : null;
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String found = findLink(entry.getValue(), wanted);
                if (found != null) {
                    return found;
                }
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                String found = findLink(child, wanted);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static Path cacheFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("mms_tweaks").resolve("tier-cache.json");
    }

    private static void readCache() {
        Path file = cacheFile();
        if (!Files.exists(file)) {
            return;
        }
        try {
            Map<String, String> stored = GSON.fromJson(Files.readString(file), new TypeToken<Map<String, String>>() {
            }.getType());
            if (stored == null) {
                return;
            }
            stored.forEach((uuid, tier) -> {
                try {
                    CACHE.put(UUID.fromString(uuid), Tier.valueOf(tier));
                } catch (IllegalArgumentException ignored) {
                    LOGGER.warn("Skipping bad tier cache entry {}", uuid);
                }
            });
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read the tier cache", e);
        }
    }

    private static void writeCache() {
        Path file = cacheFile();
        Map<String, String> stored = new TreeMap<>();
        CACHE.forEach((uuid, tier) -> stored.put(uuid.toString(), tier.name()));
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(stored));
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not write the tier cache", e);
        }
    }
}
