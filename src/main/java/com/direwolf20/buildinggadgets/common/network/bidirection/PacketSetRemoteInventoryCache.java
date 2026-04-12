package com.direwolf20.buildinggadgets.common.network.bidirection;

import com.direwolf20.buildinggadgets.client.renders.BaseRenderer;
import com.direwolf20.buildinggadgets.common.network.PacketHandler;
import com.direwolf20.buildinggadgets.common.tainted.inventory.InventoryLinker;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.mojang.datafixers.util.Either;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public record PacketSetRemoteInventoryCache(Data data) implements CustomPacketPayload {

    public static final Type<PacketSetRemoteInventoryCache> TYPE =
            new Type<>(PacketHandler.PacketSetRemoteInventoryCache);

    public static final StreamCodec<RegistryFriendlyByteBuf, PacketSetRemoteInventoryCache> CODEC = StreamCodec.of(
            (buf, packet) -> packet.data.write(buf),
            buf -> new PacketSetRemoteInventoryCache(Data.read(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static class Client {
        @Environment(EnvType.CLIENT)
        public static void handle(PacketSetRemoteInventoryCache payload, ClientPlayNetworking.Context context) {
            context.client().execute(() -> payload.data.either.ifLeft(cache -> {
                if (!payload.data.isCopyPaste()) {
                    BaseRenderer.setInventoryCache(cache.cache());
                }
            }));
        }
    }

    public static class Server {
        public static void handle(PacketSetRemoteInventoryCache payload, ServerPlayNetworking.Context context) {
            context.server().execute(() -> payload.data.either.ifRight(hand -> {
                Multiset<ItemVariant> items = HashMultiset.create();

                InventoryLinker.getLinkedInventory(context.player().level(), context.player().getItemInHand(hand)).ifPresent(inventory -> {
                    try (Transaction transaction = Transaction.openOuter()) {
                        for (StorageView<ItemVariant> view : inventory) {
                            if (!view.isResourceBlank()) {
                                ItemVariant resource = view.getResource();
                                items.add(resource, (int) view.getAmount());
                            }
                        }
                    }
                });

                send(new Data(payload.data.isCopyPaste, Either.left(new Cache(ImmutableMultiset.copyOf(items)))), context.player());
            }));
        }
    }

    private static void send(Data data, ServerPlayer player) {
        ServerPlayNetworking.send(player, new PacketSetRemoteInventoryCache(data));
    }

    @Environment(EnvType.CLIENT)
    public static void send(boolean isCopyPaste, InteractionHand hand) {
        ClientPlayNetworking.send(new PacketSetRemoteInventoryCache(new Data(isCopyPaste, Either.right(hand))));
    }

    public record Data(boolean isCopyPaste, Either<Cache, InteractionHand> either) {
        private static Data read(FriendlyByteBuf buf) {
            boolean isCopyPaste = buf.readBoolean();

            if (buf.readBoolean()) {
                int len = buf.readInt();
                ImmutableMultiset.Builder<ItemVariant> builder = ImmutableMultiset.builder();

                for (int i = 0; i < len; i++) {
                    // Use ItemVariant's built-in PACKET_CODEC
                    ItemVariant variant = ItemVariant.PACKET_CODEC.decode((RegistryFriendlyByteBuf) buf);
                    int count = buf.readInt();
                    builder.addCopies(variant, count);
                }

                return new Data(isCopyPaste, Either.left(new Cache(builder.build())));
            } else {
                InteractionHand hand = buf.readEnum(InteractionHand.class);
                return new Data(isCopyPaste, Either.right(hand));
            }
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeBoolean(isCopyPaste);

            either.mapBoth(cache -> {
                buf.writeBoolean(true);
                buf.writeInt(cache.cache().entrySet().size());

                for (Multiset.Entry<ItemVariant> entry : cache.cache().entrySet()) {
                    ItemVariant variant = entry.getElement();

                    // Use ItemVariant's built-in PACKET_CODEC
                    ItemVariant.PACKET_CODEC.encode((RegistryFriendlyByteBuf) buf, variant);
                    buf.writeInt(entry.getCount());
                }

                return null;
            }, hand -> {
                buf.writeBoolean(false);
                buf.writeEnum(hand);
                return null;
            });
        }
    }

    public record Cache(Multiset<ItemVariant> cache) {
    }
}