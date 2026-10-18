package betteradvancements.common.gui;

import java.util.Random;

/** Pre-1.12 GuiAchievements material/brightness rules, without terrain rebalancing. */
public final class OldScreenTerrain {
    public static final int TILE_SIZE = 16;
    public static final int COORDINATE_OFFSET = 288;

    private OldScreenTerrain() { }

    public static int tileAt(int column, int row, int playerSeed) {
        // Modern maps can extend above the old legal coordinates; extend the original sand surface.
        // Original integer seed arithmetic and Random consumption are retained for all legacy rows.
        if (row < 0) return 0;
        Random random = new Random((long) (playerSeed + column + row * 16));
        int material = random.nextInt(row + 1) + row / 2;
        if (row == 35 || material > 37) return 7;
        if (material == 22) return random.nextInt(2) == 0 ? 5 : 6;
        if (material == 10) return 4;
        if (material == 8) return 3;
        if (material > 4) return 2;
        if (material > 0) return 1;
        return 0;
    }

    public static int tintAt(int row) {
        float brightness = Math.max(0, Math.min(1, 0.6F - row / 25.0F * 0.3F));
        int shade = (int) (255 * brightness);
        return 0xFF000000 | shade << 16 | shade << 8 | shade;
    }
}
