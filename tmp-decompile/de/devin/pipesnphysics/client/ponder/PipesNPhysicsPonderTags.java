/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllBlocks
 *  com.simibubi.create.AllItems
 *  com.simibubi.create.infrastructure.ponder.AllCreatePonderTags
 *  net.createmod.ponder.api.registration.PonderTagRegistrationHelper
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.ItemLike
 *  net.neoforged.neoforge.registries.DeferredHolder
 */
package de.devin.pipesnphysics.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import de.devin.pipesnphysics.PipesNPhysics;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class PipesNPhysicsPonderTags {
    public static final ResourceLocation PIPE_PHYSICS = PipesNPhysics.asResource("pipe_physics");

    private PipesNPhysicsPonderTags() {
    }

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        PonderTagRegistrationHelper entries = helper.withKeyFunction(DeferredHolder::getId);
        helper.registerTag(PIPE_PHYSICS).addToIndex().item((ItemLike)AllBlocks.MECHANICAL_PUMP, true, false).title("Pipe Physics").description("Realistic fluid pressure, lift limits, and flow for Create pipe networks").register();
        entries.addToTag(PIPE_PHYSICS).add((Object)AllBlocks.FLUID_PIPE).add((Object)AllBlocks.MECHANICAL_PUMP).add((Object)AllBlocks.FLUID_TANK).add((Object)AllItems.GOGGLES);
        entries.addToTag(AllCreatePonderTags.FLUIDS).add((Object)AllBlocks.FLUID_PIPE).add((Object)AllBlocks.MECHANICAL_PUMP).add((Object)AllBlocks.FLUID_TANK);
    }
}
