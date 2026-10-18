package betteradvancements.mixin;

import betteradvancements.common.gui.BetterAdvancementsScreen;
import betteradvancements.common.reference.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Gui.class)
public class MinecraftMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private Screen swapAdvancementsScreen(Screen screen) {
        if (screen instanceof AdvancementsScreen) {
            Constants.log.info("Replacing vanilla advancements screen");
            return BetterAdvancementsScreen.create(Minecraft.getInstance().player.connection.getAdvancements(),
                ((AdvancementsScreenAccessor) screen).betteradvancements$getLastScreen());
        } else {
            return screen;
        }
    }
}
