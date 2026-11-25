package info.mudbourn.mmstweaks.mixin.chatpatches;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;
import java.util.List;

// Saves a chat message that cannot be encoded, such as an item tooltip built from the integrated
// server's registries, as plain text instead of failing the whole chat log save.
@Pseudo
@Mixin(targets = "obro1961.chatpatches.ChatLog", remap = false)
public class ChatLogSaveMixin {

    @ModifyArgs(
        method = "lambda$serialize$0",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/serialization/Codec;encodeStart(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"
        )
    )
    private static void mmsTweaks$dropUnsavableParts(Args args) {
        DynamicOps<Object> ops = args.get(0);
        Pair<List<Component>, Object> pair = args.get(1);
        List<Component> safe = new ArrayList<>(pair.getFirst().size());
        for (Component message : pair.getFirst()) {
            boolean ok;
            try {
                ok = ComponentSerialization.CODEC.encodeStart(ops, message).isSuccess();
            } catch (RuntimeException e) {
                ok = false;
            }
            safe.add(ok ? message : Component.literal(message.getString()));
        }
        if (safe.equals(pair.getFirst())) {
            return;
        }
        args.set(1, Pair.of(new it.unimi.dsi.fastutil.objects.ObjectArrayList<>(safe), pair.getSecond()));
    }
}
