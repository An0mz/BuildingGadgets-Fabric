package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.items.GadgetDestruction;
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

public record PacketChangeRange(int range) implements CustomPacketPayload {

    public static final Type<PacketChangeRange> TYPE =
            new Type<>(PacketHandler.PacketChangeRange);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketChangeRange> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeInt(packet.range),
            buf -> new PacketChangeRange(buf.readInt())
    );

    public static void send() {
        send(-1);
    }

    public static void send(int range) {
        ClientPlayNetworking.send(new PacketChangeRange(range));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketChangeRange payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack stack = AbstractGadget.getGadget(context.player());

            if (payload.range >= 0) {
                GadgetUtils.setToolRange(stack, payload.range);
            } else if (stack.getItem() instanceof GadgetBuilding) {
                GadgetBuilding.rangeChange(context.player(), stack);
            } else if (stack.getItem() instanceof GadgetExchanger) {
                GadgetExchanger.rangeChange(context.player(), stack);
            } else if (stack.getItem() instanceof GadgetDestruction) {
                GadgetDestruction.switchOverlay(context.player(), stack);
            }
        });
    }
}