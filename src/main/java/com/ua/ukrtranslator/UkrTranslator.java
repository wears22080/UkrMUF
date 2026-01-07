package com.ua.ukrtranslator;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("ukrtranslator")
public class UkrTranslator {
    public UkrTranslator() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Translation handler = new Translation();
            FMLJavaModLoadingContext.get().getModEventBus().addListener(Translation::onClientSetup);
            MinecraftForge.EVENT_BUS.register(handler);
            MinecraftForge.EVENT_BUS.addListener(handler::onRegisterCommands);
        }
    }
}