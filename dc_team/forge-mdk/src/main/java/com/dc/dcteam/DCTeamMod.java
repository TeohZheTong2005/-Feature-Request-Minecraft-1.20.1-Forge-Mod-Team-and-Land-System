package com.dc.dcteam;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(DCTeamMod.MOD_ID)
public class DCTeamMod {
    public static final String MOD_ID = "dcteam";
    private static final Logger LOGGER = LogUtils.getLogger();

    public DCTeamMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("DC Team Mod is initializing...");
        event.enqueueWork(() -> {
            // 注册网络包处理器
            TeamPacketHandler.register();
            LOGGER.info("Network handlers registered.");
        });
    }
}