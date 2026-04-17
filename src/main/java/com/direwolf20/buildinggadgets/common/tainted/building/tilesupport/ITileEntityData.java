package com.direwolf20.buildinggadgets.common.tainted.building.tilesupport;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.tainted.building.view.BuildContext;
import com.direwolf20.buildinggadgets.common.tainted.inventory.materials.MaterialList;
import com.google.common.collect.Multiset;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

public interface ITileEntityData {
    ITileDataSerializer getSerializer();

    boolean placeIn(BuildContext context, BlockState state, BlockPos position);

    default boolean allowPlacement(BuildContext context, BlockState state, BlockPos pos) {
        return true;
    }

    default MaterialList getRequiredItems(BuildContext context, BlockState state, @Nullable HitResult target, @Nullable BlockPos pos) {
        Item item = null;
        try {
            item = state.getBlock().asItem();
        } catch (Exception e) {
            BuildingGadgets.LOG.trace("Failed to retrieve item for {}.", state, e);
        }

        if (item == null) {
            item = state.getBlock().asItem();
        }

        ItemStack stack = new ItemStack(item);
        if (stack.isEmpty()) {
            return MaterialList.empty();
        }

        // Use ItemVariant.of(item) not ItemVariant.of(stack) to avoid DataComponent
        // mismatches in 1.21.5. A freshly created ItemStack may have different default
        // DataComponent values than items in the player's inventory, causing match()
        // to return false even when the player has the correct item.
        return MaterialList.of(ItemVariant.of(item));
    }
}
