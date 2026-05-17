package com.lxy.hook.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lxy.hook.HookMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class HookConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "hook.json";

    public static HookConfig INSTANCE = new HookConfig();

    // === 距离 ===
    public double maxDistance = 32.0;
    public double minDistance = 2.0;

    // === 拉力强度 ===
    public double blockPullStrength = 3.6;
    public double entityPullStrength = 2.4;

    // === 垂直提升 ===
    public double blockVerticalBoost = 1.35;
    public double entityVerticalBoost = 0.75;

    // === 速度上限 ===
    public double maxPullVelocity = 7.5;
    public double maxHorizontalVelocity = 6.6;
    public double maxVerticalVelocity = 3.6;

    // === 冷却（ticks） ===
    public int blockCooldownTicks = 20;
    public int entityCooldownTicks = 25;
    public int failCooldownTicks = 5;

    // === 耐久 ===
    public int durabilityCost = 1;

    // === 游戏规则 ===
    public boolean allowPullPlayers = false;
    public boolean allowPullBosses = false;
    public boolean reduceFallDamage = true;

    /**
     * 加载配置。如果文件不存在则生成默认配置。
     */
    public static void load() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        File configFile = new File(configDir, FILE_NAME);

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                INSTANCE = GSON.fromJson(reader, HookConfig.class);
                HookMod.LOGGER.info("Loaded config from {}", configFile.getAbsolutePath());
            } catch (IOException e) {
                HookMod.LOGGER.error("Failed to read config, using defaults", e);
                INSTANCE = new HookConfig();
            }
        } else {
            INSTANCE = new HookConfig();
            save();
        }
    }

    /**
     * 保存当前配置到文件。
     */
    public static void save() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        File configFile = new File(configDir, FILE_NAME);
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(INSTANCE, writer);
            HookMod.LOGGER.info("Saved config to {}", configFile.getAbsolutePath());
        } catch (IOException e) {
            HookMod.LOGGER.error("Failed to save config", e);
        }
    }
}
