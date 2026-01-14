package com.direwolf20.buildinggadgets.common.network.bidirection;

import com.direwolf20.buildinggadgets.client.BuildingGadgetsClient;
import com.direwolf20.buildinggadgets.common.component.BGComponent;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.network.Target;
import com.direwolf20.buildinggadgets.common.tainted.template.Template;
import com.direwolf20.buildinggadgets.common.tainted.template.TemplateIO;
import com.direwolf20.buildinggadgets.common.tainted.template.TemplateKey;
import com.direwolf20.buildinggadgets.common.util.exceptions.TemplateReadException;
import com.direwolf20.buildinggadgets.common.util.exceptions.TemplateWriteException;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public record SplitPacketUpdateTemplate(FriendlyByteBuf data) implements CustomPacketPayload {

    public static final int PAYLOAD_LIMIT = Short.MAX_VALUE;

    public static final CustomPacketPayload.Type<SplitPacketUpdateTemplate> TYPE =
            new CustomPacketPayload.Type<>(PacketHandler.SplitPacketUpdateTemplate);

    public static final StreamCodec<FriendlyByteBuf, SplitPacketUpdateTemplate> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeBytes(packet.data),
            buf -> {
                FriendlyByteBuf copy = new FriendlyByteBuf(Unpooled.buffer());
                copy.writeBytes(buf);
                return new SplitPacketUpdateTemplate(copy);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void sendToTarget(Target target, UUID id, Template template) {
        if (target.flow() == PacketFlow.CLIENTBOUND) {
            Server.send(id, template, target.player());
        } else {
            Client.send(id, template);
        }
    }

    private static Template readTemplate(FriendlyByteBuf buf) throws TemplateReadException {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return TemplateIO.readTemplate(new ByteArrayInputStream(bytes), null);
    }

    private static void write(FriendlyByteBuf buf, UUID id, Template template) {
        buf.writeUUID(id);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            TemplateIO.writeTemplate(template, stream);
            buf.writeBytes(stream.toByteArray());
        } catch (TemplateWriteException e) {
            e.printStackTrace();
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Client {

        private static FriendlyByteBuf accumulator;

        @Environment(EnvType.CLIENT)
        public static void send(UUID id, Template template) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            write(buf, id, template);

            while (buf.isReadable(PAYLOAD_LIMIT)) {
                FriendlyByteBuf chunk = new FriendlyByteBuf(Unpooled.buffer());
                buf.readBytes(chunk, PAYLOAD_LIMIT);
                ClientPlayNetworking.send(new SplitPacketUpdateTemplate(chunk));
            }

            if (buf.isReadable()) {
                ClientPlayNetworking.send(new SplitPacketUpdateTemplate(buf));
            }

            // Sentinel value when length == 0
            ClientPlayNetworking.send(new SplitPacketUpdateTemplate(new FriendlyByteBuf(Unpooled.buffer())));
        }

        @Environment(EnvType.CLIENT)
        public static void handle(SplitPacketUpdateTemplate payload, ClientPlayNetworking.Context context) {
            if (accumulator == null) {
                accumulator = new FriendlyByteBuf(Unpooled.buffer());
            }

            if (payload.data.isReadable()) {
                accumulator.writeBytes(payload.data);
                return;
            }

            UUID id = accumulator.readUUID();

            try {
                Template template = readTemplate(accumulator);
                context.client().execute(() -> BuildingGadgetsClient.CACHE_TEMPLATE_PROVIDER.setTemplate(new TemplateKey(id), template));
            } catch (TemplateReadException e) {
                e.printStackTrace();
            }

            accumulator.release();
            accumulator = null;
        }
    }

    public static class Server {

        private static final Map<ServerPlayer, FriendlyByteBuf> buffers = new WeakHashMap<>();

        public static void send(UUID id, Template template, ServerPlayer player) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            write(buf, id, template);

            while (buf.isReadable(PAYLOAD_LIMIT)) {
                FriendlyByteBuf chunk = new FriendlyByteBuf(Unpooled.buffer());
                buf.readBytes(chunk, PAYLOAD_LIMIT);
                ServerPlayNetworking.send(player, new SplitPacketUpdateTemplate(chunk));
            }

            if (buf.isReadable()) {
                ServerPlayNetworking.send(player, new SplitPacketUpdateTemplate(buf));
            }

            // Sentinel value when length == 0
            ServerPlayNetworking.send(player, new SplitPacketUpdateTemplate(new FriendlyByteBuf(Unpooled.buffer())));
        }

        public static void handle(SplitPacketUpdateTemplate payload, ServerPlayNetworking.Context context) {
            FriendlyByteBuf accumulator = buffers.computeIfAbsent(context.player(), $ -> new FriendlyByteBuf(Unpooled.buffer()));

            if (payload.data.isReadable()) {
                accumulator.writeBytes(payload.data);
                return;
            }

            UUID id = accumulator.readUUID();

            try {
                Template template = readTemplate(accumulator);

                context.server().execute(() -> BGComponent.TEMPLATE_PROVIDER_COMPONENT.maybeGet(context.player().level()).ifPresent(provider -> {
                    provider.setTemplate(new TemplateKey(id), template);
                }));
            } catch (TemplateReadException e) {
                e.printStackTrace();
            }

            buffers.remove(context.player()).release();
        }
    }
}