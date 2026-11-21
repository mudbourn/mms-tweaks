package info.mudbourn.mmstweaks.loops;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.item.ItemEntity;

/** Runs the Bubblellaneous tick loop from tracked entity sets instead of six entity selector scans a tick. */
public final class BubblellaneousTick {

    private static final Identifier LIGHT_REMOVE = fn("blocks/light/remove");
    private static final Identifier PLACER_TICK = fn("block_placer/tick");
    private static final Identifier PLAYER_TICK = fn("player/tick");
    private static final Identifier BLOCK_TICK = fn("blocks/tick");
    private static final Identifier DUMMY_TICK = fn("utils/dummy_player/tick_m");
    private static final Identifier ITEM_TICK = fn("utils/dropped_item/tick");
    private static final int PLACER_LIMIT = 8;

    private static final Set<Entity> MARKERS = new LinkedHashSet<>();
    private static final Set<Entity> FRAMES = new LinkedHashSet<>();
    private static final Set<Entity> DISPLAYS = new LinkedHashSet<>();
    private static final Set<Entity> MANNEQUINS = new LinkedHashSet<>();
    private static final Set<Entity> NEW_ITEMS = new LinkedHashSet<>();

    private BubblellaneousTick() {}

    private static Identifier fn(String path) {
        return Identifier.fromNamespaceAndPath("bubblellaneous", path);
    }

    static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> track(entity));
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> untrack(entity));
        ServerTickEvents.START_SERVER_TICK.register(BubblellaneousTick::tick);
    }

    private static Set<Entity> setFor(Entity entity) {
        if (entity instanceof Marker) return MARKERS;
        if (entity instanceof ItemFrame && !(entity instanceof GlowItemFrame)) return FRAMES;
        if (entity instanceof Display.ItemDisplay) return DISPLAYS;
        if (entity instanceof Mannequin) return MANNEQUINS;
        if (entity instanceof ItemEntity) return NEW_ITEMS;
        return null;
    }

    private static void track(Entity entity) {
        Set<Entity> set = setFor(entity);
        if (set != null) set.add(entity);
    }

    private static void untrack(Entity entity) {
        Set<Entity> set = setFor(entity);
        if (set != null) set.remove(entity);
    }

    private static void tick(MinecraftServer server) {
        if (!DatapackLoops.active(DatapackLoops.BUBBLELLANEOUS)) {
            return;
        }
        if (DatapackLoops.starting(DatapackLoops.BUBBLELLANEOUS)) {
            clear();
            for (ServerLevel level : server.getAllLevels()) {
                level.getAllEntities().forEach(BubblellaneousTick::track);
            }
        }
        for (Entity marker : tagged(MARKERS, "bbln.light")) {
            DatapackLoops.run(server, LIGHT_REMOVE, marker, true);
        }
        List<Entity> placers = tagged(FRAMES, "bbln.place");
        for (Entity frame : placers.subList(0, Math.min(PLACER_LIMIT, placers.size()))) {
            DatapackLoops.run(server, PLACER_TICK, frame, true);
        }
        for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
            DatapackLoops.run(server, PLAYER_TICK, player, true);
        }
        for (Entity display : tagged(DISPLAYS, "bbln.block")) {
            DatapackLoops.run(server, BLOCK_TICK, display, true);
        }
        for (Entity dummy : tagged(MANNEQUINS, "bbln.dummy_player")) {
            if (dummy.entityTags().contains("--bbln.loaded")) {
                DatapackLoops.run(server, DUMMY_TICK, dummy, true);
            }
        }
        List<Entity> items = new ArrayList<>(NEW_ITEMS);
        NEW_ITEMS.clear();
        for (Entity item : items) {
            if (!item.isRemoved() && !item.entityTags().contains("--bbln.item")) {
                DatapackLoops.run(server, ITEM_TICK, item, false);
            }
        }
    }

    private static List<Entity> tagged(Set<Entity> set, String tag) {
        List<Entity> out = new ArrayList<>();
        for (Entity entity : set) {
            if (!entity.isRemoved() && entity.entityTags().contains(tag)) out.add(entity);
        }
        return out;
    }

    private static void clear() {
        MARKERS.clear();
        FRAMES.clear();
        DISPLAYS.clear();
        MANNEQUINS.clear();
        NEW_ITEMS.clear();
    }
}
