package com.yourname.blockentitylimiter;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BlockEntityLimiter extends JavaPlugin {
    
    private ConfigManager configManager;
    private BlockEntityCounter blockEntityCounter;
    private ForceloadManager forceloadManager;
    
    @Override
    public void onEnable() {
        // Initialize config manager
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize block entity counter
        blockEntityCounter = new BlockEntityCounter(this);
        
        // Initialize forceload manager
        forceloadManager = new ForceloadManager(this);
        
        // Register event listener
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(this), this);
        
        // Register command
        getCommand("belimiter").setExecutor(new BELimiterCommand(this));
        
        getLogger().info("BlockEntityLimiter has been enabled!");
    }
    
    @Override
    public void onDisable() {
        // Save forceloaded chunks before shutdown
        if (forceloadManager != null) {
            forceloadManager.shutdown();
        }
        getLogger().info("BlockEntityLimiter has been disabled!");
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public BlockEntityCounter getBlockEntityCounter() {
        return blockEntityCounter;
    }
    
    public ForceloadManager getForceloadManager() {
        return forceloadManager;
    }
}