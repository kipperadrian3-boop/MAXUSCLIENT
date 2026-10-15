package com.maxusclient.client.module.esp;

import com.maxusclient.client.module.Category;
import com.maxusclient.client.module.Module;
import com.maxusclient.client.setting.BlockListSetting;
import com.maxusclient.client.setting.BooleanSetting;
import com.maxusclient.client.setting.ColorSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BlockEsp extends Module {

    public final BooleanSetting tracers;
    public final BooleanSetting useColorForAll;
    public final ColorSetting globalColor;
    public final BlockListSetting activeBlocks;

    // Tracked block positions found in world
    private final Set<BlockPos> trackedPositions = ConcurrentHashMap.newKeySet();
    private final Set<Block> cachedTrackedBlocks = Collections.synchronizedSet(new HashSet<>());

    public BlockEsp() {
        super("Block ESP", Category.ESP, "Highlights selected blocks and draws tracers to them.");

        this.tracers = addSetting(new BooleanSetting("Tracers", "Draws a line from screen center to the block", false));
        this.useColorForAll = addSetting(new BooleanSetting("Use Color for all", "Applies a single global color to all highlighted blocks", false));
        this.globalColor = addSetting(new ColorSetting("Global Color", "Global color applied when Use Color for all is enabled", 0xFFFF69B4));
        this.activeBlocks = addSetting(new BlockListSetting("Active Blocks", "List of target blocks to highlight"));
    }

    @Override
    public void onDisable() {
        trackedPositions.clear();
    }

    public void updateBlockCache() {
        cachedTrackedBlocks.clear();
        for (String idStr : activeBlocks.get().keySet()) {
            Identifier id = Identifier.tryParse(idStr);
            if (id != null) {
                Block block = BuiltInRegistries.BLOCK.getValue(id);
                if (block != null && !block.defaultBlockState().isAir()) {
                    cachedTrackedBlocks.add(block);
                }
            }
        }
    }

    public boolean isBlockTracked(BlockState state) {
        if (!isEnabled() || state == null || state.isAir()) return false;
        return cachedTrackedBlocks.contains(state.getBlock());
    }

    public int getBlockColor(BlockState state) {
        if (useColorForAll.get()) {
            return globalColor.get();
        }
        if (state != null) {
            Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (id != null) {
                return activeBlocks.getColor(id.toString());
            }
        }
        return 0xFFFF69B4;
    }

    public Set<BlockPos> getTrackedPositions() {
        return trackedPositions;
    }

    public void addPosition(BlockPos pos) {
        trackedPositions.add(pos.immutable());
    }

    public void removePosition(BlockPos pos) {
        trackedPositions.remove(pos);
    }

    @Override
    public void onRender(LevelRenderContext context) {
        if (!isEnabled() || trackedPositions.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        CameraRenderState camera = context.levelState() != null ? context.levelState().cameraRenderState : null;
        if (camera == null || camera.pos == null) return;

        Vec3 camPos = camera.pos;
        boolean isFirstPerson = mc.options.getCameraType().isFirstPerson();

        Vec3 lookDir = null;
        if (isFirstPerson) {
            float yawRad = (float) Math.toRadians(camera.yRot);
            float pitchRad = (float) Math.toRadians(camera.xRot);
            double lx = -Math.sin(yawRad) * Math.cos(pitchRad);
            double ly = -Math.sin(pitchRad);
            double lz = Math.cos(yawRad) * Math.cos(pitchRad);
            lookDir = new Vec3(lx, ly, lz).normalize();
        }

        DrawableGizmoPrimitives gizmos = new DrawableGizmoPrimitives();

        for (BlockPos pos : trackedPositions) {
            if (!mc.level.hasChunkAt(pos)) continue;

            BlockState state = mc.level.getBlockState(pos);
            if (!isBlockTracked(state)) continue;

            // Skip block if literally right at camera to avoid clipping
            if (pos.distToCenterSqr(camPos) < 0.6) continue;

            int color = getBlockColor(state);

            // Dynamically calculate bounding box from BlockState with zero hardcoding!
            VoxelShape shape = state.getShape(mc.level, pos);
            AABB aabb = shape.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : shape.bounds();

            double m = 0.003;
            double x0 = pos.getX() + aabb.minX - m;
            double y0 = pos.getY() + aabb.minY - m;
            double z0 = pos.getZ() + aabb.minZ - m;
            double x1 = pos.getX() + aabb.maxX + m;
            double y1 = pos.getY() + aabb.maxY + m;
            double z1 = pos.getZ() + aabb.maxZ + m;

            Vec3 blockCenter = new Vec3(
                pos.getX() + aabb.getCenter().x,
                pos.getY() + aabb.getCenter().y,
                pos.getZ() + aabb.getCenter().z
            );

            // 1. Tracer line (screen center to block center)
            if (tracers.get()) {
                if (isFirstPerson && lookDir != null) {
                    Vec3 toTarget = blockCenter.subtract(camPos);
                    double dist = toTarget.length();
                    // Draw if in front of camera
                    if (dist > 0.4 && lookDir.dot(toTarget.normalize()) > 0.0) {
                        double startOffset = Math.min(0.35, dist * 0.2);
                        Vec3 lineStart = camPos.add(lookDir.scale(startOffset));
                        gizmos.addLine(lineStart, blockCenter, color, 2.5f);
                    }
                } else {
                    Vec3 eyePos = mc.player.getEyePosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(true));
                    double dist = blockCenter.distanceTo(eyePos);
                    if (dist > 0.3) {
                        gizmos.addLine(eyePos, blockCenter, color, 2.5f);
                    }
                }
            }

            // 2. Wireframe Box Outline (12 clean lines only)
            float lineWidth = 2.0f;

            // Bottom 4 edges
            gizmos.addLine(new Vec3(x0, y0, z0), new Vec3(x1, y0, z0), color, lineWidth);
            gizmos.addLine(new Vec3(x1, y0, z0), new Vec3(x1, y0, z1), color, lineWidth);
            gizmos.addLine(new Vec3(x1, y0, z1), new Vec3(x0, y0, z1), color, lineWidth);
            gizmos.addLine(new Vec3(x0, y0, z1), new Vec3(x0, y0, z0), color, lineWidth);

            // Top 4 edges
            gizmos.addLine(new Vec3(x0, y1, z0), new Vec3(x1, y1, z0), color, lineWidth);
            gizmos.addLine(new Vec3(x1, y1, z0), new Vec3(x1, y1, z1), color, lineWidth);
            gizmos.addLine(new Vec3(x1, y1, z1), new Vec3(x0, y1, z1), color, lineWidth);
            gizmos.addLine(new Vec3(x0, y1, z1), new Vec3(x0, y1, z0), color, lineWidth);

            // 4 Vertical edges
            gizmos.addLine(new Vec3(x0, y0, z0), new Vec3(x0, y1, z0), color, lineWidth);
            gizmos.addLine(new Vec3(x1, y0, z0), new Vec3(x1, y1, z0), color, lineWidth);
            gizmos.addLine(new Vec3(x1, y0, z1), new Vec3(x1, y1, z1), color, lineWidth);
            gizmos.addLine(new Vec3(x0, y0, z1), new Vec3(x0, y1, z1), color, lineWidth);
        }

        // Submit to alwaysOnTop pass (visible through walls)
        gizmos.submit(context.submitNodeCollector(), camera, true);
    }
}
