package com.direwolf20.buildinggadgets.common.compat;

import com.direwolf20.buildinggadgets.common.network.Target;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateKey;
import com.direwolf20.buildinggadgets.common.tainted.template.ITemplateProvider;
import com.direwolf20.buildinggadgets.common.tainted.template.Template;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.UUID;

public class NoWorldCompat implements ITemplateProvider {
    @Override
    public UUID getId(ITemplateKey key) {
        return null;
    }

    @Override
    public Template getTemplateForKey(ITemplateKey key) {
        return null;
    }

    @Override
    public void setTemplate(ITemplateKey key, Template template) {

    }

    @Override
    public boolean requestUpdate(ITemplateKey key) {
        return false;
    }

    @Override
    public boolean requestUpdate(ITemplateKey key, Target target) {
        return false;
    }

    @Override
    public boolean requestRemoteUpdate(ITemplateKey key, Level level) {
        return false;
    }

    @Override
    public boolean requestRemoteUpdate(ITemplateKey key, Target target) {
        return false;
    }

    @Override
    public void registerUpdateListener(IUpdateListener listener) {

    }

    @Override
    public void removeUpdateListener(IUpdateListener listener) {

    }

    @Override
    public void readData(ValueInput input) {
    }

    @Override
    public void writeData(ValueOutput output) {
    }

    public void readFromNbt(CompoundTag compoundTag, HolderLookup.Provider provider) {
    }

    public void writeToNbt(CompoundTag compoundTag, HolderLookup.Provider provider) {

    }
}
