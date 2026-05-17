package com.lxy.hook.tag;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    private ModBlockTags() {
    }

    /** 允许抓取的方块（白名单）。 */
    public static final TagKey<Block> HOOK_GRABBABLE = create("hook_grabbable");

    /** 禁止抓取的方块（黑名单，优先级高于白名单）。 */
    public static final TagKey<Block> HOOK_BLACKLIST = create("hook_blacklist");

    private static TagKey<Block> create(String name) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("hook", name));
    }
}
