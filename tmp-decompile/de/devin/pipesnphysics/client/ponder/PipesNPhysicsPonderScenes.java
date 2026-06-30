/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.AllBlocks
 *  com.simibubi.create.content.fluids.pipes.FluidPipeBlock
 *  com.simibubi.create.content.fluids.pump.PumpBlock
 *  com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
 *  com.simibubi.create.foundation.blockEntity.SmartBlockEntity
 *  com.simibubi.create.foundation.ponder.CreateSceneBuilder
 *  com.simibubi.create.infrastructure.ponder.AllCreatePonderTags
 *  net.createmod.ponder.api.PonderPalette
 *  net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
 *  net.createmod.ponder.api.scene.SceneBuilder
 *  net.createmod.ponder.api.scene.SceneBuildingUtil
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.material.Fluid
 *  net.minecraft.world.level.material.Fluids
 *  net.neoforged.neoforge.fluids.FluidStack
 *  net.neoforged.neoforge.fluids.capability.IFluidHandler$FluidAction
 *  net.neoforged.neoforge.fluids.capability.templates.FluidTank
 */
package de.devin.pipesnphysics.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import de.devin.pipesnphysics.client.ponder.PipesNPhysicsPonderTags;
import de.devin.pipesnphysics.compat.CreatePipeRendering;
import de.devin.pipesnphysics.engine.FlowSolver;
import de.devin.pipesnphysics.engine.Graph;
import de.devin.pipesnphysics.engine.GraphBuilder;
import de.devin.pipesnphysics.engine.Solution;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class PipesNPhysicsPonderScenes {
    private static final int LABEL = 36;
    private static final int LABEL_GAP = 10;

    private PipesNPhysicsPonderScenes() {
    }

    private static void labelIdle(SceneBuilder scene) {
        scene.idle(46);
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation tag = PipesNPhysicsPonderTags.PIPE_PHYSICS;
        helper.forComponents((Object[])new ResourceLocation[]{AllBlocks.FLUID_PIPE.getId()}).addStoryBoard("uphill", PipesNPhysicsPonderScenes::fluidDynamics, new ResourceLocation[]{tag}).addStoryBoard("siphon", PipesNPhysicsPonderScenes::siphonsAndSuction, new ResourceLocation[]{tag});
        helper.forComponents((Object[])new ResourceLocation[]{AllBlocks.MECHANICAL_PUMP.getId()}).addStoryBoard("uphill", PipesNPhysicsPonderScenes::fluidDynamics, new ResourceLocation[]{AllCreatePonderTags.KINETIC_APPLIANCES, tag}).addStoryBoard("siphon", PipesNPhysicsPonderScenes::siphonsAndSuction, new ResourceLocation[]{tag});
    }

    private static void fillTank(SceneBuilder scene, BlockPos pos) {
        scene.world().modifyBlockEntity(pos, FluidTankBlockEntity.class, tank -> {
            FluidTank inv = tank.getTankInventory();
            inv.fill(new FluidStack((Fluid)Fluids.WATER, inv.getCapacity()), IFluidHandler.FluidAction.EXECUTE);
        });
    }

    private static void setTankFill(SceneBuilder scene, BlockPos pos, float fillPercentage) {
        scene.world().modifyBlockEntity(pos, FluidTankBlockEntity.class, tank -> {
            FluidTank inv = tank.getTankInventory();
            inv.drain(inv.getCapacity(), IFluidHandler.FluidAction.EXECUTE);
            inv.fill(new FluidStack((Fluid)Fluids.WATER, (int)((float)inv.getCapacity() * fillPercentage)), IFluidHandler.FluidAction.EXECUTE);
        });
    }

    private static void showFlow(SceneBuilder scene, BlockPos seedPos) {
        scene.world().modifyBlockEntity(seedPos, SmartBlockEntity.class, be -> {
            Level level = be.getLevel();
            if (level == null) {
                return;
            }
            Graph graph = GraphBuilder.build(level, seedPos);
            Solution solution = FlowSolver.solve(level, graph);
            CreatePipeRendering.apply(level, graph, solution);
        });
    }

    private static void showFlowTicks(SceneBuilder scene, BlockPos seedPos, int ticks) {
        for (int i = 0; i < ticks; ++i) {
            PipesNPhysicsPonderScenes.showFlow(scene, seedPos);
            scene.idle(1);
        }
    }

    private static void clearFlowNetwork(SceneBuilder scene, BlockPos seedPos) {
        scene.world().modifyBlockEntity(seedPos, SmartBlockEntity.class, be -> {
            Level level = be.getLevel();
            if (level == null) {
                return;
            }
            Graph graph = GraphBuilder.build(level, seedPos);
            CreatePipeRendering.clearNetwork(level, graph);
        });
    }

    public static void fluidDynamics(SceneBuilder scene, SceneBuildingUtil util) {
        float progress;
        int i;
        scene.title("fluid_dynamics", "Fluid Dynamics");
        scene.scaleSceneView(0.6f);
        scene.idle(5);
        CreateSceneBuilder createScene = new CreateSceneBuilder(scene);
        BlockPos bottomTank = new BlockPos(1, 1, 3);
        BlockPos topTank = new BlockPos(4, 5, 3);
        BlockPos pumpPos = new BlockPos(3, 1, 3);
        BlockPos flowSeed = new BlockPos(4, 3, 3);
        BlockState gapPipe = (BlockState)((BlockState)AllBlocks.FLUID_PIPE.getDefaultState().setValue((Property)FluidPipeBlock.WEST, (Comparable)Boolean.valueOf(true))).setValue((Property)FluidPipeBlock.EAST, (Comparable)Boolean.valueOf(true));
        scene.world().showSection(util.select().layer(0), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(bottomTank), Direction.DOWN);
        scene.world().showSection(util.select().position(topTank), Direction.DOWN);
        scene.idle(10);
        scene.addKeyframe();
        PipesNPhysicsPonderScenes.fillTank(scene, topTank);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 3, 4, 5, 3).substract(util.select().position(pumpPos)), Direction.DOWN);
        scene.world().setBlock(pumpPos, gapPipe, true);
        scene.world().showSection(util.select().position(pumpPos), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(36).text("text_1").pointAt(util.vector().centerOf(topTank)).placeNearTarget();
        PipesNPhysicsPonderScenes.labelIdle(scene);
        scene.overlay().showText(36).text("text_2").pointAt(util.vector().centerOf(flowSeed)).placeNearTarget();
        scene.idle(8);
        for (i = 0; i < 45; ++i) {
            progress = (float)i / 45.0f;
            PipesNPhysicsPonderScenes.setTankFill(scene, topTank, 1.0f - 0.5f * progress);
            PipesNPhysicsPonderScenes.setTankFill(scene, bottomTank, 0.5f * progress);
            PipesNPhysicsPonderScenes.showFlow(scene, flowSeed);
            scene.idle(1);
        }
        scene.idle(15);
        scene.overlay().showText(36).text("text_3").pointAt(util.vector().centerOf(bottomTank)).placeNearTarget();
        PipesNPhysicsPonderScenes.labelIdle(scene);
        scene.addKeyframe();
        scene.overlay().showOutline(PonderPalette.BLUE, (Object)"horiz_run", util.select().fromTo(1, 1, 3, 3, 1, 3), 81);
        scene.overlay().showText(81).text("text_4").pointAt(util.vector().centerOf(2, 1, 3)).placeNearTarget();
        PipesNPhysicsPonderScenes.showFlowTicks(scene, flowSeed, 45);
        scene.idle(46);
        scene.addKeyframe();
        scene.world().setBlock(pumpPos, (BlockState)AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue((Property)PumpBlock.FACING, (Comparable)Direction.EAST), true);
        createScene.world().propagatePipeChange(pumpPos);
        scene.idle(30);
        scene.overlay().showText(36).text("text_5").pointAt(util.vector().centerOf(pumpPos)).placeNearTarget();
        PipesNPhysicsPonderScenes.labelIdle(scene);
        scene.addKeyframe();
        createScene.world().setKineticSpeed(util.select().everywhere(), 8.0f);
        scene.idle(5);
        scene.overlay().showText(55).text("text_6").pointAt(util.vector().centerOf(pumpPos)).placeNearTarget();
        for (i = 0; i < 55; ++i) {
            PipesNPhysicsPonderScenes.setTankFill(scene, bottomTank, 0.5f);
            PipesNPhysicsPonderScenes.setTankFill(scene, topTank, 0.08f);
            PipesNPhysicsPonderScenes.showFlow(scene, pumpPos);
            scene.idle(1);
        }
        scene.effects().indicateRedstone(topTank);
        scene.idle(10);
        scene.addKeyframe();
        scene.overlay().showText(36).text("text_7").pointAt(util.vector().centerOf(pumpPos)).placeNearTarget();
        scene.idle(46);
        createScene.world().setKineticSpeed(util.select().everywhere(), 64.0f);
        for (i = 0; i < 60; ++i) {
            progress = (float)i / 60.0f;
            PipesNPhysicsPonderScenes.setTankFill(scene, bottomTank, 0.5f - 0.5f * progress);
            PipesNPhysicsPonderScenes.setTankFill(scene, topTank, 0.08f + 0.42f * progress);
            PipesNPhysicsPonderScenes.showFlow(scene, pumpPos);
            scene.idle(1);
        }
        scene.effects().indicateSuccess(topTank);
        scene.idle(50);
        scene.markAsFinished();
    }

    public static void siphonsAndSuction(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("siphons", "Siphons & Suction");
        scene.scaleSceneView(0.6f);
        scene.idle(5);
        CreateSceneBuilder createScene = new CreateSceneBuilder(scene);
        BlockPos sourceTank = new BlockPos(0, 4, 0);
        BlockPos pump = new BlockPos(0, 4, 1);
        BlockPos crest = new BlockPos(0, 4, 2);
        BlockPos destTank = new BlockPos(4, 4, 2);
        BlockPos seedPipe = new BlockPos(2, 1, 2);
        scene.world().showSection(util.select().layer(0), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 4, 0, 0, 4, 1), Direction.DOWN);
        PipesNPhysicsPonderScenes.fillTank(scene, sourceTank);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 4, 4, 2), Direction.DOWN);
        scene.idle(15);
        scene.addKeyframe();
        scene.overlay().showOutline(PonderPalette.BLUE, (Object)"pull_side", util.select().fromTo(0, 1, 2, 0, 4, 2), 46);
        scene.overlay().showText(36).text("text_1").pointAt(util.vector().centerOf(pump)).placeNearTarget();
        PipesNPhysicsPonderScenes.labelIdle(scene);
        scene.addKeyframe();
        createScene.world().setKineticSpeed(util.select().everywhere(), 64.0f);
        scene.idle(5);
        scene.overlay().showText(50).text("text_2").pointAt(util.vector().centerOf(crest)).placeNearTarget();
        for (int i = 0; i < 50; ++i) {
            float progress = (float)i / 50.0f;
            PipesNPhysicsPonderScenes.setTankFill(scene, sourceTank, 1.0f - 0.45f * progress);
            PipesNPhysicsPonderScenes.setTankFill(scene, destTank, 0.45f * progress);
            PipesNPhysicsPonderScenes.showFlow(scene, seedPipe);
            scene.idle(1);
        }
        scene.idle(10);
        scene.addKeyframe();
        scene.overlay().showText(36).text("text_3").pointAt(util.vector().centerOf(crest)).placeNearTarget();
        PipesNPhysicsPonderScenes.labelIdle(scene);
        PipesNPhysicsPonderScenes.setTankFill(scene, sourceTank, 0.52f);
        PipesNPhysicsPonderScenes.setTankFill(scene, destTank, 0.48f);
        PipesNPhysicsPonderScenes.clearFlowNetwork(scene, seedPipe);
        scene.effects().indicateRedstone(crest);
        scene.overlay().showOutline(PonderPalette.RED, (Object)"crest", util.select().position(crest), 71);
        scene.overlay().showText(71).colored(PonderPalette.RED).text("text_4").pointAt(util.vector().centerOf(crest)).placeNearTarget();
        PipesNPhysicsPonderScenes.showFlowTicks(scene, seedPipe, 35);
        scene.idle(46);
        scene.idle(20);
        scene.overlay().showText(36).text("text_5").pointAt(util.vector().centerOf(pump)).placeNearTarget();
        PipesNPhysicsPonderScenes.labelIdle(scene);
        scene.markAsFinished();
    }
}
