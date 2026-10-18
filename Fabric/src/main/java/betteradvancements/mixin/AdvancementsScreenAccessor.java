package betteradvancements.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Retains the screen supplied by vanilla when replacing its advancements screen. */
@Mixin(AdvancementsScreen.class)
public interface AdvancementsScreenAccessor {
    @Accessor("lastScreen")
    Screen betteradvancements$getLastScreen();
}
