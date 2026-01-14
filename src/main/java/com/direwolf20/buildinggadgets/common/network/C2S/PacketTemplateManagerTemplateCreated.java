package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.items.OurItems;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.network.Target;
import com.direwolf20.buildinggadgets.common.tileentities.TemplateManagerTileEntity;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public record PacketTemplateManagerTemplateCreated(UUID id, BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketTemplateManagerTemplateCreated> TYPE =
            new CustomPacketPayload.Type<>(PacketHandler.PacketTemplateManagerTemplateCreated);

    public static final StreamCodec<FriendlyByteBuf, PacketTemplateManagerTemplateCreated> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.id);
                buf.writeBlockPos(packet.pos);
            },
            buf -> new PacketTemplateManagerTemplateCreated(buf.readUUID(), buf.readBlockPos())
    );

    public static void send(UUID id, BlockPos pos) {
        ClientPlayNetworking.send(new PacketTemplateManagerTemplateCreated(id, pos));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketTemplateManagerTemplateCreated payload, ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            Level level = context.player().level();
            if (level.hasChunkAt(payload.pos)) {
                BlockEntity blockEntity = level.getBlockEntity(payload.pos);
                if (blockEntity instanceof TemplateManagerTileEntity manager) {
                    ItemStack stack = new ItemStack(OurItems.TEMPLATE_ITEM);
                    BGComponent.TEMPLATE_KEY_COMPONENT.maybeGet(stack).ifPresent(key -> {
                        UUID id = key.getOrComputeId(() -> payload.id);

                        if (!id.equals(payload.id)) {
                            BuildingGadgets.LOG.error("Failed to apply Template id on server!");
                        } else {
                            manager.setItem(1, stack);
                            BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(level).ifPresent(provider -> provider.requestUpdate(key, new Target(PacketFlow.CLIENTBOUND, context.player())));
                        }
                    });
                }
            }
        });
    }
}