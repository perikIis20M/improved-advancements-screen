package betteradvancements.fabric;

import betteradvancements.common.platform.IAdvancementVisitor;
import betteradvancements.common.platform.IEventHelper;
import betteradvancements.common.platform.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class FabricPlatformHelper implements IPlatformHelper {
    private final FabricEventHelper eventHelper = new FabricEventHelper();
    private final FabricAdvancementVisitor advancementVisitor = new FabricAdvancementVisitor();

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public IEventHelper getEventHelper() {
        return eventHelper;
    }

    @Override
    public IAdvancementVisitor getAdvancementVisitor() {
        return advancementVisitor;
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void saveConfig() {
        betteradvancements.fabric.config.ConfigFileHandler.writeToConfig();
    }
}
