package com.unnameduser.tradeoverhaul.common;

import com.unnameduser.tradeoverhaul.TradeOverhaulMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final Identifier DISASSEMBLE_ID = new Identifier(TradeOverhaulMod.MOD_ID, "disassemble");
    public static final Identifier CRAFT_1_ID = new Identifier(TradeOverhaulMod.MOD_ID, "craft_1");
    public static final Identifier CRAFT_2_ID = new Identifier(TradeOverhaulMod.MOD_ID, "craft_2");

    public static final SoundEvent DISASSEMBLE = SoundEvent.of(DISASSEMBLE_ID);
    public static final SoundEvent CRAFT_1 = SoundEvent.of(CRAFT_1_ID);
    public static final SoundEvent CRAFT_2 = SoundEvent.of(CRAFT_2_ID);

    public static void register() {
        Registry.register(Registries.SOUND_EVENT, DISASSEMBLE_ID, DISASSEMBLE);
        Registry.register(Registries.SOUND_EVENT, CRAFT_1_ID, CRAFT_1);
        Registry.register(Registries.SOUND_EVENT, CRAFT_2_ID, CRAFT_2);
    }
}