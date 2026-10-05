package me.ez.cobbleworks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Main.MOD_ID)
public final class Main {
    public static final String MOD_ID = "cobbleworks";

    public Main(IEventBus bus, ModContainer container) {
        Init.BLOCKS.register(bus);
        Init.ITEMS.register(bus);
        Init.BLOCK_ENTITIES.register(bus);
        Init.MENUS.register(bus);
        bus.addListener(Init::creativeContents);
        bus.addListener(Init::capabilities);
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        if (FMLEnvironment.getDist() == Dist.CLIENT) me.ez.cobbleworks.client.ClientSetup.registerConfig(container);
        if (Boolean.getBoolean("cobbleworks.testing")) me.ez.cobbleworks.testing.GeneratorGameTests.register(bus);
    }
}
