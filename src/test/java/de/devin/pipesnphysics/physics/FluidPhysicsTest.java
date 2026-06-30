package de.devin.pipesnphysics.physics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FluidPhysicsTest {

    @Test
    void denserFluidsHaveShorterSiphonReach() {
        double waterLimit = 8.0;
        assertEquals(waterLimit, FluidPhysics.suctionLimitForDensity(1000, waterLimit), 1e-9);
        assertEquals(waterLimit / 3.0, FluidPhysics.suctionLimitForDensity(3000, waterLimit), 1e-9);
    }

    @Test
    void viscosityScalesConductanceInversely() {
        assertEquals(1.0, FluidPhysics.WATER_DENSITY / 1000.0, 1e-9);
        assertEquals(1000.0 / 6000.0, FluidPhysics.WATER_DENSITY / 6000.0, 1e-9);
    }
}
