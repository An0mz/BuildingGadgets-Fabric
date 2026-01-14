package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.GadgetDestruction;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.util.ref.NBTKeys;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record PacketDestructionGUI(int left, int right, int up, int down, int depth) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketDestructionGUI> TYPE =
            new CustomPacketPayload.Type<>(PacketHandler.PacketDestructionGUI);

    public static final StreamCodec<FriendlyByteBuf, PacketDestructionGUI> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.left);
                buf.writeInt(packet.right);
                buf.writeInt(packet.up);
                buf.writeInt(packet.down);
                buf.writeInt(packet.depth);
            },
            buf -> new PacketDestructionGUI(
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            )
    );

    public static void send(int left, int right, int up, int down, int depth) {
        ClientPlayNetworking.send(new PacketDestructionGUI(left, right, up, down, depth));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketDestructionGUI payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack heldItem = GadgetDestruction.getGadget(context.player());
            if (!heldItem.isEmpty()) {
                GadgetDestruction.setToolValue(heldItem, payload.left, NBTKeys.GADGET_VALUE_LEFT);
                GadgetDestruction.setToolValue(heldItem, payload.right, NBTKeys.GADGET_VALUE_RIGHT);
                GadgetDestruction.setToolValue(heldItem, payload.up, NBTKeys.GADGET_VALUE_UP);
                GadgetDestruction.setToolValue(heldItem, payload.down, NBTKeys.GADGET_VALUE_DOWN);
                GadgetDestruction.setToolValue(heldItem, payload.depth, NBTKeys.GADGET_VALUE_DEPTH);
            }
        });
    }
}