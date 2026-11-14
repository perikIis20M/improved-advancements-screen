// Adapted from Improved Statistics Screen (MIT). See licenses/ImprovedStatisticsScreen-MIT.txt.
package betteradvancements.mixin.config;

import betteradvancements.fabric.config.AdvancementsConfigTabs;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigTabButton;
import net.minecraft.client.gui.components.AbstractWidget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClothConfigTabButton.class, remap = false)
public abstract class ClothConfigTabButtonMixin {
    @Shadow @Final private ClothConfigScreen screen;
    @Inject(method = "isMouseOver", at = @At("HEAD"), cancellable = true)
    private void improvedadvancements$fullHitbox(double x,double y,CallbackInfoReturnable<Boolean> cir) {
        if (!((AdvancementsConfigTabs)screen).improvedadvancements$arrowFreeTabs()) return;
        var button=(AbstractWidget)(Object)this;
        cir.setReturnValue(button.visible && x>=button.getX() && x<button.getRight() && y>=button.getY() && y<button.getBottom());
    }
}
