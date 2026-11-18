package info.mudbourn.mmstweaks.client.xaero;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;

/**
 * Hides Xaero's deathpoints from each death and deletes them after the respawn unless the player copied the coordinates.
 */
public final class XaeroDeathpoints {

    private static final Logger LOGGER = LoggerFactory.getLogger("mms_tweaks");

    private static final List<Pending> PENDING = new ArrayList<>();

    private static boolean kept;

    private XaeroDeathpoints() {}

    public static void begin() {
        kept = false;
    }

    public static void captured(MinimapWorld world) {
        if (kept) {
            return;
        }
        for (WaypointSet set : world.getIterableWaypointSets()) {
            for (Waypoint waypoint : set.getWaypoints()) {
                if (waypoint.getPurpose() == WaypointPurpose.DEATH && !waypoint.isDisabled() && !tracked(waypoint)) {
                    waypoint.setDisabled(true);
                    PENDING.add(new Pending(world, set, waypoint));
                }
            }
        }
        save(world);
    }

    // Brings the hidden deathpoints back and stops them being deleted.
    public static void keep() {
        kept = true;
        for (Pending pending : PENDING) {
            pending.waypoint.setDisabled(false);
        }
        saveAll();
        PENDING.clear();
    }

    public static void discard() {
        for (Pending pending : PENDING) {
            pending.set.remove(pending.waypoint);
        }
        saveAll();
        PENDING.clear();
    }

    private static boolean tracked(Waypoint waypoint) {
        for (Pending pending : PENDING) {
            if (pending.waypoint == waypoint) {
                return true;
            }
        }
        return false;
    }

    private static void saveAll() {
        List<MinimapWorld> saved = new ArrayList<>();
        for (Pending pending : PENDING) {
            if (!saved.contains(pending.world)) {
                saved.add(pending.world);
                save(pending.world);
            }
        }
    }

    private static void save(MinimapWorld world) {
        MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (session == null) {
            return;
        }
        try {
            session.getWorldManagerIO().saveWorld(world);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("could not save Xaero waypoints after a deathpoint change", e);
        }
    }

    private record Pending(MinimapWorld world, WaypointSet set, Waypoint waypoint) {}
}
