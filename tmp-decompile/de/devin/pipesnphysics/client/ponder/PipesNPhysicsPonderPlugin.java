/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.createmod.ponder.api.registration.PonderPlugin
 *  net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
 *  net.createmod.ponder.api.registration.PonderTagRegistrationHelper
 *  net.minecraft.resources.ResourceLocation
 */
package de.devin.pipesnphysics.client.ponder;

import de.devin.pipesnphysics.client.ponder.PipesNPhysicsPonderScenes;
import de.devin.pipesnphysics.client.ponder.PipesNPhysicsPonderTags;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class PipesNPhysicsPonderPlugin
implements PonderPlugin {
    public String getModId() {
        return "pipesnphysics";
    }

    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PipesNPhysicsPonderScenes.register(helper);
    }

    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PipesNPhysicsPonderTags.register(helper);
    }
}
