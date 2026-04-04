package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetDestruction;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record PacketBindTool() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketBindTool> TYPE =
            new CustomPacketPayload.Type<>(PacketHandler.PacketBindTool);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketBindTool> CODEC = StreamCodec.of(
            (buf, packet) -> {}, // Write nothing
            buf -> new PacketBindTool() // Read nothing, return new instance
    );

    public static void send() {
        ClientPlayNetworking.send(new PacketBindTool());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketBindTool payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack stack = AbstractGadget.getGadget(context.player());
            if (!(stack.getItem() instanceof GadgetDestruction)) {
                GadgetUtils.linkToInventory(stack, context.player());
            }
        });
    }
}