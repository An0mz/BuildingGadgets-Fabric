package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.items.OurItems;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.network.Target;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateKey;
import com.direwolf20.buildinggadgets.common.tileentities.TemplateManagerTileEntity;
import com.direwolf20.buildinggadgets.common.util.TemplateKeyHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public record PacketTemplateManagerTemplateCreated(UUID id, BlockPos pos) implements CustomPacketPayload {

    public static final Type<PacketTemplateManagerTemplateCreated> TYPE =
            new Type<>(PacketHandler.PacketTemplateManagerTemplateCreated);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketTemplateManagerTemplateCreated> CODEC = StreamCodec.of(
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
            ServerLevel level = (ServerLevel) context.player().level();

            if (level.isLoaded(payload.pos)) {
                BlockEntity blockEntity = level.getBlockEntity(payload.pos);
                if (blockEntity instanceof TemplateManagerTileEntity manager) {
                    ItemStack stack = new ItemStack(OurItems.TEMPLATE_ITEM);
                    // Set the template key UUID directly on the stack
                    TemplateKeyHelper.setTemplateKey(stack, payload.id);

                    manager.setItem(1, stack);

                    // Request update with the key
                    ITemplateKey key = TemplateKeyHelper.getTemplateKey(stack);
                    if (key != null) {
                        BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(level).ifPresent(provider ->
                                provider.requestUpdate(key, new Target(PacketFlow.CLIENTBOUND, context.player()))
                        );
                    } else {
                        BuildingGadgets.LOG.error("Failed to apply Template id on server!");
                    }
                }
            }
        });
    }
}
