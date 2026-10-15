package com.maxusclient.client.model;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class BlockInfo {
    public final Block block;
    public final Identifier id;
    public final String name;
    public final ItemStack stack;

    public BlockInfo(Block block, Identifier id, String name, ItemStack stack) {
        this.block = block;
        this.id = id;
        this.name = name;
        this.stack = stack;
    }
}
