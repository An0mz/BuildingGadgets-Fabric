package com.direwolf20.buildinggadgets.common.tainted.template;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import org.ladysnake.cca.api.v3.component.ComponentKey;

import java.util.UUID;
import java.util.function.Supplier;

public class SimpleTemplateKey implements ITemplateKey {
    private final UUID id;

    public SimpleTemplateKey(UUID id) {
        this.id = id;
    }

    @Override
    public UUID getOrComputeId(Supplier<UUID> freeIdAllocator) {
        return id;
    }

    @Override
    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        // Immutable - no read needed
    }

    @Override
    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        // Immutable - no write needed
    }

    public boolean shouldSyncWith(ComponentKey<?> key) {
        return false;
    }
}