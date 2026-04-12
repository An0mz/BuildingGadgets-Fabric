package com.direwolf20.buildinggadgets.common.compat;/*package com.direwolf20.buildinggadgets.common.compat;

import draylar.goml.api.ClaimUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class GOMLCompat {

    public static boolean MOD_LOADED;

    public static boolean canUse(ServerPlayer player, BlockPos pos) {
        if (MOD_LOADED) {
            ClaimUtils.getClaimsAt(player.level(), pos).forEach(entry -> {
                if (!ClaimUtils.playerHasPermission(entry, player)) {
                    throw new RuntimeException("Player cannot use this block");
                }
            });
        }
        return true;
    }
}
*/