package de.devin.pipesnphysics.handler;

import de.devin.pipesnphysics.PipesNPhysicsConfig;
import de.devin.pipesnphysics.engine.net.EngineConfigPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import de.devin.pipesnphysics.PipesNPhysics;

@EventBusSubscriber(modid = PipesNPhysics.ID)
public final class EngineConfigSyncHandler {
    private EngineConfigSyncHandler() {}

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onServerConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != PipesNPhysicsConfig.SERVER_SPEC) return;
        PacketDistributor.sendToAllPlayers(new EngineConfigPayload(PipesNPhysicsConfig.ENABLE_ENGINE.get()));
    }

    public static void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new EngineConfigPayload(PipesNPhysicsConfig.ENABLE_ENGINE.get()));
    }
}
