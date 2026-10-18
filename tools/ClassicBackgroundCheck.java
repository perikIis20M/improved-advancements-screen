import betteradvancements.common.gui.OldScreenTerrain;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.ImageIO;

/** Original GuiAchievements generator fixtures, independent of the port's implementation. */
public final class ClassicBackgroundCheck {
    public static void main(String[] args) throws Exception {
        int[] seeds = {0, 1234, 1428967488};
        String[] expected = {
            "110f4e7835fa000ecec455fb09874671eb72fbf886356a7da5a8e6ad416b2d53",
            "c7aeda5a2d809983f2f08b28be61cdc9a8c7a07c0a22e69f31635d8d693176cf",
            "5cf4bfc822e37e6bff168a213a73831671837d9e747960ef512941fcd66dfa2b"
        };
        for (int i = 0; i < seeds.length; i++) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (int row = 3; row < 50; row++) for (int column = -32; column < 32; column++) {
                digest.update((byte)OldScreenTerrain.tileAt(column, row, seeds[i]));
                digest.update((byte)(OldScreenTerrain.tintAt(row) & 255));
            }
            if (!expected[i].equals(HexFormat.of().formatHex(digest.digest())))
                throw new AssertionError("Port differs from original material/brightness fixture for seed " + seeds[i]);
        }
        for (int column = -128; column < 128; column++) {
            if (OldScreenTerrain.tileAt(column, 0, 1234) != 0)
                throw new AssertionError("Original surface is sand");
            if (OldScreenTerrain.tileAt(column, -10, 1234) != 0)
                throw new AssertionError("Modern negative coordinates safely extend the original surface");
            if (OldScreenTerrain.tileAt(column, 35, 1234) != 7)
                throw new AssertionError("Original bedrock row");
        }
        Path root = Path.of(args[0]);
        String[] names = {"sand", "dirt", "stone", "coal_ore", "iron_ore", "diamond_ore", "redstone_ore", "bedrock"};
        BufferedImage[] tiles = new BufferedImage[names.length];
        Path textures = root.resolve("Common/src/main/resources/assets/betteradvancements/textures/classic");
        for (int i = 0; i < names.length; i++) {
            tiles[i] = ImageIO.read(textures.resolve(names[i] + ".png").toFile());
            if (tiles[i].getWidth() != 16 || tiles[i].getHeight() != 16)
                throw new AssertionError("Original tile dimensions");
        }
        BufferedImage frame = ImageIO.read(textures.resolve("achievement_background.png").toFile());
        if (frame.getWidth() != 256 || frame.getHeight() != 256)
            throw new AssertionError("Original achievement atlas must be 256x256");
        if (Files.mismatch(textures.resolve("achievement_background.png"), root.resolve("build/classic-reference/achievement_background.png")) != -1)
            throw new AssertionError("Original achievement atlas was modified");
        // The original opening contains a translucent vignette, rather than an empty cutout.
        int centerAlpha = frame.getRGB(128, 85) >>> 24;
        if (centerAlpha >= 255 || (frame.getRGB(0, 0) >>> 24) != 0)
            throw new AssertionError("Original map vignette and stepped corners");
        BufferedImage preview = new BufferedImage(512, 202, BufferedImage.TYPE_INT_ARGB);
        for (int panel = 0; panel < 2; panel++) {
            float zoom = panel == 0 ? 1.0F : 2.0F;
            int scrollX = -82, scrollY = -70, row = (scrollY + 288) >> 4, column = (scrollX + 288) >> 4;
            int offsetX = Math.floorMod(scrollX + 288, 16), offsetY = Math.floorMod(scrollY + 288, 16);
            for (int y = 0; y < 155; y++) for (int x = 0; x < 224; x++) {
                int wx = (int)(x * zoom) + offsetX, wy = (int)(y * zoom) + offsetY;
                int tileRow = row + wy / 16;
                int shade = OldScreenTerrain.tintAt(tileRow) & 255;
                int pixel = tiles[OldScreenTerrain.tileAt(column + wx / 16, tileRow, seeds[2])].getRGB(wx % 16, wy % 16);
                int r = ((pixel >> 16) & 255) * shade / 255, g = ((pixel >> 8) & 255) * shade / 255, b = (pixel & 255) * shade / 255;
                preview.setRGB(panel * 256 + 16 + x, 17 + y, 0xFF000000 | r << 16 | g << 8 | b);
            }
            var graphics = preview.createGraphics();
            graphics.drawImage(frame, panel * 256, 0, panel * 256 + 256, 202, 0, 0, 256, 202, null);
            graphics.dispose();
        }
        Path output = root.resolve("build/classic-reference/legacy-frame-preview.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(preview, "png", output.toFile());
        System.out.println("PASS: 9,024 original tile/shade fixture samples, sand surface, bedrock row, original block dimensions and unmodified achievement atlas with vignette/stepped corners.");
    }
}
