package top.damao_zhiling;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DamaoZlMod implements ModInitializer {
	public static final String MOD_ID = "damao";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Damao Mod (not Da Mao) initialized!");
	}
}