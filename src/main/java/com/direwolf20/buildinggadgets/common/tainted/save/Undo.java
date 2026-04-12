package com.direwolf20.buildinggadgets.common.tainted.save;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.tainted.building.BlockData;
import com.direwolf20.buildinggadgets.common.tainted.building.Region;
import com.direwolf20.buildinggadgets.common.tainted.building.tilesupport.ITileDataSerializer;
import com.direwolf20.buildinggadgets.common.tainted.building.tilesupport.ITileEntityData;
import com.direwolf20.buildinggadgets.common.tainted.building.tilesupport.NBTTileEntityData;
import com.direwolf20.buildinggadgets.common.tainted.building.tilesupport.TileSupport;
import com.direwolf20.buildinggadgets.common.tainted.registry.Registries;
import com.direwolf20.buildinggadgets.common.util.compression.DataCompressor;
import com.direwolf20.buildinggadgets.common.util.compression.DataDecompressor;
import com.direwolf20.buildinggadgets.common.util.helpers.NBTHelper;
import com.direwolf20.buildinggadgets.common.util.ref.NBTKeys;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

public record Undo(ResourceKey<Level> dim, Map<BlockPos, BlockInfo> dataMap, Region boundingBox) {

    public static Undo deserialize(CompoundTag nbt) {
        Preconditions.checkArgument(nbt.contains(NBTKeys.WORLD_SAVE_DIM)
                && nbt.contains(NBTKeys.WORLD_SAVE_UNDO_BLOCK_LIST)
                && nbt.contains(NBTKeys.WORLD_SAVE_UNDO_DATA_LIST)
                && nbt.contains(NBTKeys.WORLD_SAVE_UNDO_DATA_SERIALIZER_LIST));
        DataDecompressor<ITileDataSerializer> serializerReverseObjectIncrementer = new DataDecompressor<>(
                (ListTag) nbt.get(NBTKeys.WORLD_SAVE_UNDO_DATA_SERIALIZER_LIST),
                inbt -> {
                    String s = inbt.asString().orElse("");
                    ITileDataSerializer serializer = Registries.getTileDataSerializers().getValue(Identifier.parse(s));
                    if (serializer == null) {
                        BuildingGadgets.LOG.warn("Found unknown serializer {}. Replacing with dummy!", s);
                        serializer = TileSupport.dummyTileEntityData().getSerializer();
                    }
                    return serializer;
                },
                value -> {
                    BuildingGadgets.LOG.warn("Attempted to query unknown serializer {}. Replacing with dummy!", value);
                    return TileSupport.dummyTileEntityData().getSerializer();
                });
        DataDecompressor<BlockData> dataReverseObjectIncrementer = new DataDecompressor<>(
                (ListTag) nbt.get(NBTKeys.WORLD_SAVE_UNDO_DATA_LIST),
                inbt -> BlockData.deserialize((CompoundTag) inbt, serializerReverseObjectIncrementer, true),
                value -> BlockData.AIR);
        DataDecompressor<Multiset<ItemVariant>> itemSetReverseObjectIncrementer = new DataDecompressor<>(
                (ListTag) nbt.get(NBTKeys.WORLD_SAVE_UNDO_ITEMS_LIST),
                inbt -> NBTHelper.deserializeMultisetEntries((ListTag) inbt, HashMultiset.create(), Undo::readEntry),
                value -> HashMultiset.create());
        Map<BlockPos, BlockInfo> map = NBTHelper.deserializeMap(
                (ListTag) nbt.get(NBTKeys.WORLD_SAVE_UNDO_BLOCK_LIST), new HashMap<>(),
                inbt -> {
                    CompoundTag posTag = ((CompoundTag) inbt).getCompoundOrEmpty("pos");
                    return new BlockPos(posTag.getIntOr("X", 0), posTag.getIntOr("Y", 0), posTag.getIntOr("Z", 0));
                },
                inbt -> BlockInfo.deserialize((CompoundTag) inbt, dataReverseObjectIncrementer, itemSetReverseObjectIncrementer));

        ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(nbt.getStringOr(NBTKeys.WORLD_SAVE_DIM, "")));
        Region bounds = Region.deserializeFrom(nbt.getCompoundOrEmpty(NBTKeys.WORLD_SAVE_UNDO_BOUNDS));
        return new Undo(dim, map, bounds);
    }

    private static Tuple<ItemVariant, Integer> readEntry(Tag inbt) {
        CompoundTag nbt = (CompoundTag) inbt;
        int count = nbt.getIntOr(NBTKeys.UNIQUE_ITEM_COUNT, 0);

        // Deserialize ItemVariant from NBT
        CompoundTag itemData = nbt.getCompoundOrEmpty(NBTKeys.UNIQUE_ITEM_ITEM);
        Identifier itemId = Identifier.parse(itemData.getStringOr("item", "minecraft:air"));
        Item item = BuiltInRegistries.ITEM.getValue(itemId);

        DataComponentPatch components = DataComponentPatch.CODEC
                .parse(NbtOps.INSTANCE, itemData.get("components"))
                .resultOrPartial(error -> {})
                .orElse(DataComponentPatch.EMPTY);

        ItemVariant variant = ItemVariant.of(item, components);
        return new Tuple<>(variant, count);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Region getBoundingBox() {
        return boundingBox;
    }

    public Map<BlockPos, BlockInfo> getUndoData() {
        return Collections.unmodifiableMap(dataMap);
    }

    public CompoundTag serialize() {
        DataCompressor<BlockData> dataObjectIncrementer = new DataCompressor<>();
        DataCompressor<Multiset<ItemVariant>> itemObjectIncrementer = new DataCompressor<>();
        DataCompressor<ITileDataSerializer> serializerObjectIncrementer = new DataCompressor<>();
        CompoundTag res = new CompoundTag();

        ListTag infoList = NBTHelper.serializeMap(dataMap, pos -> {
            CompoundTag tag = new CompoundTag();
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("X", pos.getX());
            posTag.putInt("Y", pos.getY());
            posTag.putInt("Z", pos.getZ());
            tag.put("pos", posTag);
            return tag;
        }, i -> i.serialize(dataObjectIncrementer, itemObjectIncrementer));
        ListTag dataList = dataObjectIncrementer.write(d -> d.serialize(serializerObjectIncrementer, true));
        ListTag itemSetList = itemObjectIncrementer.write(ms -> NBTHelper.writeIterable(ms.entrySet(), this::writeEntry));
        ListTag dataSerializerList = serializerObjectIncrementer.write(ts -> StringTag.valueOf(Registries.getTileDataSerializers().getKey(ts).toString()));

        res.putString(NBTKeys.WORLD_SAVE_DIM, dim.identifier().toString());
        res.put(NBTKeys.WORLD_SAVE_UNDO_BLOCK_LIST, infoList);
        res.put(NBTKeys.WORLD_SAVE_UNDO_DATA_LIST, dataList);
        res.put(NBTKeys.WORLD_SAVE_UNDO_DATA_SERIALIZER_LIST, dataSerializerList);
        res.put(NBTKeys.WORLD_SAVE_UNDO_ITEMS_LIST, itemSetList);
        res.put(NBTKeys.WORLD_SAVE_UNDO_BOUNDS, boundingBox.serialize());

        return res;
    }

    private CompoundTag writeEntry(Entry<ItemVariant> entry) {
        CompoundTag res = new CompoundTag();

        // Serialize ItemVariant to NBT
        ItemVariant variant = entry.getElement();
        CompoundTag itemData = new CompoundTag();
        itemData.putString("item", BuiltInRegistries.ITEM.getKey(variant.getItem()).toString());

        DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, variant.toStack().getComponentsPatch())
                .resultOrPartial(error -> {})
                .ifPresent(tag -> itemData.put("components", tag));

        res.put(NBTKeys.UNIQUE_ITEM_ITEM, itemData);
        res.putInt(NBTKeys.UNIQUE_ITEM_COUNT, entry.getCount());
        return res;
    }

    public record BlockInfo(BlockData recordedData, BlockData placedData, Multiset<ItemVariant> usedItems,
                            Multiset<ItemVariant> producedItems) {
        private static BlockInfo deserialize(CompoundTag nbt, IntFunction<BlockData> dataSupplier, IntFunction<Multiset<ItemVariant>> itemSetSupplier) {
            BlockData data = dataSupplier.apply(nbt.getIntOr(NBTKeys.WORLD_SAVE_UNDO_RECORDED_DATA, 0));
            BlockData placedData = dataSupplier.apply(nbt.getIntOr(NBTKeys.WORLD_SAVE_UNDO_PLACED_DATA, 0));
            Multiset<ItemVariant> usedItems = itemSetSupplier.apply(nbt.getIntOr(NBTKeys.WORLD_SAVE_UNDO_ITEMS_USED, 0));
            Multiset<ItemVariant> producedItems = itemSetSupplier.apply(nbt.getIntOr(NBTKeys.WORLD_SAVE_UNDO_ITEMS_PRODUCED, 0));
            return new BlockInfo(data, placedData, usedItems, producedItems);
        }

        private CompoundTag serialize(ToIntFunction<BlockData> dataIdSupplier, ToIntFunction<Multiset<ItemVariant>> itemIdSupplier) {
            CompoundTag res = new CompoundTag();
            res.putInt(NBTKeys.WORLD_SAVE_UNDO_RECORDED_DATA, dataIdSupplier.applyAsInt(recordedData));
            res.putInt(NBTKeys.WORLD_SAVE_UNDO_PLACED_DATA, dataIdSupplier.applyAsInt(placedData));
            res.putInt(NBTKeys.WORLD_SAVE_UNDO_ITEMS_USED, itemIdSupplier.applyAsInt(usedItems));
            res.putInt(NBTKeys.WORLD_SAVE_UNDO_ITEMS_PRODUCED, itemIdSupplier.applyAsInt(producedItems));
            return res;
        }

        public BlockData getRecordedData() {
            return recordedData;
        }

        public BlockData getPlacedData() {
            return placedData;
        }

        public Multiset<ItemVariant> getUsedItems() {
            return Multisets.unmodifiableMultiset(usedItems);
        }

        public Multiset<ItemVariant> getProducedItems() {
            return Multisets.unmodifiableMultiset(producedItems);
        }
    }

    public static final class Builder {
        private final ImmutableMap.Builder<BlockPos, BlockInfo> mapBuilder;
        private Region.Builder regionBuilder;

        private Builder() {
            mapBuilder = ImmutableMap.builder();
            regionBuilder = null;
        }

        public Builder record(BlockGetter reader, BlockPos pos, BlockData placeData, Multiset<ItemVariant> requiredItems, Multiset<ItemVariant> producedItems) {
            BlockState state = reader.getBlockState(pos);
            BlockEntity be = reader.getBlockEntity(pos);
            ITileEntityData data = be != null ? NBTTileEntityData.ofTile(be) : TileSupport.dummyTileEntityData();
            return record(pos, new BlockData(state, data), placeData, requiredItems, producedItems);
        }

        private Builder record(BlockPos pos, BlockData recordedData, BlockData placedData, Multiset<ItemVariant> requiredItems, Multiset<ItemVariant> producedItems) {
            mapBuilder.put(pos, new BlockInfo(recordedData, placedData, requiredItems, producedItems));
            if (regionBuilder == null)
                regionBuilder = Region.enclosingBuilder();
            regionBuilder.enclose(pos);
            return this;
        }

        public Undo build(Level dim) {
            return new Undo(dim.dimension(), mapBuilder.build(), regionBuilder != null ? regionBuilder.build() : Region.singleZero());
        }
    }
}