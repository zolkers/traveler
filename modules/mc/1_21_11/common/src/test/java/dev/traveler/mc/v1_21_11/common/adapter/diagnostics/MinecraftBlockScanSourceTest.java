package dev.traveler.mc.v1_21_11.common.adapter.diagnostics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.navigation.diagnostics.BlockScanSample;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.mc.v1_21_11.common.adapter.testing.MinecraftTestBootstrap;
import dev.traveler.mc.v1_21_11.common.adapter.testing.SingleStateBlockGetter;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MinecraftBlockScanSourceTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void sampleIncludesMinecraftBlockStateAndCoreClassification() {
        MinecraftBlockScanSource source = new MinecraftBlockScanSource(
                new SingleStateBlockGetter(Blocks.STONE.defaultBlockState()));

        BlockScanSample sample = source.sample(new BlockPosition(1, 64, 2));

        assertEquals(new BlockPosition(1, 64, 2), sample.position());
        assertTrue(sample.block().contains("stone"));
        assertEquals("SOLID", sample.passability());
        assertEquals("AVOID", sample.fluid());
        assertEquals("FULL_BLOCK", sample.behavior());
    }
}
