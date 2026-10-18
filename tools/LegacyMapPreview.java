import betteradvancements.common.gui.OldScreenTerrain;
import com.google.gson.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;

/** Offline renderer of the exact authored geometry; item textures here are flat preview icons. */
public final class LegacyMapPreview {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        var data = JsonParser.parseString(Files.readString(root.resolve("build/legacy-layout-check/map-data.json"))).getAsJsonObject();
        var positions = data.getAsJsonObject("positions");
        var routes = data.getAsJsonObject("routes");
        var displays = JsonParser.parseString(Files.readString(root.resolve("Common/src/main/resources/assets/betteradvancements/old_screen_titles.json"))).getAsJsonObject();
        int minX = 0, maxX = 0, minY = 0, maxY = 0;
        for (var value : positions.entrySet()) {
            var p = value.getValue().getAsJsonObject();
            int x = p.get("x").getAsInt(), y = p.get("y").getAsInt();
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
        int originX = minX - 48, originY = minY - 48;
        int width = maxX - originX + 72, height = maxY - originY + 72;
        var result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Path textures = root.resolve("Common/src/main/resources/assets/betteradvancements/textures/classic");
        String[] names = {"sand", "dirt", "stone", "coal_ore", "iron_ore", "diamond_ore", "redstone_ore", "bedrock"};
        BufferedImage[] tiles = new BufferedImage[names.length];
        for (int i = 0; i < names.length; i++) tiles[i] = ImageIO.read(textures.resolve(names[i] + ".png").toFile());
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            int wx = x + originX + 288, wy = y + originY + 288;
            int column = Math.floorDiv(wx, 16), row = Math.floorDiv(wy, 16);
            int pixel = tiles[OldScreenTerrain.tileAt(column, row, 1234)].getRGB(Math.floorMod(wx, 16), Math.floorMod(wy, 16));
            int shade = OldScreenTerrain.tintAt(row) & 255;
            result.setRGB(x, y, 0xff000000 | ((pixel >> 16) & 255) * shade / 255 << 16
                | ((pixel >> 8) & 255) * shade / 255 << 8 | (pixel & 255) * shade / 255);
        }
        var g = result.createGraphics();
        g.setColor(new Color(160, 160, 160));
        for (var route : routes.entrySet()) {
            var path = route.getValue().getAsJsonArray();
            for (int i = 1; i < path.size(); i++) {
                var a = path.get(i-1).getAsJsonObject(); var b = path.get(i).getAsJsonObject();
                g.drawLine(a.get("x").getAsInt() - originX + 11, a.get("y").getAsInt() - originY + 11,
                    b.get("x").getAsInt() - originX + 11, b.get("y").getAsInt() - originY + 11);
            }
        }
        var frame = ImageIO.read(textures.resolve("achievement_background.png").toFile());
        try (var jar = new ZipFile(args[1])) {
            for (var node : positions.entrySet()) {
                String id = node.getKey();
                var display = displays.getAsJsonObject(id);
                var p = node.getValue().getAsJsonObject();
                int x = p.get("x").getAsInt() - originX, y = p.get("y").getAsInt() - originY;
                int u = display.has("frame") && display.get("frame").getAsString().equals("challenge") ? 26 : 0;
                g.drawImage(frame, x - 2, y - 2, x + 24, y + 24, u, 202, u + 26, 228, null);
                String item = id.equals("minecraft:story/root") ? "book" : display.getAsJsonObject("icon").get("id").getAsString().substring(10);
                BufferedImage icon = null;
                for (String name : new String[]{"item/" + item, "block/" + item, "block/" + item + "_front", "block/" + item + "_side", "block/" + item + "_top"}) {
                    var entry = jar.getEntry("assets/minecraft/textures/" + name + ".png");
                    if (entry != null) { try (var input = jar.getInputStream(entry)) { icon = ImageIO.read(input); } break; }
                }
                if (icon != null) g.drawImage(icon, x + 3, y + 3, x + 19, y + 19, 0, 0, icon.getWidth(), Math.min(icon.getWidth(), icon.getHeight()), null);
                else {
                    g.setColor(new Color(40, 40, 40));
                    g.setFont(new Font("SansSerif", Font.PLAIN, 8));
                    g.drawString(item.substring(0, Math.min(3, item.length())), x + 3, y + 14);
                }
                var path = routes.getAsJsonArray(id);
                if (path != null && path.size() >= 2) {
                    var next = path.get(1).getAsJsonObject();
                    int dx = next.get("x").getAsInt() - p.get("x").getAsInt(), dy = next.get("y").getAsInt() - p.get("y").getAsInt();
                    int au, av, ax, ay, aw, ah;
                    if (dx < 0) { au=114; av=234; ax=x-7; ay=y+6; aw=7; ah=11; }
                    else if (dx > 0) { au=107; av=234; ax=x+22; ay=y+6; aw=7; ah=11; }
                    else if (dy < 0) { au=96; av=234; ax=x+6; ay=y-7; aw=11; ah=7; }
                    else { au=96; av=241; ax=x+6; ay=y+22; aw=11; ah=7; }
                    g.drawImage(frame, ax, ay, ax+aw, ay+ah, au, av, au+aw, av+ah, null);
                }
            }
        }
        g.dispose();
        Path output = root.resolve("output/old-screen-map-preview.png");
        ImageIO.write(result, "png", output.toFile());
        System.out.println("Offline map geometry preview: " + output + " (" + width + " x " + height + ")");
    }
}
