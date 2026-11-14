package betteradvancements.fabric.handler;

import betteradvancements.common.gui.BetterAdvancementsScreen;
import betteradvancements.common.gui.BetterAdvancementsScreenButton;
import betteradvancements.common.gui.LegacyAchievementsScreen;
import betteradvancements.common.gui.NavigationTabLayout;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import java.util.ArrayList;

public final class GuiOpenHandler implements ScreenEvents.AfterInit {
    public static final GuiOpenHandler instance = new GuiOpenHandler();
    private GuiOpenHandler() { }

    public void registerEventHandlers() { ScreenEvents.AFTER_INIT.register(this); }

    @Override public void afterInit(Minecraft minecraft, Screen screen, int width, int height) {
        boolean inventory = screen instanceof InventoryScreen;
        if (!(inventory || screen instanceof BetterAdvancementsScreen || screen instanceof LegacyAchievementsScreen)
            || !BetterAdvancementsScreenButton.isAvailable()) return;
        var widgets = Screens.getWidgets(screen);
        if (widgets.stream().noneMatch(BetterAdvancementsScreenButton.class::isInstance)) {
            var button = new BetterAdvancementsScreenButton(!inventory);
            button.visible = false; // Positioned before extraction, after all mods have initialized their widgets.
            widgets.add(button);
        }
        // Fabric recreates per-screen events during every init, including a resize.
        ScreenEvents.beforeExtract(screen).register((current, graphics, x, y, delta) -> position(current));
        position(screen);
    }

    private void position(Screen screen) {
        var widgets = Screens.getWidgets(screen);
        NavigationTabLayout.Rect frame;
        if (screen instanceof InventoryScreen inventory) {
            // leftPos may move when the recipe book opens; read it each frame.
            frame = new NavigationTabLayout.Rect(inventory.leftPos, inventory.topPos, inventory.imageWidth, 166);
        } else if (screen instanceof BetterAdvancementsScreen modern) frame = modern.navigationFrame();
        else if (screen instanceof LegacyAchievementsScreen classic) frame = classic.navigationFrame();
        else return;
        var occupied = new ArrayList<NavigationTabLayout.Rect>();
        if (screen instanceof BetterAdvancementsScreen modern) occupied.addAll(modern.navigationObstacles());
        for (AbstractWidget widget : widgets) {
            if (widget.visible && !(widget instanceof BetterAdvancementsScreenButton))
                occupied.add(new NavigationTabLayout.Rect(widget.getX(), widget.getY(), widget.getWidth(), widget.getHeight()));
        }
        var slot = NavigationTabLayout.find(frame, screen.width, screen.height, occupied);
        for (AbstractWidget widget : widgets) if (widget instanceof BetterAdvancementsScreenButton) {
            widget.visible = widget.active = slot != null && BetterAdvancementsScreenButton.isAvailable();
            if (slot != null) widget.setPosition(slot.x(), slot.y());
        }
    }
}
