package betteradvancements.common.gui;

import betteradvancements.common.reference.Resources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** A normal accessible button with the advancement-tab frame and a diamond item icon. */
public final class BetterAdvancementsScreenButton extends Button {
    public static boolean addToInventory = true;

    public BetterAdvancementsScreenButton(boolean toInventory) {
        super(0, 0, NavigationTabLayout.WIDTH, NavigationTabLayout.HEIGHT,
            Component.translatable(toInventory ? "container.inventory" : "gui.advancements"), button -> {
                Minecraft mc = Minecraft.getInstance();
                if (!isAvailable()) return;
                mc.setScreenAndShow(toInventory ? new InventoryScreen(mc.player)
                    : BetterAdvancementsScreen.create(mc.player.connection.getAdvancements(), mc.gui.screen()));
            }, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(getMessage()));
    }

    public static boolean isAvailable() {
        Minecraft mc = Minecraft.getInstance();
        return addToInventory && mc != null && mc.player != null && mc.gameMode != null
            && mc.gameMode.getPlayerMode() == GameType.SURVIVAL;
    }

    @Override protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, Resources.Gui.TABS, getX(), getY(), 56, 0, 28, 32, 256, 256);
        if (isHoveredOrFocused()) graphics.outline(getX() + 2, getY() + 2, getWidth() - 4, getHeight() - 4, 0xFFFFFFFF);
        graphics.fakeItem(new ItemStack(Items.DIAMOND), getX() + 6, getY() + 9);
    }
}
