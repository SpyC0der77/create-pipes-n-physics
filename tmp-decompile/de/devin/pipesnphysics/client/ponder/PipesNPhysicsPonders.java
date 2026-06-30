/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllBlocks
 *  net.createmod.ponder.foundation.PonderScene
 *  net.minecraft.resources.ResourceLocation
 */
package de.devin.pipesnphysics.client.ponder;

import com.simibubi.create.AllBlocks;
import de.devin.pipesnphysics.PipesNPhysicsConfig;
import java.util.Set;
import net.createmod.ponder.foundation.PonderScene;
import net.minecraft.resources.ResourceLocation;

public final class PipesNPhysicsPonders {
    public static final Set<ResourceLocation> OVERRIDDEN_COMPONENTS = Set.of((Object)AllBlocks.FLUID_PIPE.getId(), (Object)AllBlocks.MECHANICAL_PUMP.getId());

    private PipesNPhysicsPonders() {
    }

    public static boolean useModPonders() {
        return (Boolean)PipesNPhysicsConfig.ENABLE_ENGINE.get();
    }

    public static boolean shouldHide(PonderScene scene) {
        if (!OVERRIDDEN_COMPONENTS.contains(scene.getLocation())) {
            return false;
        }
        if (PipesNPhysicsPonders.useModPonders()) {
            return "create".equals(scene.getNamespace());
        }
        return "pipesnphysics".equals(scene.getNamespace());
    }
}
