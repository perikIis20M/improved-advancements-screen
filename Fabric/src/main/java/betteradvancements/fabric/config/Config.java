package betteradvancements.fabric.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class Config {
    public static Screen createConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("title.betteradvancements.config"));

        builder.setSavingRunnable(ConfigFileHandler::writeToConfig);

        ConfigValues.buildGeneral(builder.getOrCreateCategory(Component.translatable("category.improvedadvancements.general")), builder.entryBuilder());
        ConfigValues.buildModern(builder.getOrCreateCategory(Component.translatable("category.improvedadvancements.modern")), builder.entryBuilder());
        ConfigValues.buildOld(builder.getOrCreateCategory(Component.translatable("category.improvedadvancements.old")), builder.entryBuilder());

        Screen screen = builder.build();
        ((AdvancementsConfigTabs) screen).improvedadvancements$enableArrowFreeTabs();
        return screen;
    }
}
