package com.direwolf20.buildinggadgets.common.tainted.template;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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
    public void readData(ValueInput input) {
        // Immutable - no read needed
    }

    @Override
    public void writeData(ValueOutput output) {
        // Immutable - no write needed
    }

    public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        // Immutable - no read needed
    }

    public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
        // Immutable - no write needed
    }
}