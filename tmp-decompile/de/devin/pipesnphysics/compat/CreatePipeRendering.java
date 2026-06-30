/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.simibubi.create.content.fluids.FluidPropagator
 *  com.simibubi.create.content.fluids.FluidTransportBehaviour
 *  com.simibubi.create.content.fluids.PipeConnection
 *  com.simibubi.create.content.fluids.PipeConnection$Flow
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.Level
 *  net.neoforged.neoforge.fluids.FluidStack
 */
package de.devin.pipesnphysics.compat;

import com.simibubi.create.content.fluids.FluidPropagator;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import de.devin.pipesnphysics.compat.SableCompat;
import de.devin.pipesnphysics.engine.Edge;
import de.devin.pipesnphysics.engine.EdgeFlow;
import de.devin.pipesnphysics.engine.Graph;
import de.devin.pipesnphysics.engine.Node;
import de.devin.pipesnphysics.engine.Solution;
import de.devin.pipesnphysics.mixin.PipeConnectionAccessor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

public final class CreatePipeRendering {
    private static final float FILL_PRESSURE_PER_MBPT = 0.6f;
    private static final float MIN_FILL_PRESSURE = 1.0f;
    private static final float MAX_FILL_PRESSURE = 128.0f;
    private static final int DRAIN_INTERVAL_TICKS = 4;
    private static final double SUBMERSION_EPS = 0.05;

    private CreatePipeRendering() {
    }

    public static void clearNetwork(Level level, Graph graph) {
        for (BlockPos cell : graph.coverage()) {
            CreatePipeRendering.clearCell(level, cell);
        }
    }

    public static boolean apply(Level level, Graph graph, Solution solution) {
        HashSet<BlockPos> filled = new HashSet<BlockPos>();
        boolean draining = false;
        for (Edge edge : graph.edges()) {
            if (solution.stalledEdges().contains(edge.index()) && solution.edgeReasons().get(edge.index()) == Solution.Reason.SOURCE_DRY) continue;
            EdgeFlow flow = solution.edgeFlows().get(edge.index());
            FluidStack flowing = solution.edgeFluids().getOrDefault(edge.index(), FluidStack.EMPTY);
            if (!flowing.isEmpty() && flow.direction() != EdgeFlow.Direction.NONE) {
                CreatePipeRendering.chargeEdge(level, graph, edge, flowing, flow.direction() == EdgeFlow.Direction.A_TO_B, flow.mbPerTick(), filled);
                continue;
            }
            if (CreatePipeRendering.isBackedUp(solution, edge)) {
                filled.addAll(edge.pipes());
                continue;
            }
            FluidStack resting = solution.restFluids().getOrDefault(edge.index(), FluidStack.EMPTY);
            Double headA = solution.nodeHeads().get(edge.a());
            Double headB = solution.nodeHeads().get(edge.b());
            if (!resting.isEmpty() && headA != null && headB != null) {
                draining |= CreatePipeRendering.restEdge(level, graph, edge, resting, headA, headB, filled, level.getGameTime(), edge.index());
                continue;
            }
            draining |= CreatePipeRendering.drainDeadEdge(level, edge, filled, level.getGameTime());
        }
        for (BlockPos cell : graph.coverage()) {
            if (filled.contains(cell)) continue;
            CreatePipeRendering.clearCell(level, cell);
        }
        return draining;
    }

    private static boolean isBackedUp(Solution solution, Edge edge) {
        if (solution.noHeadEdges().contains(edge.index())) {
            return true;
        }
        return solution.stalledEdges().contains(edge.index()) && solution.edgeReasons().get(edge.index()) == Solution.Reason.SINK_FULL;
    }

    public static boolean deliveryReady(Level level, Graph graph, Solution solution, Solution.Transfer transfer) {
        Node sink = graph.nodeAt(transfer.to());
        if (sink == null) {
            return true;
        }
        boolean travellingFeeder = false;
        for (Edge edge : graph.edgesOf(sink.index())) {
            FluidStack carried;
            EdgeFlow flow = solution.edgeFlows().get(edge.index());
            if (flow.direction() == EdgeFlow.Direction.NONE) continue;
            boolean bl = edge.b() == sink.index() ? flow.direction() == EdgeFlow.Direction.A_TO_B : flow.direction() == EdgeFlow.Direction.B_TO_A;
            boolean towardSink = bl;
            if (!towardSink || !(carried = solution.edgeFluids().getOrDefault(edge.index(), FluidStack.EMPTY)).isEmpty() && !FluidStack.isSameFluidSameComponents((FluidStack)carried, (FluidStack)transfer.fluid())) continue;
            if (edge.pipes().isEmpty()) {
                return true;
            }
            if (CreatePipeRendering.frontReachedNode(level, graph, edge, sink.index())) {
                return true;
            }
            travellingFeeder = true;
        }
        return !travellingFeeder;
    }

    private static boolean frontReachedNode(Level level, Graph graph, Edge edge, int nodeIndex) {
        List<BlockPos> pipes = edge.pipes();
        BlockPos endCell = edge.b() == nodeIndex ? pipes.get(pipes.size() - 1) : pipes.get(0);
        FluidTransportBehaviour pipe = FluidPropagator.getPipe((BlockGetter)level, (BlockPos)endCell);
        if (pipe == null) {
            return true;
        }
        Direction towardNode = CreatePipeRendering.direction(endCell, graph.node(nodeIndex).pos());
        if (towardNode == null) {
            return true;
        }
        PipeConnection conn = pipe.getConnection(towardNode);
        return conn == null || CreatePipeRendering.isComplete(conn);
    }

    private static void chargeEdge(Level level, Graph graph, Edge edge, FluidStack fluid, boolean flowFromA, int mbPerTick, Set<BlockPos> filled) {
        BlockPos cell;
        FluidTransportBehaviour pipe;
        List pipes = edge.pipes();
        if (pipes.isEmpty()) {
            return;
        }
        List order = flowFromA ? pipes : pipes.reversed();
        BlockPos upstream = (flowFromA ? graph.node(edge.a()) : graph.node(edge.b())).pos();
        BlockPos downstream = (flowFromA ? graph.node(edge.b()) : graph.node(edge.a())).pos();
        float pressure = CreatePipeRendering.flowPressure(mbPerTick);
        boolean reached = true;
        for (int j = 0; j < order.size() && reached && (pipe = FluidPropagator.getPipe((BlockGetter)level, (BlockPos)(cell = (BlockPos)order.get(j)))) != null; ++j) {
            BlockPos up = j == 0 ? upstream : (BlockPos)order.get(j - 1);
            BlockPos down = j == order.size() - 1 ? downstream : (BlockPos)order.get(j + 1);
            Direction inDir = CreatePipeRendering.direction(cell, up);
            Direction outDir = CreatePipeRendering.direction(cell, down);
            if (inDir == null || outDir == null) break;
            PipeConnection inC = pipe.getConnection(inDir);
            PipeConnection outC = pipe.getConnection(outDir);
            if (inC == null || outC == null) break;
            boolean changed = CreatePipeRendering.seedCharging(inC, true, fluid, pressure);
            changed = CreatePipeRendering.isComplete(inC) ? (changed |= CreatePipeRendering.seedCharging(outC, false, fluid, pressure)) : (changed |= CreatePipeRendering.clearFlow(outC));
            if (changed) {
                pipe.blockEntity.notifyUpdate();
            }
            filled.add(cell);
            reached = CreatePipeRendering.isComplete(inC) && CreatePipeRendering.isComplete(outC);
        }
    }

    private static boolean restEdge(Level level, Graph graph, Edge edge, FluidStack fluid, double headA, double headB, Set<BlockPos> filled, long gameTime, int edgeIndex) {
        List<BlockPos> pipes = edge.pipes();
        BlockPos aEnd = graph.node(edge.a()).pos();
        BlockPos bEnd = graph.node(edge.b()).pos();
        boolean gas = fluid.getFluid().getFluidType().isLighterThanAir();
        boolean equalizing = graph.node(edge.a()).isHandler() && graph.node(edge.b()).isHandler();
        ArrayList<BlockPos> stranded = new ArrayList<BlockPos>();
        for (int i = 0; i < pipes.size(); ++i) {
            BlockPos cell = pipes.get(i);
            FluidTransportBehaviour pipe = FluidPropagator.getPipe((BlockGetter)level, (BlockPos)cell);
            if (pipe == null) continue;
            boolean submerged = gas;
            if (!gas) {
                double cellBottom;
                double frac = ((double)i + 1.0) / (double)(edge.length() + 1);
                double headHere = headA + (headB - headA) * frac;
                boolean bl = submerged = headHere + 0.05 >= (cellBottom = SableCompat.getWorldY(level, cell) - 0.5);
            }
            if (!submerged) {
                if (!equalizing || !CreatePipeRendering.hasFluid(pipe)) continue;
                stranded.add(cell);
                continue;
            }
            BlockPos aSide = i == 0 ? aEnd : pipes.get(i - 1);
            BlockPos bSide = i == pipes.size() - 1 ? bEnd : pipes.get(i + 1);
            Direction towardA = CreatePipeRendering.direction(cell, aSide);
            Direction towardB = CreatePipeRendering.direction(cell, bSide);
            if (towardA == null || towardB == null) continue;
            boolean aInbound = headA >= headB;
            boolean changed = CreatePipeRendering.seedComplete(pipe.getConnection(towardA), aInbound, fluid);
            if (changed |= CreatePipeRendering.seedComplete(pipe.getConnection(towardB), !aInbound, fluid)) {
                pipe.blockEntity.notifyUpdate();
            }
            filled.add(cell);
        }
        CreatePipeRendering.drainColumn(level, stranded, filled, gameTime, edgeIndex);
        return !stranded.isEmpty();
    }

    private static boolean drainDeadEdge(Level level, Edge edge, Set<BlockPos> filled, long gameTime) {
        ArrayList<BlockPos> wet = new ArrayList<BlockPos>();
        for (BlockPos cell : edge.pipes()) {
            FluidTransportBehaviour pipe = FluidPropagator.getPipe((BlockGetter)level, (BlockPos)cell);
            if (pipe == null || !CreatePipeRendering.hasFluid(pipe)) continue;
            wet.add(cell);
        }
        CreatePipeRendering.drainColumn(level, wet, filled, gameTime, edge.index());
        return !wet.isEmpty();
    }

    private static void drainColumn(Level level, List<BlockPos> stranded, Set<BlockPos> filled, long gameTime, int edgeIndex) {
        if (stranded.isEmpty()) {
            return;
        }
        BlockPos top = null;
        if ((gameTime + (long)edgeIndex) % 4L == 0L) {
            for (BlockPos cell : stranded) {
                if (top != null && !(SableCompat.getWorldY(level, cell) > SableCompat.getWorldY(level, top))) continue;
                top = cell;
            }
        }
        for (BlockPos cell : stranded) {
            if (cell.equals(top)) continue;
            filled.add(cell);
        }
    }

    private static boolean hasFluid(FluidTransportBehaviour pipe) {
        for (Direction dir : Direction.values()) {
            PipeConnectionAccessor accessor;
            PipeConnection pipeConnection = pipe.getConnection(dir);
            if (!(pipeConnection instanceof PipeConnectionAccessor) || !(accessor = (PipeConnectionAccessor)pipeConnection).pipesnphysics$getFlow().isPresent()) continue;
            return true;
        }
        return false;
    }

    private static boolean seedCharging(PipeConnection conn, boolean inbound, FluidStack fluid, float pressure) {
        if (!(conn instanceof PipeConnectionAccessor)) {
            return false;
        }
        PipeConnectionAccessor accessor = (PipeConnectionAccessor)conn;
        Optional<PipeConnection.Flow> current = accessor.pipesnphysics$getFlow();
        if (current.isPresent()) {
            PipeConnection.Flow flow = current.get();
            boolean sameFluid = FluidStack.isSameFluidSameComponents((FluidStack)flow.fluid, (FluidStack)fluid);
            if (flow.inbound == inbound && sameFluid) {
                return false;
            }
            if (sameFluid && flow.complete) {
                flow.inbound = inbound;
                conn.wipePressure();
                conn.addPressure(inbound, pressure);
                return true;
            }
            flow.inbound = inbound;
            flow.fluid = fluid.copy();
            flow.progress.startWithValue(0.0);
            flow.complete = false;
        } else {
            PipeConnection pipeConnection = conn;
            Objects.requireNonNull(pipeConnection);
            accessor.pipesnphysics$setFlow(Optional.of(new PipeConnection.Flow(pipeConnection, inbound, fluid.copy())));
        }
        conn.wipePressure();
        conn.addPressure(inbound, pressure);
        return true;
    }

    private static boolean seedComplete(PipeConnection conn, boolean inbound, FluidStack fluid) {
        if (!(conn instanceof PipeConnectionAccessor)) {
            return false;
        }
        PipeConnectionAccessor accessor = (PipeConnectionAccessor)conn;
        Optional<PipeConnection.Flow> current = accessor.pipesnphysics$getFlow();
        if (current.isPresent()) {
            PipeConnection.Flow flow = current.get();
            if (flow.inbound == inbound && flow.complete && FluidStack.isSameFluidSameComponents((FluidStack)flow.fluid, (FluidStack)fluid)) {
                return false;
            }
            flow.inbound = inbound;
            flow.fluid = fluid.copy();
            flow.progress.startWithValue(1.0);
            flow.complete = true;
            return true;
        }
        PipeConnection pipeConnection = conn;
        Objects.requireNonNull(pipeConnection);
        PipeConnection.Flow flow = new PipeConnection.Flow(pipeConnection, inbound, fluid.copy());
        flow.progress.startWithValue(1.0);
        flow.complete = true;
        accessor.pipesnphysics$setFlow(Optional.of(flow));
        return true;
    }

    private static boolean isComplete(PipeConnection conn) {
        if (!(conn instanceof PipeConnectionAccessor)) {
            return false;
        }
        PipeConnectionAccessor accessor = (PipeConnectionAccessor)conn;
        Optional<PipeConnection.Flow> flow = accessor.pipesnphysics$getFlow();
        return flow.isPresent() && flow.get().complete;
    }

    private static void clearCell(Level level, BlockPos cell) {
        FluidTransportBehaviour pipe = FluidPropagator.getPipe((BlockGetter)level, (BlockPos)cell);
        if (pipe == null) {
            return;
        }
        boolean changed = false;
        for (Direction dir : Direction.values()) {
            changed |= CreatePipeRendering.clearFlow(pipe.getConnection(dir));
        }
        if (changed) {
            pipe.blockEntity.notifyUpdate();
        }
    }

    private static boolean clearFlow(PipeConnection conn) {
        PipeConnectionAccessor accessor;
        if (conn instanceof PipeConnectionAccessor && (accessor = (PipeConnectionAccessor)conn).pipesnphysics$getFlow().isPresent()) {
            accessor.pipesnphysics$setFlow(Optional.empty());
            conn.wipePressure();
            return true;
        }
        return false;
    }

    private static float flowPressure(int mbPerTick) {
        return Math.clamp((float)((float)Math.abs(mbPerTick) * 0.6f), (float)1.0f, (float)128.0f);
    }

    private static Direction direction(BlockPos from, BlockPos to) {
        return Direction.fromDelta((int)(to.getX() - from.getX()), (int)(to.getY() - from.getY()), (int)(to.getZ() - from.getZ()));
    }
}
