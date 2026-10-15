package com.maxusclient.client.model;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class BlockInfo {
    public final Block block;
    public final Identifier id;
    public final String name;
    private ItemStack cachedStack = null;

    public BlockInfo(Block block, Identifier id, String name) {
        this.block = block;
        this.id = id;
        this.name = name;
    }

    public ItemStack getStack() {
        if (cachedStack == null) {
            try {
                Item item = block.asItem();
                if (item != null && item != Items.AIR) {
                    cachedStack = new ItemStack(item);
                } else {
                    cachedStack = ItemStack.EMPTY;
                }
            } catch (Throwable t) {
                cachedStack = ItemStack.EMPTY;
            }
        }
        return cachedStack;
    }
}

