package info.mudbourn.mmstweaks.revive;

import goetic.mods.absolutrevive.common.damagesystem.PlayerDamageModel;
import goetic.mods.absolutrevive.common.util.CommonUtils;
import info.mudbourn.mmstweaks.MmsTweaks;
import info.mudbourn.mmstweaks.net.DownedStatus;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Server side of going down: the downed status, the self revive budget and succumbing to wounds.
 */
public final class Downed {

    public static final ResourceKey<DamageType> SUCCUMBED = ResourceKey.create(Registries.DAMAGE_TYPE, MmsTweaks.id("succumbed"));

    public static final int RESCUE_TICKS = 70;

    public static final int SELF_REVIVE_TICKS = Math.round(RESCUE_TICKS * 11.0F / 6.0F);

    private static final Map<UUID, Long> DOWNED_AT = new HashMap<>();

    private Downed() {}

    // Downed players can only be searched while keepInventory is on.
    public static boolean lootable(Player target) {
        return target.level() instanceof ServerLevel level && level.getGameRules().get(GameRules.KEEP_INVENTORY);
    }

    public static void onDowned(ServerPlayer player) {
        DOWNED_AT.put(player.getUUID(), player.level().getGameTime());
        sendStatus(player);
    }

    public static void sendStatus(ServerPlayer player) {
        ServerPlayNetworking.send(player, new DownedStatus(lootable(player)));
    }

    public static void selfRevive(ServerPlayer player) {
        if (!(CommonUtils.getDamageModel(player) instanceof PlayerDamageModel model) || !model.isUnconscious()) {
            return;
        }
        Long downedAt = DOWNED_AT.get(player.getUUID());
        if (downedAt == null || player.level().getGameTime() - downedAt < SELF_REVIVE_TICKS) {
            return;
        }
        int used = SelfRevives.used(player);
        DOWNED_AT.remove(player.getUUID());
        if (used >= SelfRevives.LIMIT) {
            succumb(player, model);
            return;
        }

        used++;
        SelfRevives.set(player, used);
        model.rescueFromCriticalState(player);
        ChatFormatting tone = used >= SelfRevives.LIMIT ? ChatFormatting.RED : ChatFormatting.WHITE;
        player.sendOverlayMessage(Component.translatable("mms_tweaks.downed.recovered").withStyle(tone));
    }

    private static void succumb(ServerPlayer player, PlayerDamageModel model) {
        ServerLevel level = player.level();
        model.clearUnconsciousState();
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SUCCUMBED));
        player.hurtServer(level, source, Float.MAX_VALUE);
        if (player.isAlive()) {
            player.setHealth(0.0F);
            player.die(source);
        }
    }
}
