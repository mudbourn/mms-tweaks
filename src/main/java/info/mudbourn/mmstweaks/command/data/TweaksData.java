package info.mudbourn.mmstweaks.command.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TweaksData {

    private static final Logger LOGGER = LoggerFactory.getLogger("mms_tweaks");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TweaksData current = new TweaksData();

    public Map<String, String> names = new LinkedHashMap<>();
    public Map<String, Spot> homes = new LinkedHashMap<>();
    public Map<String, String> nicks = new ConcurrentHashMap<>();
    public Set<String> fly = new LinkedHashSet<>();
    public Set<String> god = new LinkedHashSet<>();
    public Map<String, Float> flySpeeds = new LinkedHashMap<>();
    public Map<String, Spot> deaths = new LinkedHashMap<>();
    public Map<String, Spot> logouts = new LinkedHashMap<>();

    private transient Path file;

    public static TweaksData get() {
        return current;
    }

    public static Path directory(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("mms_tweaks");
    }

    public static void load(MinecraftServer server) {
        Path dir = directory(server);
        Path file = dir.resolve("data.json");
        TweaksData loaded = null;
        if (Files.exists(file)) {
            try {
                loaded = GSON.fromJson(Files.readString(file), TweaksData.class);
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Could not read {}, moving it aside and starting empty", file, e);
                moveAside(file);
            }
        }
        if (loaded == null) {
            loaded = new TweaksData();
        }
        loaded.file = file;
        loaded.fillMissing();
        current = loaded;
    }

    private static void moveAside(Path file) {
        Path corrupt = file.resolveSibling("data.json.corrupt-" + System.currentTimeMillis());
        try {
            Files.move(file, corrupt, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.error("Moved the unreadable data file to {}", corrupt);
        } catch (IOException e) {
            LOGGER.error("Could not move {} aside", file, e);
        }
    }

    public static void unload() {
        current = new TweaksData();
    }

    public String uuidOf(String name) {
        for (Map.Entry<String, String> entry : names.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(name)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling("data.json.tmp");
            Files.writeString(temp, GSON.toJson(this));
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Could not write {}", file, e);
        }
    }

    private void fillMissing() {
        names = names == null ? new LinkedHashMap<>() : names;
        homes = homes == null ? new LinkedHashMap<>() : homes;
        nicks = nicks == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(nicks);
        fly = fly == null ? new LinkedHashSet<>() : fly;
        god = god == null ? new LinkedHashSet<>() : god;
        flySpeeds = flySpeeds == null ? new LinkedHashMap<>() : flySpeeds;
        deaths = deaths == null ? new LinkedHashMap<>() : deaths;
        logouts = logouts == null ? new LinkedHashMap<>() : logouts;
    }
}
