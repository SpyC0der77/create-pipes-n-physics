package de.devin.pipesnphysics.engine.net;

import de.devin.pipesnphysics.PipesNPhysics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server → client: whether the hydraulic engine is enabled on this server. */
public record EngineConfigPayload(boolean enabled) implements CustomPacketPayload {
    public static final Type<EngineConfigPayload> TYPE =
            new Type<>(PipesNPhysics.asResource("engine_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EngineConfigPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeBoolean(payload.enabled),
                    buf -> new EngineConfigPayload(buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
