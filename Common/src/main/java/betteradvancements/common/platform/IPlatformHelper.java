package betteradvancements.common.platform;

import java.nio.file.Path;

public interface IPlatformHelper {
    String getPlatformName();

    IEventHelper getEventHelper();

    IAdvancementVisitor getAdvancementVisitor();

    default void saveConfig() { }

    default Path getConfigDirectory() {
        return Path.of(System.getProperty("user.dir"), "config");
    }
}
