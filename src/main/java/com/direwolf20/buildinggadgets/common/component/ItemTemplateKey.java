package com.direwolf20.buildinggadgets.common.component;

import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateKey;
import com.direwolf20.buildinggadgets.common.util.ref.NBTKeys;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.UUID;
import java.util.function.Supplier;

public final class ItemTemplateKey implements Component, AutoSyncedComponent, ITemplateKey {

    private UUID id;

    public ItemTemplateKey(ItemStack stack) {
        // stack is provided by the component factory, you do NOT store it
    }

    @Override
    public UUID getOrComputeId(Supplier<UUID> freeIdAllocator) {
        if (id == null) {
            id = freeIdAllocator.get();
        }
        return id;
    }

    public void readFromNbt(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains(NBTKeys.TEMPLATE_KEY_ID)) {
            String idStr = tag.getStringOr(NBTKeys.TEMPLATE_KEY_ID, "");
            id = idStr.isEmpty() ? null : UUID.fromString(idStr);
        }
    }

    public void writeToNbt(CompoundTag tag, HolderLookup.Provider provider) {
        if (id != null) {
            tag.putString(NBTKeys.TEMPLATE_KEY_ID, id.toString());
        }
    }
}
