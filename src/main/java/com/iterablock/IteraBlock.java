package com.iterablock;

import com.iterablock.client.InitHandler;
import com.iterablock.network.IteraBlockNetwork;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(IteraBlock.MODID)
public class IteraBlock {
    public static final String MODID = "iterablock";
    public static final String MOD_NAME = "IteraBlock";

    public IteraBlock(IEventBus modEventBus) {
        modEventBus.addListener(IteraBlockNetwork::register);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            InitHandler.register();
        }
    }
}
