package me.ez.cobbleworks.client;

import me.ez.cobbleworks.Init;
import me.ez.cobbleworks.Main;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@EventBusSubscriber(modid = Main.MOD_ID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}
    public static void registerConfig(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
        event.register(Init.GENERATOR_MENU.get(), GeneratorScreen::new);
    }
}
