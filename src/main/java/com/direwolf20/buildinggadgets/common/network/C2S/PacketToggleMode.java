package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.items.GadgetExchanger;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record PacketToggleMode(int mode) implements CustomPacketPayload {

    public static final Type<PacketToggleMode> TYPE =
            new Type<>(PacketHandler.PacketToggleMode);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketToggleMode> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeInt(packet.mode),
            buf -> new PacketToggleMode(buf.readInt())
    );

    public static void send(int mode) {
        ClientPlayNetworking.send(new PacketToggleMode(mode));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketToggleMode payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack heldItem = AbstractGadget.getGadget(context.player());
            if (heldItem.isEmpty())
                return;

            if (heldItem.getItem() instanceof GadgetBuilding gadgetBuilding) {
                gadgetBuilding.setMode(heldItem, payload.mode);
            } else if (heldItem.getItem() instanceof GadgetExchanger gadgetExchanger) {
                gadgetExchanger.setMode(heldItem, payload.mode);
            } else if (heldItem.getItem() instanceof GadgetCopyPaste gadgetCopyPaste) {
                gadgetCopyPaste.setMode(heldItem, payload.mode);
            }
        });
    }
}
