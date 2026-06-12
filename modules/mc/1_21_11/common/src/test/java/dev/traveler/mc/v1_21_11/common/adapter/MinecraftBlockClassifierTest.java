package dev.traveler.mc.v1_21_11.common.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.world.BlockPassability;
import dev.traveler.core.world.FluidHandling;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinecraftBlockClassifierTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void classifiesAirAsPassableAndSolidBlocksAsSolid() {
        MinecraftBlockClassifier classifier = new MinecraftBlockClassifier();

        assertEquals(BlockPassability.PASSABLE, classifier.classify(Blocks.AIR.defaultBlockState()));
        assertEquals(BlockPassability.SOLID, classifier.classify(Blocks.STONE.defaultBlockState()));
    }

    @Test
    void treatsNonEmptyFluidsAsPassableAndSupportsFluidHandlingChecks() {
        MinecraftBlockClassifier classifier = new MinecraftBlockClassifier();

        assertEquals(BlockPassability.PASSABLE, classifier.classify(Fluids.WATER.defaultFluidState()));
        assertTrue(classifier.hasFluid(Fluids.WATER.defaultFluidState()));
        assertTrue(classifier.allowsFluid(Fluids.WATER.defaultFluidState(), FluidHandling.ALLOW));
        assertTrue(classifier.allowsFluid(Fluids.WATER.defaultFluidState(), FluidHandling.REQUIRE));
        assertFalse(classifier.allowsFluid(Fluids.WATER.defaultFluidState(), FluidHandling.AVOID));
        assertFalse(classifier.allowsFluid(Fluids.EMPTY.defaultFluidState(), FluidHandling.REQUIRE));
    }
}
