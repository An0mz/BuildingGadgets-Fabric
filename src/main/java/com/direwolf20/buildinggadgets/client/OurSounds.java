package com.direwolf20.buildinggadgets.client;

import com.direwolf20.buildinggadgets.common.util.ref.Reference;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.Registry;

public class OurSounds {

    public static SoundEvent BEEP;

    public static void initSounds() {
        ResourceLocation loc = new ResourceLocation(Reference.MODID, "beep");

        // Use factory method instead of constructor
        BEEP = SoundEvent.createVariableRangeEvent(loc);

        // Register the sound
        Registry.register(BuiltInRegistries.SOUND_EVENT, loc, BEEP);
    }

    public static void playSound() {
        playSound(1.0F);
    }

    public static void playSound(float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && BEEP != null) {
            mc.player.playSound(BEEP, 1.0F, pitch);
        }
    }

}
