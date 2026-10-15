package com.maxusclient.client.controller;

import com.maxusclient.client.module.Module;
import com.maxusclient.client.module.ModuleManager;
import com.maxusclient.client.module.esp.BlockEsp;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Iterator;
import java.util.Set;

public class ModuleController {
    private static int tickCounter = 0;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;

            // Tick all active modules
            for (Module m : ModuleManager.getActiveModules()) {
                m.onTick();
            }

            tickCounter++;
            if (tickCounter % 20 != 0) return; // 1 second interval

            BlockEsp blockEsp = ModuleManager.getModule(BlockEsp.class);
            if (blockEsp == null || !blockEsp.isEnabled()) return;

            Set<BlockPos> tracked = blockEsp.getTrackedPositions();

            // 1. Prune removed or unloaded blocks
            Iterator<BlockPos> it = tracked.iterator();
            while (it.hasNext()) {
                BlockPos pos = it.next();
                if (client.level.hasChunkAt(pos)) {
                    BlockState state = client.level.getBlockState(pos);
                    if (!blockEsp.isBlockTracked(state)) {
                        it.remove();
                    }
                }
            }

            // 2. Scan loaded chunks using fast section palettes
            BlockPos playerPos = client.player.blockPosition();
            int centerChunkX = playerPos.getX() >> 4;
            int centerChunkZ = playerPos.getZ() >> 4;
            int chunkRadius = Math.max(12, client.options.renderDistance().get());

            for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
                for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                    if (!client.level.getChunkSource().hasChunk(cx, cz)) continue;
                    LevelChunk chunk = client.level.getChunk(cx, cz);
                    if (chunk == null) continue;

                    // A. Block entities
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (blockEsp.isBlockTracked(be.getBlockState())) {
                            blockEsp.addPosition(be.getBlockPos());
                        }
                    }

                    // B. Chunk section palettes
                    LevelChunkSection[] sections = chunk.getSections();
                    for (int sIdx = 0; sIdx < sections.length; sIdx++) {
                        LevelChunkSection section = sections[sIdx];
                        if (section == null || section.hasOnlyAir()) continue;

                        if (section.maybeHas(blockEsp::isBlockTracked)) {
                            int secMinY = client.level.getMinY() + (sIdx << 4);
                            for (int x = 0; x < 16; x++) {
                                for (int y = 0; y < 16; y++) {
                                    for (int z = 0; z < 16; z++) {
                                        BlockState s = section.getBlockState(x, y, z);
                                        if (blockEsp.isBlockTracked(s)) {
                                            BlockPos bPos = new BlockPos((cx << 4) + x, secMinY + y, (cz << 4) + z);
                                            blockEsp.addPosition(bPos);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
