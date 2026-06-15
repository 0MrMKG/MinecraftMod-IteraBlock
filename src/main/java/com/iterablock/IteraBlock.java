package com.iterablock;

import com.iterablock.client.InitHandler;
import com.iterablock.client.hotkeys.VanillaKeyMappings;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = IteraBlock.MODID, dist = Dist.CLIENT)
public class IteraBlock {
    public static final String MODID = "iterablock";
    public static final String MOD_NAME = "IteraBlock";

    public IteraBlock(IEventBus modEventBus) {
        modEventBus.addListener(VanillaKeyMappings::register);
        InitHandler.register();
    }
}
