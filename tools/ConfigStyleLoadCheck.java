import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;

/** Apply real Fabric/Mixin transformations, stopping before Minecraft starts or opens a window. */
public final class ConfigStyleLoadCheck {
    public static void main(String[] args) throws Exception {
        ClassLoader loader = new Knot(EnvType.CLIENT).init(args);
        Class<?> marker = Class.forName("betteradvancements.fabric.config.AdvancementsConfigTabs", false, loader);
        Class<?> screen = Class.forName("me.shedaniel.clothconfig2.gui.ClothConfigScreen", false, loader);
        Class<?> tab = Class.forName("me.shedaniel.clothconfig2.gui.ClothConfigTabButton", false, loader);
        if (!marker.isAssignableFrom(screen)) throw new AssertionError("Screen layout mixin was not applied");
        screen.getDeclaredMethod("improvedadvancements$layoutTabs");
        boolean tabMixin = java.util.Arrays.stream(tab.getDeclaredMethods())
            .anyMatch(method -> method.getName().contains("improvedadvancements$fullHitbox"));
        if (!tabMixin) throw new AssertionError("Tab hitbox mixin was not applied");
        try {
            Class<?> statistics = Class.forName("com.johnfrag.improvedstatisticsscreen.config.StatisticsConfigTabs", false, loader);
            if (!statistics.isAssignableFrom(screen)) throw new AssertionError("Statistics layout marker was lost");
            System.out.println("PASS: Statistics Screen's original layout mixins coexist with the advancement mixins.");
        } catch (ClassNotFoundException absent) {
            // The independent check does not include the Statistics layout fixture.
        }
        System.out.println("PASS: real Fabric startup applied both Cloth Config mixins; Minecraft was not launched.");
    }
}
