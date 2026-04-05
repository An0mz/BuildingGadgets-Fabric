package com.direwolf20.buildinggadgets.common.tainted.template;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * A very simple {@link ITemplateKey} which allows to query an {@link ITemplateProvider} for a specific Template, without
 * having the CapabilityProvider at hand. (For example useful for packets)
 */
public final class TemplateKey implements ITemplateKey {
    @Nullable
    private UUID id;

    public TemplateKey(@Nullable UUID id) {
        this.id = id;
    }

    @Override
    public UUID getOrComputeId(Supplier<UUID> freeIdAllocator) {
        if (id == null) {
            setId(freeIdAllocator.get());
        }

        return id;
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public void setId(@Nullable UUID id) {
        this.id = id;
    }

    @Override
    public void readData(ValueInput input) {
        String idStr = input.getStringOr("id", "");
        id = idStr.isEmpty() ? null : UUID.fromString(idStr);
    }

    @Override
    public void writeData(ValueOutput output) {
        if (id != null) output.putString("id", id.toString());
    }

    public void readFromNbt(CompoundTag tag, HolderLookup.Provider provider) {
        String idStr = tag.getStringOr("id", "");
        id = idStr.isEmpty() ? null : UUID.fromString(idStr);
    }

    public void writeToNbt(CompoundTag tag, HolderLookup.Provider provider) {
        if (id != null) tag.putString("id", id.toString());
    }

}
