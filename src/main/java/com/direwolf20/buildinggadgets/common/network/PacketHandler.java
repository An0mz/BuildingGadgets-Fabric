package com.direwolf20.buildinggadgets.common.network;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.network.C2S.*;
import com.direwolf20.buildinggadgets.common.network.bidirection.PacketRequestTemplate;
import com.direwolf20.buildinggadgets.common.network.bidirection.PacketSetRemoteInventoryCache;
import com.direwolf20.buildinggadgets.common.network.bidirection.SplitPacketUpdateTemplate;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;

public class PacketHandler {

    // Keep ResourceLocations for reference in packet classes
    public static final ResourceLocation PacketAnchor = BuildingGadgets.id("packet_anchor");
    public static final ResourceLocation PacketBindTool = BuildingGadgets.id("packet_bind_tool");
    public static final ResourceLocation PacketToggleFuzzy = BuildingGadgets.id("packet_toggle_fuzzy");
    public static final ResourceLocation PacketToggleFluidOnly = BuildingGadgets.id("packet_toggle_fluid_only");
    public static final ResourceLocation PacketToggleConnectedArea = BuildingGadgets.id("packet_toggle_connected_area");
    public static final ResourceLocation PacketToggleRayTraceFluid = BuildingGadgets.id("packet_toggle_ray_trace_fluid");
    public static final ResourceLocation PacketToggleBlockPlacement = BuildingGadgets.id("packet_toggle_block_placement");
    public static final ResourceLocation PacketChangeRange = BuildingGadgets.id("packet_change_range");
    public static final ResourceLocation PacketRotateMirror = BuildingGadgets.id("packet_rotate_mirror");
    public static final ResourceLocation PacketCopyCoords = BuildingGadgets.id("packet_copy_coords");
    public static final ResourceLocation PacketDestructionGUI = BuildingGadgets.id("packet_destruction_gui");
    public static final ResourceLocation PacketPasteGUI = BuildingGadgets.id("packet_paste_gui");
    public static final ResourceLocation PacketToggleMode = BuildingGadgets.id("packet_toggle_mode");
    public static final ResourceLocation PacketUndo = BuildingGadgets.id("packet_undo");

    public static final ResourceLocation PacketTemplateManagerTemplateCreated = BuildingGadgets.id("packet_template_manager_template_created");
    public static final ResourceLocation SplitPacketUpdateTemplate = BuildingGadgets.id("split_packet_update_template");
    public static final ResourceLocation PacketSetRemoteInventoryCache = BuildingGadgets.id("packet_set_remote_inventory_cache");

    public static final ResourceLocation PacketRequestTemplate = BuildingGadgets.id("packet_request_template");

    public static final ResourceLocation PacketLookupResult = BuildingGadgets.id("packet_lookup_result");

    public static void registerMessages() {
        // Register payload types (codecs) - C2S packets
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketAnchor.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketAnchor.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketBindTool.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketBindTool.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleFuzzy.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleFuzzy.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleFluidOnly.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleFluidOnly.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleConnectedArea.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleConnectedArea.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleRayTraceFluid.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleRayTraceFluid.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleBlockPlacement.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleBlockPlacement.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketChangeRange.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketChangeRange.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketRotateMirror.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketRotateMirror.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketCopyCoords.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketCopyCoords.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketDestructionGUI.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketDestructionGUI.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketPasteGUI.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketPasteGUI.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleMode.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleMode.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketUndo.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketUndo.CODEC);
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.C2S.PacketTemplateManagerTemplateCreated.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketTemplateManagerTemplateCreated.CODEC);

        // Register payload types (codecs) - Bidirectional packets (both C2S and S2C)
        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.bidirection.PacketRequestTemplate.TYPE, com.direwolf20.buildinggadgets.common.network.bidirection.PacketRequestTemplate.CODEC);
        PayloadTypeRegistry.playS2C().register(com.direwolf20.buildinggadgets.common.network.bidirection.PacketRequestTemplate.TYPE, com.direwolf20.buildinggadgets.common.network.bidirection.PacketRequestTemplate.CODEC);

        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.bidirection.SplitPacketUpdateTemplate.TYPE, com.direwolf20.buildinggadgets.common.network.bidirection.SplitPacketUpdateTemplate.CODEC);
        PayloadTypeRegistry.playS2C().register(com.direwolf20.buildinggadgets.common.network.bidirection.SplitPacketUpdateTemplate.TYPE, com.direwolf20.buildinggadgets.common.network.bidirection.SplitPacketUpdateTemplate.CODEC);

        PayloadTypeRegistry.playC2S().register(com.direwolf20.buildinggadgets.common.network.bidirection.PacketSetRemoteInventoryCache.TYPE, com.direwolf20.buildinggadgets.common.network.bidirection.PacketSetRemoteInventoryCache.CODEC);
        PayloadTypeRegistry.playS2C().register(com.direwolf20.buildinggadgets.common.network.bidirection.PacketSetRemoteInventoryCache.TYPE, com.direwolf20.buildinggadgets.common.network.bidirection.PacketSetRemoteInventoryCache.CODEC);

        // Register server-side handlers (receives C2S packets)
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketAnchor.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketAnchor::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketBindTool.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketBindTool::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleFuzzy.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleFuzzy::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleFluidOnly.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleFluidOnly::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleConnectedArea.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleConnectedArea::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleRayTraceFluid.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleRayTraceFluid::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleBlockPlacement.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleBlockPlacement::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketChangeRange.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketChangeRange::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketRotateMirror.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketRotateMirror::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketCopyCoords.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketCopyCoords::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketDestructionGUI.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketDestructionGUI::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketPasteGUI.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketPasteGUI::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleMode.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketToggleMode::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketUndo.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketUndo::handle);
        ServerPlayNetworking.registerGlobalReceiver(com.direwolf20.buildinggadgets.common.network.C2S.PacketTemplateManagerTemplateCreated.TYPE, com.direwolf20.buildinggadgets.common.network.C2S.PacketTemplateManagerTemplateCreated::handle);

        // Register server-side handlers for bidirectional packets (receives C2S)
        ServerPlayNetworking.registerGlobalReceiver(
                com.direwolf20.buildinggadgets.common.network.bidirection.PacketRequestTemplate.TYPE,
                com.direwolf20.buildinggadgets.common.network.bidirection.PacketRequestTemplate.Server::handle
        );
        ServerPlayNetworking.registerGlobalReceiver(
                com.direwolf20.buildinggadgets.common.network.bidirection.SplitPacketUpdateTemplate.TYPE,
                com.direwolf20.buildinggadgets.common.network.bidirection.SplitPacketUpdateTemplate.Server::handle
        );
        ServerPlayNetworking.registerGlobalReceiver(
                com.direwolf20.buildinggadgets.common.network.bidirection.PacketSetRemoteInventoryCache.TYPE,
                com.direwolf20.buildinggadgets.common.network.bidirection.PacketSetRemoteInventoryCache.Server::handle
        );
    }
}