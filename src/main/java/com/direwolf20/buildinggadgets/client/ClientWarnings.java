package com.direwolf20.buildinggadgets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ClientWarnings {

    private static boolean warnedThisSession = false;

    private ClientWarnings() {}

    public static void warnChunkRenderIssue() {
        if (warnedThisSession) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MutableComponent message =
                Component.literal("[BuildingGadgets] ")
                        .withStyle(style -> style.withColor(0xFFE066))
                        .append(Component.literal("⚠\n")
                                .withStyle(style -> style.withColor(0xFFE066)))
                        .append(Component.literal(
                                "If chunks look broken, open Video Settings and close it to refresh rendering."
                        ).withStyle(style -> style.withColor(0xAAAAAA)));

        mc.player.sendSystemMessage(message);

        warnedThisSession = true;
    }
}
