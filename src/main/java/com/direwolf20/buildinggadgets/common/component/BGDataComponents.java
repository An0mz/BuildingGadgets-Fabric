package com.direwolf20.buildinggadgets.common.component;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

public class BGDataComponents {

    // Energy storage
    public static final DataComponentType<Long> ENERGY = register("energy",
            builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    // Fuzzy mode
    public static final DataComponentType<Boolean> FUZZY = register("fuzzy",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // Connected area
    public static final DataComponentType<Boolean> UNCONNECTED_AREA = register("unconnected_area",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // Raytrace fluid
    public static final DataComponentType<Boolean> RAYTRACE_FLUID = register("raytrace_fluid",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // UUID
    public static final DataComponentType<UUID> GADGET_UUID = register("gadget_uuid",
            builder -> builder.persistent(Codec.STRING.xmap(UUID::fromString, UUID::toString)));

    // Anchor position
    public static final DataComponentType<BlockPos> ANCHOR = register("anchor",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    // Building mode (stores mode name as string)
    public static final DataComponentType<String> BUILDING_MODE = register("building_mode",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // Place inside/atop toggle
    public static final DataComponentType<Boolean> PLACE_INSIDE = register("place_inside",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // Copy/Paste mode
    public static final DataComponentType<String> COPYPASTE_MODE = register("copypaste_mode",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // Relative position vector
    public static final DataComponentType<BlockPos> RELATIVE_VECTOR = register("relative_vector",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    // Copy counter
    public static final DataComponentType<Integer> COPY_COUNTER = register("copy_counter",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // Region bounds (upper/start)
    public static final DataComponentType<BlockPos> UPPER_REGION_BOUND = register("upper_region_bound",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    // Region bounds (lower/end)
    public static final DataComponentType<BlockPos> LOWER_REGION_BOUND = register("lower_region_bound",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    // In BGDataComponents.java, add:

    // Destruction gadget anchor side
    public static final DataComponentType<String> ANCHOR_SIDE = register("anchor_side",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // Destruction gadget dimensions
    public static final DataComponentType<Integer> DESTRUCTION_LEFT = register("destruction_left",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DataComponentType<Integer> DESTRUCTION_RIGHT = register("destruction_right",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DataComponentType<Integer> DESTRUCTION_UP = register("destruction_up",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DataComponentType<Integer> DESTRUCTION_DOWN = register("destruction_down",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DataComponentType<Integer> DESTRUCTION_DEPTH = register("destruction_depth",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // Destruction overlay toggle
    public static final DataComponentType<Boolean> DESTRUCTION_OVERLAY = register("destruction_overlay",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    // Fluid-only mode
    public static final DataComponentType<Boolean> FLUID_ONLY = register("fluid_only",
            builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    public static final DataComponentType<String> EXCHANGER_MODE = register("exchanger_mode",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DataComponentType<String> REMOTE_INVENTORY_DIM = register("remote_inventory_dim",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    public static final DataComponentType<BlockPos> REMOTE_INVENTORY_POS = register("remote_inventory_pos",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));

    public static final DataComponentType<String> REMOTE_INVENTORY_FACE = register("remote_inventory_face",
            builder -> builder.persistent(Codec.STRING).networkSynchronized(ByteBufCodecs.STRING_UTF8));

    // Tool range
    public static final DataComponentType<Integer> TOOL_RANGE = register("tool_range",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    // Tool block (BlockData serialized as CompoundTag)
    public static final DataComponentType<CompoundTag> TOOL_BLOCK = register("tool_block",
            builder -> builder.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));

    // Anchor coordinates (list of positions)
    public static final DataComponentType<List<BlockPos>> ANCHOR_COORDS = register("anchor_coords",
            builder -> builder.persistent(BlockPos.CODEC.listOf()).networkSynchronized(ByteBufCodecs.fromCodec(BlockPos.CODEC.listOf())));

    // Template Key (stores UUID of template)
    public static final DataComponentType<UUID> TEMPLATE_KEY = register("template_key",
            builder -> builder.persistent(Codec.STRING.xmap(UUID::fromString, UUID::toString))
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8.map(UUID::fromString, UUID::toString)));

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                BuildingGadgets.id(name),
                builder.apply(DataComponentType.builder()).build()
        );
    }

    public static void register() {
        // Called to initialize static fields
        BuildingGadgets.LOG.info("Registering Building Gadgets Data Components");
    }
}