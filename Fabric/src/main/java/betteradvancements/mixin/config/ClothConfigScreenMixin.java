// Adapted from Improved Statistics Screen (MIT). See licenses/ImprovedStatisticsScreen-MIT.txt.
package betteradvancements.mixin.config;

import betteradvancements.fabric.config.AdvancementsConfigTabs;
import me.shedaniel.clothconfig2.gui.ClothConfigScreen;
import me.shedaniel.clothconfig2.gui.ClothConfigTabButton;
import me.shedaniel.math.Rectangle;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.List;

@Mixin(value = ClothConfigScreen.class, remap = false)
public abstract class ClothConfigScreenMixin extends Screen implements AdvancementsConfigTabs {
    @Unique private static final int improvedadvancements$BAR_LEFT = 4;
    @Unique private static final int improvedadvancements$BAR_TOP = 41;
    @Unique private static final int improvedadvancements$TAB_GAP = 2;
    @Shadow @Final private List<ClothConfigTabButton> tabButtons;
    @Shadow private AbstractWidget buttonLeftTab;
    @Shadow private AbstractWidget buttonRightTab;
    @Shadow private Rectangle tabsBounds;
    @Unique private boolean improvedadvancements$arrowFree;
    protected ClothConfigScreenMixin(Component title) { super(title); }
    public void improvedadvancements$enableArrowFreeTabs() { improvedadvancements$arrowFree=true; }
    public boolean improvedadvancements$arrowFreeTabs() { return improvedadvancements$arrowFree; }

    @Inject(method = "init", at = @At("TAIL"))
    private void improvedadvancements$initTabs(CallbackInfo ci) { improvedadvancements$layoutTabs(); }

    // Run after Cloth has placed its tabs. Unlike replacing the shared 24px constant,
    // this composes with Improved Statistics Screen's own layout mixin.
    @Inject(method = "extractRenderState", at = @At(value = "INVOKE",
        target = "Lme/shedaniel/clothconfig2/gui/ClothConfigScreen;isTransparentBackground()Z"))
    private void improvedadvancements$tabSizes(GuiGraphicsExtractor graphics,int mx,int my,float delta,CallbackInfo ci) {
        improvedadvancements$layoutTabs();
    }

    @Unique private void improvedadvancements$layoutTabs() {
        if (!improvedadvancements$arrowFree || tabButtons.isEmpty()) return;
        buttonLeftTab.visible=false;
        buttonRightTab.visible=false;
        int x=improvedadvancements$BAR_LEFT+improvedadvancements$TAB_GAP;
        for (var button : tabButtons) {
            button.setPosition(x,improvedadvancements$BAR_TOP+improvedadvancements$TAB_GAP);
            x+=button.getWidth()+improvedadvancements$TAB_GAP;
        }
    }

    @ModifyVariable(method = "extractRenderState", at = @At("STORE"), ordinal = 0)
    private Rectangle improvedadvancements$tabClip(Rectangle original) {
        return improvedadvancements$arrowFree ? new Rectangle(improvedadvancements$BAR_LEFT,improvedadvancements$BAR_TOP,width-improvedadvancements$BAR_LEFT*2,24) : original;
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void improvedadvancements$fixedTabs(double x,double y,double dx,double dy,CallbackInfoReturnable<Boolean> cir) {
        if (improvedadvancements$arrowFree && tabsBounds != null && tabsBounds.contains(x,y)) cir.setReturnValue(false);
    }
}
