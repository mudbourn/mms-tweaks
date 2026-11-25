package info.mudbourn.mmstweaks.command.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public record Spot(String dim, double x, double y, double z, float yaw, float pitch) {

    public static Spot of(Entity entity) {
        return new Spot(
            entity.level().dimension().identifier().toString(),
            entity.getX(),
            entity.getY(),
            entity.getZ(),
            entity.getYRot(),
            entity.getXRot()
        );
    }

    public ServerLevel level(MinecraftServer server) {
        Identifier id = Identifier.tryParse(dim);
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    public String describe() {
        return String.format("%.1f, %.1f, %.1f in %s", x, y, z, dim);
    }
}
