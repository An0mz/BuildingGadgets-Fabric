package com.direwolf20.buildinggadgets.common.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class FTBChunksCompat {

    public static boolean MOD_LOADED = false;

    public static boolean canUse(ServerPlayer player, BlockPos pos) {
        // FTBChunks compat is disabled - mod not in classpath
        return true;
    }
}
