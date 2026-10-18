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

        ConfigValues.buildScreen(builder.getOrCreateCategory(Component.translatable("category.improvedadvancements.screen")), builder.entryBuilder());
        ConfigValues.buildDetails(builder.getOrCreateCategory(Component.translatable("category.improvedadvancements.details")), builder.entryBuilder());
        ConfigValues.buildAppearance(builder.getOrCreateCategory(Component.translatable("category.improvedadvancements.appearance")), builder.entryBuilder());
        ConfigValues.buildAdvanced(builder.getOrCreateCategory(Component.translatable("category.improvedadvancements.advanced")), builder.entryBuilder());

        return builder.build();
    }
}
