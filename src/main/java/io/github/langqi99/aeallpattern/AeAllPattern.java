package io.github.langqi99.aeallpattern;

import com.mojang.logging.LogUtils;
import io.github.langqi99.aeallpattern.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AeAllPattern.MOD_ID)
public final class AeAllPattern {
    public static final String MOD_ID = "aeallpattern";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AeAllPattern(IEventBus modBus) {
        ModItems.register(modBus);
        LOGGER.info("AE All Pattern initialized (documentation-first prototype)");
    }
}
