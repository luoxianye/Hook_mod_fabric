package com.lxy.hook;

import com.lxy.hook.config.HookConfig;
import com.lxy.hook.entity.ModEntityTypes;
import com.lxy.hook.item.ModItems;
import com.lxy.hook.util.PlayerPullManager;
import net.fabricmc.api.ModInitializer;
import com.lxy.hook.network.ModNetwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HookMod implements ModInitializer {
	public static final String MOD_ID = "hook";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		HookConfig.load();
		ModEntityTypes.initialize();
		ModItems.initialize();
		PlayerPullManager.initialize();
		ModNetwork.initialize();
		LOGGER.info("Hook Mod initialized!");
	}
}