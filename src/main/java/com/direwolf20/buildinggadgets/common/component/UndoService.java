package com.direwolf20.buildinggadgets.common.component;

import com.direwolf20.buildinggadgets.common.BuildingGadgets;
import com.direwolf20.buildinggadgets.common.tainted.save.Undo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

import java.util.*;
import java.util.LinkedList;

public final class UndoService implements Component, ServerTickingComponent {

    private final Map<UUID, Deque<UndoData>> histories;
    private int tick;

    public UndoService() {
        this.histories = new HashMap<>();
    }

    public void insertUndo(UUID uuid, Undo undo) {
        Deque<UndoData> deque = histories.computeIfAbsent(uuid, $ -> new LinkedList<>());
        deque.push(new UndoData(System.currentTimeMillis() + BuildingGadgets.getConfig().gadgets.undoExpiry, undo));

        // Remove too many elements
        while (deque.size() > BuildingGadgets.getConfig().gadgets.undoSize) {
            deque.removeLast();
        }
    }

    public Optional<Undo> getUndo(UUID uuid) {
        Deque<UndoData> deque = histories.get(uuid);

        if (deque != null) {
            UndoData poll = deque.poll();

            if (poll != null) {
                return Optional.ofNullable(poll.undo());
            }
        }

        return Optional.empty();
    }

    @Override
    public void readData(ValueInput input) {
        histories.clear();
        for (String key : input.childrenListOrEmpty("__keys__").stream()
                .map(vi -> vi.getStringOr("k", ""))
                .filter(s -> !s.isEmpty())
                .toList()) {
            // fallback: just skip if format doesn't match
        }
        // Use nested child approach per UUID
        // Since there's no direct way to list all child keys in ValueInput,
        // we rely on the writeData format using a childrenList
        input.childrenListOrEmpty("histories").stream().forEach(entry -> {
            String uuidStr = entry.getStringOr("uuid", "");
            if (uuidStr.isEmpty()) return;
            LinkedList<UndoData> history = new LinkedList<>();
            entry.childrenListOrEmpty("data").stream().forEach(d -> {
                long expiry = d.getLongOr("Expiry", 0L);
                d.read("Undo", CompoundTag.CODEC)
                        .ifPresent(undoTag -> history.add(new UndoData(expiry, Undo.deserialize(undoTag))));
            });
            if (!history.isEmpty()) {
                histories.put(UUID.fromString(uuidStr), history);
            }
        });
    }

    @Override
    public void writeData(ValueOutput output) {
        ValueOutput.ValueOutputList historiesList = output.childrenList("histories");
        histories.forEach((uuid, history) -> {
            ValueOutput entry = historiesList.addChild();
            entry.putString("uuid", uuid.toString());
            ValueOutput.ValueOutputList dataList = entry.childrenList("data");
            for (UndoData data : history) {
                ValueOutput child = dataList.addChild();
                child.putLong("Expiry", data.expiry());
                child.store("Undo", CompoundTag.CODEC, data.undo().serialize());
            }
        });
    }

    public void readFromNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        histories.clear();

        for (String key : tag.keySet()) {
            LinkedList<UndoData> history = new LinkedList<>();

            for (Tag d : tag.getListOrEmpty(key)) {
                CompoundTag data = (CompoundTag) d;
                history.add(new UndoData(data.getLongOr("Expiry", 0L), Undo.deserialize(data.getCompoundOrEmpty("Undo"))));
            }

            histories.put(UUID.fromString(key), history);
        }
    }

    public void writeToNbt(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        histories.forEach((uuid, history) -> {
            ListTag list = new ListTag();

            for (UndoData data : history) {
                CompoundTag inner = new CompoundTag();
                inner.putLong("Expiry", data.expiry());
                inner.put("Undo", data.undo().serialize());
                list.add(inner);
            }

            tag.put(uuid.toString(), list);
        });
    }

    @Override
    public void serverTick() {
        // Only check every 30 seconds
        if (++tick % 600 == 0) {
            long now = System.currentTimeMillis();

            histories.entrySet().removeIf(entry -> {
                entry.getValue().removeIf(data -> data.expiry >= now);
                return entry.getValue().isEmpty();
            });
        }
    }

    private record UndoData(long expiry, Undo undo) {
    }
}
