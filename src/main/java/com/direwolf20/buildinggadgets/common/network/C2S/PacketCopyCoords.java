package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.GadgetCopyPaste;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.tainted.building.Region;
import com.direwolf20.buildinggadgets.common.util.lang.MessageTranslation;
import com.direwolf20.buildinggadgets.common.util.lang.Styles;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record PacketCopyCoords(BlockPos startPos, BlockPos endPos) implements CustomPacketPayload {

    public static final Type<PacketCopyCoords> TYPE =
            new Type<>(PacketHandler.PacketCopyCoords);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketCopyCoords> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBlockPos(packet.startPos);
                buf.writeBlockPos(packet.endPos);
            },
            buf -> new PacketCopyCoords(buf.readBlockPos(), buf.readBlockPos())
    );

    public static void send(BlockPos start, BlockPos end) {
        ClientPlayNetworking.send(new PacketCopyCoords(start, end));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketCopyCoords payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack heldItem = GadgetCopyPaste.getGadget(context.player());
            if (heldItem.isEmpty()) return;

            if (payload.startPos.equals(BlockPos.ZERO) && payload.endPos.equals(BlockPos.ZERO)) {
                GadgetCopyPaste.setSelectedRegion(heldItem, null);
                context.player().sendSystemMessage(MessageTranslation.AREA_RESET.componentTranslation().setStyle(Styles.AQUA));
            } else {
                GadgetCopyPaste.setSelectedRegion(heldItem, new Region(payload.startPos, payload.endPos));
            }

            Optional<Region> regionOpt = GadgetCopyPaste.getSelectedRegion(heldItem);
            if (regionOpt.isEmpty()) {
                context.player().sendSystemMessage(MessageTranslation.FIRST_COPY.componentTranslation().setStyle(Styles.DK_GREEN));
            }
            regionOpt.ifPresent(region -> ((GadgetCopyPaste) heldItem.getItem()).tryCopy(heldItem, context.player().level(), context.player(), region));
        });
    }
}