package de.devin.pipesnphysics.physics;

import net.neoforged.neoforge.fluids.FluidType;

/**
 * Fluid-property scaling for the hydraulic engine. Keeps Minecraft's
 * {@link FluidType} density and viscosity in one place so conductance and
 * siphon limits stay physically consistent.
 */
public final class FluidPhysics {

    /** Reference density for water in NeoForge fluid types. */
    public static final int WATER_DENSITY = 1000;

    private FluidPhysics() {}

    /**
     * Pipe conductance multiplier from viscosity. Water (1000) is 1×; lava
     * (6000) flows at one-sixth the rate through the same run.
     */
    public static double viscosityConductanceScale(FluidType type) {
        return WATER_DENSITY / (double) Math.max(1, type.getViscosity());
    }

    /**
     * How many blocks of fluid column atmospheric pressure can support before
     * cavitation. Denser fluids break sooner — lava at 3000 kg/m³ siphons roughly
     * one-third as high as water for the same config limit.
     *
     * Lighter-than-air fluids keep the raw limit; their buoyancy inversion is
     * handled separately in {@link de.devin.pipesnphysics.engine.solve.NetworkSolver}.
     */
    public static double suctionLimitBlocks(FluidType type, double waterReferenceLimit) {
        if (type.isLighterThanAir()) return waterReferenceLimit;
        return suctionLimitForDensity(type.getDensity(), waterReferenceLimit);
    }

    /** Package-visible for unit tests without bootstrapping Minecraft fluids. */
    static double suctionLimitForDensity(int densityKgPerCubicMeter, double waterReferenceLimit) {
        return waterReferenceLimit * (double) WATER_DENSITY / Math.max(1, densityKgPerCubicMeter);
    }
}
