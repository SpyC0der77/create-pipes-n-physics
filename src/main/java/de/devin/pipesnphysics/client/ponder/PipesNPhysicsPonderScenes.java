package de.devin.pipesnphysics.client.ponder;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlock;
import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
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
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Ponder storyboards for pipe physics. Tank levels during equalization are scripted
 * to match communicating-vessels behaviour (equal waterline, not equal fill %),
 * and pipe visuals are driven by the real {@link FlowSolver} so the demo matches gameplay.
 */
public final class PipesNPhysicsPonderScenes {

    private PipesNPhysicsPonderScenes() {}

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ResourceLocation tag = PipesNPhysicsPonderTags.PIPE_PHYSICS;
        helper.forComponents(AllBlocks.FLUID_PIPE.getId())
                .addStoryBoard("uphill", PipesNPhysicsPonderScenes::fluidDynamics, tag)
                .addStoryBoard("siphon", PipesNPhysicsPonderScenes::siphonsAndSuction, tag);
        helper.forComponents(AllBlocks.MECHANICAL_PUMP.getId())
                .addStoryBoard("uphill", PipesNPhysicsPonderScenes::fluidDynamics,
                        AllCreatePonderTags.KINETIC_APPLIANCES, tag)
                .addStoryBoard("siphon", PipesNPhysicsPonderScenes::siphonsAndSuction, tag);
    }

    private static void fillTank(SceneBuilder scene, BlockPos pos) {
        scene.world().modifyBlockEntity(pos, FluidTankBlockEntity.class, tank -> {
            FluidTank inv = tank.getTankInventory();
            inv.fill(new FluidStack(Fluids.WATER, inv.getCapacity()), FluidAction.EXECUTE);
        });
    }

    private static void setTankFill(SceneBuilder scene, BlockPos pos, float fillPercentage) {
        scene.world().modifyBlockEntity(pos, FluidTankBlockEntity.class, tank -> {
            FluidTank inv = tank.getTankInventory();
            inv.drain(inv.getCapacity(), FluidAction.EXECUTE);
            inv.fill(new FluidStack(Fluids.WATER, (int) (inv.getCapacity() * fillPercentage)),
                    FluidAction.EXECUTE);
        });
    }

    /** Drive Create's pipe fill animation from one engine solve. */
    private static void showFlow(SceneBuilder scene, BlockPos seedPos) {
        scene.world().modifyBlockEntity(seedPos, SmartBlockEntity.class, be -> {
            Level level = be.getLevel();
            if (level == null) return;
            Graph graph = GraphBuilder.build(level, seedPos);
            Solution solution = FlowSolver.solve(level, graph);
            CreatePipeRendering.apply(level, graph, solution);
        });
    }

    private static void showFlowTicks(SceneBuilder scene, BlockPos seedPos, int ticks) {
        for (int i = 0; i < ticks; i++) {
            showFlow(scene, seedPos);
            scene.idle(1);
        }
    }

    private static void clearFlowNetwork(SceneBuilder scene, BlockPos seedPos) {
        scene.world().modifyBlockEntity(seedPos, SmartBlockEntity.class, be -> {
            Level level = be.getLevel();
            if (level == null) return;
            Graph graph = GraphBuilder.build(level, seedPos);
            CreatePipeRendering.clearNetwork(level, graph);
        });
    }

    private static void labelIdle(SceneBuilder scene) {
        scene.idle(46);
    }

    public static void fluidDynamics(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("fluid_dynamics", "Fluid Dynamics");
        scene.scaleSceneView(0.6f);
        scene.idle(5);
        CreateSceneBuilder createScene = new CreateSceneBuilder(scene);

        BlockPos bottomTank = new BlockPos(1, 1, 3);
        BlockPos topTank = new BlockPos(4, 5, 3);
        BlockPos pumpPos = new BlockPos(3, 1, 3);
        BlockPos flowSeed = new BlockPos(4, 3, 3);

        BlockState gapPipe = AllBlocks.FLUID_PIPE.getDefaultState()
                .setValue(FluidPipeBlock.WEST, true)
                .setValue(FluidPipeBlock.EAST, true);

        scene.world().showSection(util.select().layer(0), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().position(bottomTank), Direction.DOWN);
        scene.world().showSection(util.select().position(topTank), Direction.DOWN);
        scene.idle(10);
        scene.addKeyframe();
        fillTank(scene, topTank);
        scene.idle(10);
        scene.world().showSection(
                util.select().fromTo(1, 1, 3, 4, 5, 3).substract(util.select().position(pumpPos)),
                Direction.DOWN);
        scene.world().setBlock(pumpPos, gapPipe, true);
        scene.world().showSection(util.select().position(pumpPos), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(36).text("text_1")
                .pointAt(util.vector().centerOf(topTank)).placeNearTarget();
        labelIdle(scene);
        scene.overlay().showText(36).text("text_2")
                .pointAt(util.vector().centerOf(flowSeed)).placeNearTarget();
        scene.idle(8);

        // Communicating vessels: equal surface elevation, not equal fill %. The high
        // tank drains while the low one fills — it ends noticeably fuller.
        for (int i = 0; i < 45; i++) {
            float progress = i / 45.0f;
            setTankFill(scene, topTank, 1.0f - 0.92f * progress);
            setTankFill(scene, bottomTank, 0.5f * progress);
            showFlow(scene, flowSeed);
            scene.idle(1);
        }

        scene.idle(15);
        scene.overlay().showText(36).text("text_3")
                .pointAt(util.vector().centerOf(bottomTank)).placeNearTarget();
        labelIdle(scene);
        scene.addKeyframe();
        scene.overlay().showOutline(PonderPalette.BLUE, "horiz_run",
                util.select().fromTo(1, 1, 3, 3, 1, 3), 81);
        scene.overlay().showText(81).text("text_4")
                .pointAt(util.vector().centerOf(2, 1, 3)).placeNearTarget();
        showFlowTicks(scene, flowSeed, 45);
        scene.idle(46);
        scene.addKeyframe();

        scene.world().setBlock(pumpPos,
                AllBlocks.MECHANICAL_PUMP.getDefaultState().setValue(PumpBlock.FACING, Direction.EAST), true);
        createScene.world().propagatePipeChange(pumpPos);
        scene.idle(30);
        scene.overlay().showText(36).text("text_5")
                .pointAt(util.vector().centerOf(pumpPos)).placeNearTarget();
        labelIdle(scene);
        scene.addKeyframe();
        createScene.world().setKineticSpeed(util.select().everywhere(), 8.0f);
        scene.idle(5);
        scene.overlay().showText(55).text("text_6")
                .pointAt(util.vector().centerOf(pumpPos)).placeNearTarget();
        for (int i = 0; i < 55; i++) {
            setTankFill(scene, bottomTank, 0.5f);
            setTankFill(scene, topTank, 0.08f);
            showFlow(scene, pumpPos);
            scene.idle(1);
        }
        scene.effects().indicateRedstone(topTank);
        scene.idle(10);
        scene.addKeyframe();
        scene.overlay().showText(36).text("text_7")
                .pointAt(util.vector().centerOf(pumpPos)).placeNearTarget();
        scene.idle(46);
        createScene.world().setKineticSpeed(util.select().everywhere(), 64.0f);
        for (int i = 0; i < 60; i++) {
            float progress = i / 60.0f;
            setTankFill(scene, bottomTank, 0.5f - 0.5f * progress);
            setTankFill(scene, topTank, 0.08f + 0.42f * progress);
            showFlow(scene, pumpPos);
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
        fillTank(scene, sourceTank);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 2, 4, 4, 2), Direction.DOWN);
        scene.idle(15);
        scene.addKeyframe();
        scene.overlay().showOutline(PonderPalette.BLUE, "pull_side",
                util.select().fromTo(0, 1, 2, 0, 4, 2), 46);
        scene.overlay().showText(36).text("text_1")
                .pointAt(util.vector().centerOf(pump)).placeNearTarget();
        labelIdle(scene);
        scene.addKeyframe();
        createScene.world().setKineticSpeed(util.select().everywhere(), 64.0f);
        scene.idle(5);
        scene.overlay().showText(50).text("text_2")
                .pointAt(util.vector().centerOf(crest)).placeNearTarget();
        for (int i = 0; i < 50; i++) {
            float progress = i / 50.0f;
            setTankFill(scene, sourceTank, 1.0f - 0.45f * progress);
            setTankFill(scene, destTank, 0.45f * progress);
            showFlow(scene, seedPipe);
            scene.idle(1);
        }
        scene.idle(10);
        scene.addKeyframe();
        scene.overlay().showText(36).text("text_3")
                .pointAt(util.vector().centerOf(crest)).placeNearTarget();
        labelIdle(scene);
        setTankFill(scene, sourceTank, 0.52f);
        setTankFill(scene, destTank, 0.48f);
        clearFlowNetwork(scene, seedPipe);
        scene.effects().indicateRedstone(crest);
        scene.overlay().showOutline(PonderPalette.RED, "crest", util.select().position(crest), 71);
        scene.overlay().showText(71).colored(PonderPalette.RED).text("text_4")
                .pointAt(util.vector().centerOf(crest)).placeNearTarget();
        showFlowTicks(scene, seedPipe, 35);
        scene.idle(46);
        scene.idle(20);
        scene.overlay().showText(36).text("text_5")
                .pointAt(util.vector().centerOf(pump)).placeNearTarget();
        labelIdle(scene);
        scene.markAsFinished();
    }
}
