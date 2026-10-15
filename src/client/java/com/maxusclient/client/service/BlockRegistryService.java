package com.maxusclient.client.service;

import com.maxusclient.client.model.BlockInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BlockRegistryService {
    private static List<BlockInfo> cachedBlocks = null;

    public static void ensureCache() {
        if (cachedBlocks != null) return;
        List<BlockInfo> list = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block.defaultBlockState().isAir()) continue;
            Identifier id = BuiltInRegistries.BLOCK.getKey(block);
            String name = block.getName().getString();
            Item item = block.asItem();
            ItemStack stack = (item != null && item != Items.AIR) ? new ItemStack(item) : ItemStack.EMPTY;
            list.add(new BlockInfo(block, id, name, stack));
        }
        list.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        cachedBlocks = list;
    }

    public static List<BlockInfo> getAllBlocks() {
        ensureCache();
        return cachedBlocks;
    }

    public static List<BlockInfo> searchAllBlocks(String query) {
        ensureCache();
        if (query == null || query.trim().isEmpty()) return cachedBlocks;
        String q = query.toLowerCase().trim();
        List<BlockInfo> result = new ArrayList<>();
        for (BlockInfo info : cachedBlocks) {
            if (info.name.toLowerCase().contains(q) || info.id.getPath().toLowerCase().contains(q)) {
                result.add(info);
            }
        }
        return result;
    }

    public static BlockInfo findBlockInfo(String idStr) {
        ensureCache();
        for (BlockInfo info : cachedBlocks) {
            if (info.id.toString().equals(idStr) || info.id.getPath().equals(idStr)) {
                return info;
            }
        }
        return null;
    }
}
