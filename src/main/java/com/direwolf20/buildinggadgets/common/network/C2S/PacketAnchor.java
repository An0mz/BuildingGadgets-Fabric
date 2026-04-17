package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.items.GadgetExchanger;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.util.GadgetUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record PacketAnchor() implements CustomPacketPayload {

    public static final Type<PacketAnchor> TYPE =
            new Type<>(PacketHandler.PacketAnchor);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketAnchor> CODEC = StreamCodec.of(
            (buf, packet) -> {},
            buf -> new PacketAnchor()
    );

    public static void send() {
        ClientPlayNetworking.send(new PacketAnchor());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketAnchor payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack stack = AbstractGadget.getGadget(context.player());
            if (stack.getItem() instanceof GadgetBuilding) {
                GadgetUtils.anchorBlocks(context.player(), stack);
            } else if (stack.getItem() instanceof GadgetExchanger) {
                GadgetUtils.anchorBlocks(context.player(), stack);
            } else {
                ((AbstractGadget) stack.getItem()).onAnchor(stack, context.player());
            }
        });
    }
}
