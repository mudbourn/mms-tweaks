package info.mudbourn.mmstweaks.command.migrate;

import info.mudbourn.mmstweaks.command.data.Spot;
import info.mudbourn.mmstweaks.command.data.TweaksData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public final class EcMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger("mms_tweaks");
    private static final String MARKER = "ec-migrated";

    private EcMigration() {
    }

    public static void run(MinecraftServer server) {
        Path marker = TweaksData.directory(server).resolve(MARKER);
        Path playerData = server.getWorldPath(LevelResource.ROOT).resolve("modplayerdata");
        if (Files.exists(marker)) {
            return;
        }
        TweaksData data = TweaksData.get();
        int homes = 0;
        int nicks = 0;
        int skipped = 0;
        if (Files.isDirectory(playerData)) {
            List<Path> files;
            try (Stream<Path> listing = Files.list(playerData)) {
                files = listing.filter(path -> path.getFileName().toString().endsWith(".dat")).sorted().toList();
            } catch (IOException e) {
                LOGGER.error("Could not list {}", playerData, e);
                return;
            }
            for (Path file : files) {
                String name = file.getFileName().toString();
                UUID uuid;
                try {
                    uuid = UUID.fromString(name.substring(0, name.length() - 4));
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Skipping {}: not a player file", name);
                    skipped++;
                    continue;
                }
                try {
                    CompoundTag tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
                    int[] counts = importPlayer(server, data, uuid, tag);
                    homes += counts[0];
                    nicks += counts[1];
                    skipped += counts[2];
                } catch (IOException | RuntimeException e) {
                    LOGGER.error("Could not import {}", name, e);
                    skipped++;
                }
            }
        }
        data.save();
        try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "done");
        } catch (IOException e) {
            LOGGER.error("Could not write the migration marker", e);
        }
        LOGGER.info("Essential Commands import: {} homes, {} nicknames, {} skipped", homes, nicks, skipped);
    }

    private static int[] importPlayer(MinecraftServer server, TweaksData data, UUID uuid, CompoundTag tag) {
        int homes = 0;
        int nicks = 0;
        int skipped = 0;
        CompoundTag homeTags = tag.getCompoundOrEmpty("homes");
        String chosen = homeTags.contains("home") ? "home" : homeTags.keySet().stream().sorted().findFirst().orElse(null);
        for (String homeName : homeTags.keySet()) {
            if (!homeName.equals(chosen)) {
                LOGGER.info("Skipped home {} of {}", homeName, uuid);
                skipped++;
            }
        }
        if (chosen != null) {
            Optional<Spot> spot = readSpot(homeTags.getCompoundOrEmpty(chosen));
            if (spot.isPresent()) {
                if (data.homes.putIfAbsent(uuid.toString(), spot.get()) == null) {
                    homes++;
                }
            } else {
                LOGGER.warn("Skipped home {} of {}: unreadable", chosen, uuid);
                skipped++;
            }
        }
        Optional<String> nick = tag.getString("nickname").filter(text -> !text.isBlank());
        if (nick.isPresent()) {
            if (data.nicks.putIfAbsent(uuid.toString(), nick.get()) == null) {
                nicks++;
            }
        }
        if (homes + nicks > 0) {
            Optional<NameAndId> known = server.services().nameToIdCache().get(uuid);
            known.ifPresent(entry -> data.names.putIfAbsent(uuid.toString(), entry.name()));
        }
        return new int[] {homes, nicks, skipped};
    }

    private static Optional<Spot> readSpot(CompoundTag tag) {
        Optional<String> dim = tag.getString("WorldRegistryKey");
        Optional<Double> x = tag.getDouble("x");
        Optional<Double> y = tag.getDouble("y");
        Optional<Double> z = tag.getDouble("z");
        if (dim.isEmpty() || x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Spot(dim.get(), x.get(), y.get(), z.get(), tag.getFloatOr("headYaw", 0f), tag.getFloatOr("pitch", 0f)));
    }
}
