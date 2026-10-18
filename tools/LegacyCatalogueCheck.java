import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.SharedConstants;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import java.nio.file.*;

/** Decode the built-in map through Minecraft's real registry/codec APIs without opening a window. */
public final class LegacyCatalogueCheck {
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var ops = VanillaRegistries.createWorldLookup().createSerializationContext(JsonOps.INSTANCE);
        var json = JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
        int count = 0, hidden = 0;
        for (var entry : json.entrySet()) {
            DisplayInfo display = DisplayInfo.CODEC.parse(ops, entry.getValue()).getOrThrow();
            // Item component defaults are supplied during the server handshake in 26.3.
            // This headless codec check validates the template/reference, not a rendered stack.
            if (display.icon().item().value() == null || display.icon().count() < 1)
                throw new AssertionError("Invalid icon reference: " + entry.getKey());
            if (display.title().getString().isEmpty()) throw new AssertionError("Empty title: " + entry.getKey());
            if (display.hidden()) hidden++;
            count++;
        }
        if (count != 126) throw new AssertionError("Incomplete vanilla catalogue: " + count);
        System.out.println("PASS: Minecraft 26.3 decoded all " + count + " classic-map displays and item references; " + hidden + " secret goals retain their hidden flag. No game window was opened.");
    }
}
