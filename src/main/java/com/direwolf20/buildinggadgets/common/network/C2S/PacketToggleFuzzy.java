package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.items.GadgetBuilding;
import com.direwolf20.buildinggadgets.common.items.GadgetDestruction;
import com.direwolf20.buildinggadgets.common.items.GadgetExchanger;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

public record PacketToggleFuzzy() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketToggleFuzzy> TYPE =
            new CustomPacketPayload.Type<>(PacketHandler.PacketToggleFuzzy);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketToggleFuzzy> CODEC = StreamCodec.of(
            (buf, packet) -> {}, // Write nothing
            buf -> new PacketToggleFuzzy() // Read nothing, return new instance
    );

    public static void send() {
        ClientPlayNetworking.send(new PacketToggleFuzzy());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketToggleFuzzy payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack stack = AbstractGadget.getGadget(context.player());
            if (stack.getItem() instanceof GadgetExchanger || stack.getItem() instanceof GadgetBuilding || (stack.getItem() instanceof GadgetDestruction && BuildingGadgets.getConfig().gadgets.gadgetDestruction.nonFuzzyEnabled)) {
                AbstractGadget.toggleFuzzy(context.player(), stack);
            }
        });
    }
}