package info.mudbourn.mmstweaks.command.state;

import info.mudbourn.mmstweaks.command.config.CommandConfig;
import info.mudbourn.mmstweaks.command.data.Spot;
import info.mudbourn.mmstweaks.command.data.TweaksData;
import info.mudbourn.mmstweaks.command.migrate.EcMigration;
import info.mudbourn.mmstweaks.command.tier.DiscordTiers;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Abilities;

public final class PlayerState {

    public static final float BASE_FLY_SPEED = 0.05f;

    private PlayerState() {
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTED.register(PlayerState::started);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            DiscordTiers.stop();
            TweaksData.get().save();
            TweaksData.unload();
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> joined(server, handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> left(handler.getPlayer()));
        ServerPlayerEvents.AFTER_RESPAWN.register((old, player, alive) -> apply(player));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, from, to) -> apply(player));
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> Afk.active(sender));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
            !(entity instanceof ServerPlayer player
                && TweaksData.get().god.contains(player.getUUID().toString())
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                TweaksData data = TweaksData.get();
                data.deaths.put(player.getUUID().toString(), Spot.of(player));
                data.save();
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(PlayerState::tick);
    }

    public static void apply(ServerPlayer player) {
        TweaksData data = TweaksData.get();
        String uuid = player.getUUID().toString();
        Abilities abilities = player.getAbilities();
        boolean changed = false;
        if (data.fly.contains(uuid) && !abilities.mayfly) {
            abilities.mayfly = true;
            changed = true;
        }
        Float speed = data.flySpeeds.get(uuid);
        if (speed != null && abilities.getFlyingSpeed() != speed * BASE_FLY_SPEED) {
            abilities.setFlyingSpeed(speed * BASE_FLY_SPEED);
            changed = true;
        }
        if (changed) {
            player.onUpdateAbilities();
        }
        if (data.god.contains(uuid) && player.getFoodData().getFoodLevel() < 20) {
            player.getFoodData().setFoodLevel(20);
        }
    }

    private static void started(MinecraftServer server) {
        CommandConfig.load(server);
        TweaksData.load(server);
        DiscordTiers.start();
        EcMigration.run(server);
    }

    private static void joined(MinecraftServer server, ServerPlayer player) {
        TweaksData data = TweaksData.get();
        data.names.put(player.getUUID().toString(), player.getGameProfile().name());
        data.save();
        apply(player);
        Afk.active(player);
        DiscordTiers.refresh(server, player);
    }

    private static void left(ServerPlayer player) {
        TweaksData data = TweaksData.get();
        data.logouts.put(player.getUUID().toString(), Spot.of(player));
        data.save();
        Afk.forget(player.getUUID());
    }

    private static void tick(MinecraftServer server) {
        Afk.tick(server);
        DiscordTiers.tick(server);
        if (server.getTickCount() % 20 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                apply(player);
            }
        }
    }
}
