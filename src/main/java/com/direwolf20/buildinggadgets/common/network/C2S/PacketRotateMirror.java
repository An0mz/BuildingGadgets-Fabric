package com.direwolf20.buildinggadgets.common.network.C2S;

import com.direwolf20.buildinggadgets.common.items.AbstractGadget;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public record PacketRotateMirror(@Nullable Operation operation) implements CustomPacketPayload {

    public static final Type<PacketRotateMirror> TYPE =
            new Type<>(PacketHandler.PacketRotateMirror);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketRotateMirror> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBoolean(packet.operation != null);
                if (packet.operation != null) {
                    buf.writeEnum(packet.operation);
                }
            },
            buf -> {
                boolean hasOperation = buf.readBoolean();
                Operation operation = hasOperation ? buf.readEnum(Operation.class) : null;
                return new PacketRotateMirror(operation);
            }
    );

    public enum Operation {
        ROTATE, MIRROR
    }

    public static void send(@Nullable Operation operation) {
        ClientPlayNetworking.send(new PacketRotateMirror(operation));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PacketRotateMirror payload, ServerPlayNetworking.Context context) {
        Operation operation = payload.operation != null ? payload.operation :
                (context.player().isShiftKeyDown() ? Operation.MIRROR : Operation.ROTATE);

        context.server().execute(() -> {
            ItemStack stack = AbstractGadget.getGadget(context.player());

            if (stack.getItem() instanceof AbstractGadget item) {
                if (operation == Operation.MIRROR) {
                    item.onMirror(stack, context.player());
                } else {
                    item.onRotate(stack, context.player());
                }
            }
        });
    }
}