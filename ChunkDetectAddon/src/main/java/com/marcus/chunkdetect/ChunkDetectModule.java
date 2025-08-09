package com.marcus.chunkdetect;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.ChunkDataEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.settings.IntSetting;
import meteordevelopment.meteorclient.systems.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.settings.ColorSetting;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.HashSet;
import java.util.Set;

public class ChunkDetectModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final IntSetting threshold = sgGeneral.add(new IntSetting.Builder()
        .name("threshold")
        .description("Minimum suspicious block changes before flagging a chunk.")
        .defaultValue(5)
        .min(1)
        .sliderMax(50)
        .build()
    );

    private final ColorSetting espColor = sgGeneral.add(new ColorSetting.Builder()
        .name("ESP Color")
        .description("Color of ESP box around flagged chunks.")
        .defaultValue(0, 255, 0, 100)
        .build()
    );

    private final Set<ChunkPos> flaggedChunks = new HashSet<>();

    private static final long SEED = 6608149111735331168L;

    public ChunkDetectModule() {
        super(Categories.Render, "chunk-detect", "Highlights chunks modified compared to vanilla seed.");
    }

    @EventHandler
    private void onChunkLoad(ChunkDataEvent event) {
        ChunkPos pos = new ChunkPos(event.x, event.z);

        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;

        if (world == null) return;

        Chunk liveChunk = world.getChunkManager().getChunk(event.x, event.z, false);
        if (liveChunk == null) return;

        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();

        ChunkRandom random = new ChunkRandom();
        random.setPopulationSeed(SEED, event.x, event.z);

        Chunk vanillaChunk = new Chunk(world, event.x, event.z);

        generator.generateFeatures(vanillaChunk, world.getStructureAccessor());

        int suspiciousCount = 0;

        for (int y = 0; y < world.getHeight(); y++) {
            for (int cx = 0; cx < 16; cx++) {
                for (int cz = 0; cz < 16; cz++) {
                    BlockPos posInChunk = new BlockPos(event.x * 16 + cx, y, event.z * 16 + cz);

                    Block liveBlock = liveChunk.getBlockState(posInChunk).getBlock();
                    Block vanillaBlock = vanillaChunk.getBlockState(posInChunk).getBlock();

                    if (!blocksAreEqualIgnoringNatural(liveBlock, vanillaBlock)) {
                        suspiciousCount++;
                    }
                }
            }
        }

        if (suspiciousCount >= threshold.get()) {
            flaggedChunks.add(pos);
        }
    }

    private boolean blocksAreEqualIgnoringNatural(Block live, Block vanilla) {
        if (isNaturalBlock(live) && isNaturalBlock(vanilla)) return true;
        return live == vanilla;
    }

    private boolean isNaturalBlock(Block block) {
        return block == Blocks.OAK_LOG || block == Blocks.BIRCH_LOG || block == Blocks.SPRUCE_LOG ||
               block == Blocks.JUNGLE_LOG || block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG ||
               block == Blocks.OAK_LEAVES || block == Blocks.BIRCH_LEAVES || block == Blocks.SPRUCE_LEAVES ||
               block == Blocks.JUNGLE_LEAVES || block == Blocks.ACACIA_LEAVES || block == Blocks.DARK_OAK_LEAVES ||
               block == Blocks.GRASS || block == Blocks.TALL_GRASS || block == Blocks.FERN ||
               block == Blocks.DANDELION || block == Blocks.POPPY || block == Blocks.SUNFLOWER ||
               block == Blocks.LILAC || block == Blocks.ROSE_BUSH || block == Blocks.SAPLING ||
               block == Blocks.VINE || block == Blocks.MUSHROOM_STEM || block == Blocks.BROWN_MUSHROOM ||
               block == Blocks.RED_MUSHROOM || block == Blocks.DEAD_BUSH;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = mc.world;
        if (world == null) return;

        for (ChunkPos pos : flaggedChunks) {
            Box box = new Box(
                pos.getStartX(), world.getBottomY(), pos.getStartZ(),
                pos.getEndX() + 1, world.getTopY(), pos.getEndZ() + 1
            );
            event.renderer.box(box, espColor.get(), espColor.get(), ShapeMode.Lines, 1);
        }
    }
}