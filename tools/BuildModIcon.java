import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Builds a small pixel-art achievement tree using Minecraft's classic item artwork. */
public final class BuildModIcon {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        Path reference = root.resolve("build/classic-reference");
        BufferedImage icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setColor(new Color(0x212121));
        g.fillRect(4, 2, 56, 60);
        g.fillRect(2, 4, 60, 56);
        g.setColor(new Color(0xC6C6C6));
        g.fillRect(4, 3, 55, 56);
        g.fillRect(3, 4, 56, 55);
        g.setColor(Color.WHITE);
        g.fillRect(4, 3, 54, 1);
        g.fillRect(3, 4, 1, 54);
        g.setClip(6, 10, 52, 47);
        BufferedImage stone = ImageIO.read(root.resolve("Common/src/main/resources/assets/betteradvancements/textures/classic/stone.png").toFile());
        for (int y = 10; y < 57; y += 16) for (int x = 6; x < 58; x += 16) g.drawImage(stone, x, y, null);
        g.setColor(new Color(0, 0, 0, 155));
        g.fillRect(6, 10, 52, 47);
        g.setClip(null);
        g.setColor(new Color(0x151515));
        g.fillRect(30, 30, 4, 19);
        g.fillRect(15, 42, 34, 4);
        g.setColor(new Color(0xD5D5D5));
        g.fillRect(31, 30, 2, 15);
        g.fillRect(16, 43, 32, 2);
        frame(g, 21, 13, 22, true);
        frame(g, 8, 39, 18, false);
        frame(g, 38, 39, 18, true);
        g.drawImage(ImageIO.read(reference.resolve("diamond.png").toFile()), 24, 16, 16, 16, null);
        g.drawImage(ImageIO.read(reference.resolve("book.png").toFile()), 11, 42, 12, 12, null);
        g.drawImage(ImageIO.read(reference.resolve("iron_pickaxe.png").toFile()), 41, 42, 12, 12, null);
        g.setColor(new Color(0x545454));
        g.fillRect(6, 6, 13, 2);
        g.fillRect(22, 6, 7, 2);
        g.dispose();
        BufferedImage output = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D scaled = output.createGraphics();
        scaled.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        scaled.drawImage(icon, 0, 0, 128, 128, null);
        scaled.dispose();
        ImageIO.write(output, "png", root.resolve("Fabric/src/main/resources/icon.png").toFile());
    }

    private static void frame(Graphics2D g, int x, int y, int size, boolean completed) {
        g.setColor(Color.BLACK);
        g.fillRect(x + 1, y, size - 2, size);
        g.fillRect(x, y + 1, size, size - 2);
        g.setColor(new Color(completed ? 0xA47D0F : 0x919191));
        g.fillRect(x + 2, y + 2, size - 4, size - 4);
        g.setColor(new Color(completed ? 0xEAC044 : 0xE9E9E9));
        g.fillRect(x + 1, y + 1, size - 2, 1);
        g.fillRect(x + 1, y + 2, 1, size - 3);
        g.setColor(new Color(completed ? 0x665008 : 0x555555));
        g.fillRect(x + 2, y + size - 2, size - 3, 1);
        g.fillRect(x + size - 2, y + 2, 1, size - 3);
    }
}
