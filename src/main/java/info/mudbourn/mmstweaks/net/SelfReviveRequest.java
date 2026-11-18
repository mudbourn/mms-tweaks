package info.mudbourn.mmstweaks.net;

import info.mudbourn.mmstweaks.MmsTweaks;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by a downed client that finished holding the self revive key.
 */
public record SelfReviveRequest() implements CustomPacketPayload {

    public static final Type<SelfReviveRequest> TYPE = new Type<>(MmsTweaks.id("self_revive"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelfReviveRequest> CODEC = StreamCodec.unit(new SelfReviveRequest());

    @Override
    public Type<SelfReviveRequest> type() {
        return TYPE;
    }
}
