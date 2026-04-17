package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record PacketPasteGUI(BlockPos pos) implements CustomPacketPayload {

    public static final Type<PacketPasteGUI> TYPE =
            new Type<>(PacketHandler.PacketPasteGUI);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketPasteGUI> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeBlockPos(packet.pos),
            buf -> new PacketPasteGUI(buf.readBlockPos())
    );

    public static void send(int x, int y, int z) {
        ClientPlayNetworking.send(new PacketPasteGUI(new BlockPos(x, y, z)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketPasteGUI payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack heldItem = GadgetCopyPaste.getGadget(context.player());
            if (!heldItem.isEmpty()) {
                GadgetCopyPaste.setRelativeVector(heldItem, payload.pos);
            }
        });
    }
}
