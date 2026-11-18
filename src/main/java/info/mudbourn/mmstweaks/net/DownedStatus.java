package info.mudbourn.mmstweaks.net;

import info.mudbourn.mmstweaks.MmsTweaks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells a downed client whether it can be looted.
 */
public record DownedStatus(boolean lootable) implements CustomPacketPayload {

    public static final Type<DownedStatus> TYPE = new Type<>(MmsTweaks.id("downed_status"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DownedStatus> CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        DownedStatus::lootable,
        DownedStatus::new
    );

    @Override
    public Type<DownedStatus> type() {
        return TYPE;
    }
}
