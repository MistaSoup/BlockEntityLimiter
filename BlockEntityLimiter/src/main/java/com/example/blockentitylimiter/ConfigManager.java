package com.yourname.blockentitylimiter;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    
    private final BlockEntityLimiter plugin;
    private FileConfiguration config;
    
    private boolean useMasterLimit;
    private int masterLimit;
    private Map<Material, Integer> blockLimits;
    private Map<Material, Boolean> blockEnabled;
    
    // Forceload settings
    private boolean forceloadEnabled;
    private int forceloadThreshold;
    private boolean notifyPlayer;
    
    // Periodic check settings
    private boolean periodicCheckEnabled;
    private int periodicCheckInterval;
    private int delayPerChunk;
    private boolean verboseLogging;
    
    public ConfigManager(BlockEntityLimiter plugin) {
        this.plugin = plugin;
        this.blockLimits = new HashMap<>();
        this.blockEnabled = new HashMap<>();
    }
    
    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        
        // Load master limit settings
        useMasterLimit = config.getBoolean("master-limit.enabled", true);
        masterLimit = config.getInt("master-limit.amount", 1000);
        
        // Load forceload settings
        forceloadEnabled = config.getBoolean("forceload.enabled", false);
        forceloadThreshold = config.getInt("forceload.threshold", 50);
        notifyPlayer = config.getBoolean("forceload.notify-player", true);
        
        // Load periodic check settings
        periodicCheckEnabled = config.getBoolean("forceload.periodic-check.enabled", true);
        periodicCheckInterval = config.getInt("forceload.periodic-check.interval", 30);
        delayPerChunk = config.getInt("forceload.periodic-check.delay-per-chunk", 20);
        verboseLogging = config.getBoolean("forceload.periodic-check.verbose", false);
        
        // Load individual block entity limits
        loadBlockEntityConfig("decorated-pot", Material.DECORATED_POT);
        loadBlockEntityConfig("ender-chest", Material.ENDER_CHEST);
        loadBlockEntityConfig("blast-furnace", Material.BLAST_FURNACE);
        loadBlockEntityConfig("smoker", Material.SMOKER);
        loadBlockEntityConfig("furnace", Material.FURNACE);
        loadBlockEntityConfig("barrel", Material.BARREL);
        loadBlockEntityConfig("dropper", Material.DROPPER);
        loadBlockEntityConfig("dispenser", Material.DISPENSER);
        loadBlockEntityConfig("chest", Material.CHEST);
        loadBlockEntityConfig("trapped-chest", Material.TRAPPED_CHEST);
        loadBlockEntityConfig("hopper", Material.HOPPER);
        
        // Load all shulker box types under one config
        loadShulkerBoxConfig();
    }
    
    private void loadBlockEntityConfig(String configKey, Material material) {
        boolean enabled = config.getBoolean("block-entities." + configKey + ".enabled", true);
        int limit = config.getInt("block-entities." + configKey + ".limit", 100);
        
        blockEnabled.put(material, enabled);
        blockLimits.put(material, limit);
    }
    
    private void loadShulkerBoxConfig() {
        boolean enabled = config.getBoolean("block-entities.shulker-box.enabled", true);
        int limit = config.getInt("block-entities.shulker-box.limit", 100);
        
        // All shulker box colors
        Material[] shulkerBoxes = {
            Material.SHULKER_BOX,
            Material.WHITE_SHULKER_BOX,
            Material.ORANGE_SHULKER_BOX,
            Material.MAGENTA_SHULKER_BOX,
            Material.LIGHT_BLUE_SHULKER_BOX,
            Material.YELLOW_SHULKER_BOX,
            Material.LIME_SHULKER_BOX,
            Material.PINK_SHULKER_BOX,
            Material.GRAY_SHULKER_BOX,
            Material.LIGHT_GRAY_SHULKER_BOX,
            Material.CYAN_SHULKER_BOX,
            Material.PURPLE_SHULKER_BOX,
            Material.BLUE_SHULKER_BOX,
            Material.BROWN_SHULKER_BOX,
            Material.GREEN_SHULKER_BOX,
            Material.RED_SHULKER_BOX,
            Material.BLACK_SHULKER_BOX
        };
        
        for (Material shulkerBox : shulkerBoxes) {
            blockEnabled.put(shulkerBox, enabled);
            blockLimits.put(shulkerBox, limit);
        }
    }
    
    public boolean isUseMasterLimit() {
        return useMasterLimit;
    }
    
    public int getMasterLimit() {
        return masterLimit;
    }
    
    public int getBlockLimit(Material material) {
        return blockLimits.getOrDefault(material, Integer.MAX_VALUE);
    }
    
    public boolean isBlockEnabled(Material material) {
        return blockEnabled.getOrDefault(material, false);
    }
    
    public boolean isTrackedBlockEntity(Material material) {
        return blockEnabled.containsKey(material);
    }
    
    public boolean isForceloadEnabled() {
        return forceloadEnabled;
    }
    
    public int getForceloadThreshold() {
        return forceloadThreshold;
    }
    
    public boolean shouldNotifyPlayer() {
        return notifyPlayer;
    }
    
    public boolean isPeriodicCheckEnabled() {
        return periodicCheckEnabled;
    }
    
    public int getPeriodicCheckInterval() {
        return periodicCheckInterval;
    }
    
    public int getDelayPerChunk() {
        return delayPerChunk;
    }
    
    public boolean isVerboseLogging() {
        return verboseLogging;
    }
}