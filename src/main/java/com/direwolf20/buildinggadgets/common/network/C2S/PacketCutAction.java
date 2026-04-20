package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.GadgetCutPaste;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/**
 * Sent from client when the player clicks the "Cut" button in the radial menu.
 * Triggers the actual cut-and-store operation server-side.
 */
public record PacketCutAction() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketCutAction> TYPE =
            new CustomPacketPayload.Type<>(PacketHandler.PacketCutAction);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCutAction> CODEC = StreamCodec.of(
            (buf, packet) -> { /* no payload */ },
            buf -> new PacketCutAction()
    );

    public static void send() {
        ClientPlayNetworking.send(new PacketCutAction());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketCutAction payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack heldItem = GadgetCutPaste.getGadget(context.player());
            if (heldItem.isEmpty()) return;

            ((GadgetCutPaste) heldItem.getItem()).performCutFromButton(heldItem, context.player());
        });
    }
}

