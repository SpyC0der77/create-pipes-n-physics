/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyReturnValue
 *  net.createmod.ponder.foundation.PonderTag
 *  net.createmod.ponder.foundation.registration.PonderTagRegistry
 *  net.minecraft.resources.ResourceLocation
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package de.devin.pipesnphysics.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import de.devin.pipesnphysics.client.ponder.PipesNPhysicsPonderTags;
import de.devin.pipesnphysics.client.ponder.PipesNPhysicsPonders;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.createmod.ponder.foundation.PonderTag;
import net.createmod.ponder.foundation.registration.PonderTagRegistry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={PonderTagRegistry.class}, remap=false)
public class PonderTagRegistryMixin {
    @ModifyReturnValue(method={"getListedTags()Ljava/util/List;"}, at={@At(value="RETURN")})
    private List<PonderTag> pipesnphysics$filterListedTags(List<PonderTag> original) {
        if (PipesNPhysicsPonders.useModPonders()) {
            return original;
        }
        ArrayList<PonderTag> filtered = new ArrayList<PonderTag>(original.size());
        for (PonderTag tag : original) {
            if (PipesNPhysicsPonderTags.PIPE_PHYSICS.equals((Object)tag.getId())) continue;
            filtered.add(tag);
        }
        return filtered;
    }

    @ModifyReturnValue(method={"getTags(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Set;"}, at={@At(value="RETURN")})
    private Set<PonderTag> pipesnphysics$filterItemTags(Set<PonderTag> original, ResourceLocation item) {
        if (PipesNPhysicsPonders.useModPonders()) {
            return original;
        }
        return (Set)original.stream().filter(tag -> !PipesNPhysicsPonderTags.PIPE_PHYSICS.equals((Object)tag.getId())).collect(Collectors.toUnmodifiableSet());
    }
}
