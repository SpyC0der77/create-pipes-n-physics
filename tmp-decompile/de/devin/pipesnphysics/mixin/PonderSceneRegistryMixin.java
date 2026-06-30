/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.createmod.ponder.api.registration.StoryBoardEntry
 *  net.createmod.ponder.foundation.PonderScene
 *  net.createmod.ponder.foundation.registration.PonderSceneRegistry
 *  net.minecraft.resources.ResourceLocation
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package de.devin.pipesnphysics.mixin;

import de.devin.pipesnphysics.client.ponder.PipesNPhysicsPonders;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={PonderSceneRegistry.class}, remap=false)
public class PonderSceneRegistryMixin {
    @Inject(method={"compile(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/List;"}, at={@At(value="RETURN")})
    private void pipesnphysics$filterFluidScenes(ResourceLocation componentId, CallbackInfoReturnable<List<PonderScene>> cir) {
        if (!PipesNPhysicsPonders.OVERRIDDEN_COMPONENTS.contains(componentId)) {
            return;
        }
        PonderSceneRegistryMixin.applyFilter((List)cir.getReturnValue());
    }

    @Inject(method={"compile(Ljava/util/Collection;)Ljava/util/List;"}, at={@At(value="RETURN")})
    private void pipesnphysics$filterFluidScenes(Collection<StoryBoardEntry> entries, CallbackInfoReturnable<List<PonderScene>> cir) {
        List scenes = (List)cir.getReturnValue();
        boolean affectsOverridden = false;
        for (PonderScene scene : scenes) {
            if (!PipesNPhysicsPonders.OVERRIDDEN_COMPONENTS.contains(scene.getLocation())) continue;
            affectsOverridden = true;
            break;
        }
        if (!affectsOverridden) {
            return;
        }
        PonderSceneRegistryMixin.applyFilter(scenes);
    }

    private static void applyFilter(List<PonderScene> scenes) {
        if (scenes.isEmpty()) {
            return;
        }
        ArrayList<PonderScene> kept = new ArrayList<PonderScene>(scenes.size());
        for (PonderScene scene : scenes) {
            if (PipesNPhysicsPonders.shouldHide(scene)) continue;
            kept.add(scene);
        }
        if (kept.isEmpty()) {
            return;
        }
        scenes.clear();
        scenes.addAll(kept);
    }
}
