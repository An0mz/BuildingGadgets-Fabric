package com.direwolf20.buildinggadgets.common.network;

import com.direwolf20.buildinggadgets.common.network.bidirection.PacketRequestTemplate;
import com.direwolf20.buildinggadgets.common.network.bidirection.PacketSetRemoteInventoryCache;
import com.direwolf20.buildinggadgets.common.network.bidirection.SplitPacketUpdateTemplate;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class ClientPacketHandler {

    public static void registerMessages() {
        ClientPlayNetworking.registerGlobalReceiver(PacketRequestTemplate.TYPE, PacketRequestTemplate.Client::handle);
        ClientPlayNetworking.registerGlobalReceiver(SplitPacketUpdateTemplate.TYPE, SplitPacketUpdateTemplate.Client::handle);
        ClientPlayNetworking.registerGlobalReceiver(PacketSetRemoteInventoryCache.TYPE, PacketSetRemoteInventoryCache.Client::handle);
    }
}