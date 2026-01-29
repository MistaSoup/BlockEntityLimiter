package com.yourname.blockentitylimiter;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ForceloadManager {
    
    private final BlockEntityLimiter plugin;
    private final ForceloadData dataStorage;
    
    // Track which chunks we've forceloaded
    private final Set<String> forceloadedChunks;
    
    public ForceloadManager(BlockEntityLimiter plugin) {
        this.plugin = plugin;
        this.dataStorage = new ForceloadData(plugin);
        this.forceloadedChunks = ConcurrentHashMap.newKeySet();
        
        // Load saved forceloaded chunks
        loadForceloadedChunks();
        
        // Start periodic checker
        startPeriodicChecker();
    }
    
    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
    
    private void loadForceloadedChunks() {
        Set<String> savedChunks = dataStorage.loadForceloadedChunks();
        
        if (savedChunks.isEmpty()) {
            return;
        }
        
        plugin.getLogger().info("Loading " + savedChunks.size() + " forceloaded chunks from storage...");
        
        // Re-apply forceload to all saved chunks
        for (String chunkKey : savedChunks) {
            String[] parts = chunkKey.split(":");
            if (parts.length != 3) continue;
            
            String worldName = parts[0];
            int chunkX = Integer.parseInt(parts[1]);
            int chunkZ = Integer.parseInt(parts[2]);
            
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World " + worldName + " not found for chunk " + chunkKey);
                continue;
            }
            
            // Schedule on the region thread for this chunk, then forceload on global thread
            plugin.getServer().getRegionScheduler().run(plugin, world, chunkX, chunkZ, (task) -> {
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);
                
                // Now forceload on global region thread
                plugin.getServer().getGlobalRegionScheduler().run(plugin, (globalTask) -> {
                    chunk.setForceLoaded(true);
                    forceloadedChunks.add(chunkKey);
                    
                    if (plugin.getConfigManager().isVerboseLogging()) {
                        plugin.getLogger().info("Restored forceload for chunk " + chunkKey);
                    }
                });
            });
        }
    }
    
    private void saveForceloadedChunks() {
        dataStorage.saveForceloadedChunks(forceloadedChunks);
    }
    
    public void checkAndForceload(Chunk chunk, Player player) {
        ConfigManager config = plugin.getConfigManager();
        
        // Check if forceload is enabled
        if (!config.isForceloadEnabled()) {
            return;
        }
        
        String chunkKey = getChunkKey(chunk);
        
        // Skip if already forceloaded by this plugin
        if (forceloadedChunks.contains(chunkKey)) {
            return;
        }
        
        // Count block entities in chunk
        int totalBlockEntities = plugin.getBlockEntityCounter().getTotalBlockEntitiesInChunk(chunk);
        
        // Check if threshold is met
        if (totalBlockEntities >= config.getForceloadThreshold()) {
            // Schedule forceload on global region thread (required for Folia)
            plugin.getServer().getGlobalRegionScheduler().run(plugin, (task) -> {
                // Forceload the chunk
                chunk.setForceLoaded(true);
                forceloadedChunks.add(chunkKey);
                saveForceloadedChunks();
                
                // Notify player if enabled (schedule on player's thread)
                if (config.shouldNotifyPlayer() && player != null) {
                    player.getScheduler().run(plugin, (playerTask) -> {
                        Component message = Component.text("Chunk forceloaded: ")
                            .color(NamedTextColor.GOLD)
                            .append(Component.text("This chunk has " + totalBlockEntities + " block entities and will remain loaded.")
                                .color(NamedTextColor.YELLOW));
                        player.sendMessage(message);
                    }, null);
                }
                
                // Log to console (always log new forceloads)
                plugin.getLogger().info("Forceloaded chunk " + chunkKey + " with " + totalBlockEntities + " block entities");
            });
        }
    }
    
    public void unforceloadChunk(Chunk chunk) {
        ConfigManager config = plugin.getConfigManager();
        
        if (!config.isForceloadEnabled()) {
            return;
        }
        
        String chunkKey = getChunkKey(chunk);
        
        // Only unforceload if we forceloaded it and it's below threshold
        if (forceloadedChunks.contains(chunkKey)) {
            int totalBlockEntities = plugin.getBlockEntityCounter().getTotalBlockEntitiesInChunk(chunk);
            
            if (totalBlockEntities < config.getForceloadThreshold()) {
                // Schedule unforceload on global region thread (required for Folia)
                plugin.getServer().getGlobalRegionScheduler().run(plugin, (task) -> {
                    chunk.setForceLoaded(false);
                    forceloadedChunks.remove(chunkKey);
                    saveForceloadedChunks();
                    
                    // Always log when removing forceload
                    plugin.getLogger().info("Removed forceload from chunk " + chunkKey + " (now has " + totalBlockEntities + " block entities)");
                });
            }
        }
    }
    
    private void startPeriodicChecker() {
        ConfigManager config = plugin.getConfigManager();
        
        if (!config.isPeriodicCheckEnabled()) {
            return;
        }
        
        // Convert minutes to seconds for TimeUnit
        long intervalSeconds = config.getPeriodicCheckInterval() * 60L;
        
        // Schedule repeating task on async scheduler
        plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, (task) -> {
            if (!config.isForceloadEnabled() || forceloadedChunks.isEmpty()) {
                return;
            }
            
            if (config.isVerboseLogging()) {
                plugin.getLogger().info("Starting periodic forceload check for " + forceloadedChunks.size() + " chunks...");
            }
            
            // Create a copy to avoid concurrent modification
            List<String> chunksToCheck = new ArrayList<>(forceloadedChunks);
            
            // Check each chunk with delay
            checkChunksWithDelay(chunksToCheck, 0);
            
        }, 5, intervalSeconds, TimeUnit.SECONDS);
    }
    
    private void checkChunksWithDelay(List<String> chunks, int index) {
        ConfigManager config = plugin.getConfigManager();
        
        if (index >= chunks.size()) {
            if (config.isVerboseLogging()) {
                plugin.getLogger().info("Periodic forceload check complete.");
            }
            return;
        }
        
        String chunkKey = chunks.get(index);
        String[] parts = chunkKey.split(":");
        
        if (parts.length != 3) {
            // Invalid format, skip
            checkChunksWithDelay(chunks, index + 1);
            return;
        }
        
        String worldName = parts[0];
        int chunkX;
        int chunkZ;
        
        try {
            chunkX = Integer.parseInt(parts[1]);
            chunkZ = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            // Invalid coordinates, skip
            checkChunksWithDelay(chunks, index + 1);
            return;
        }
        
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            // World doesn't exist anymore, remove from list
            forceloadedChunks.remove(chunkKey);
            saveForceloadedChunks();
            plugin.getLogger().warning("Removed " + chunkKey + " - world no longer exists");
            checkChunksWithDelay(chunks, index + 1);
            return;
        }
        
        // Schedule check on region thread for this chunk
        plugin.getServer().getRegionScheduler().run(plugin, world, chunkX, chunkZ, (task) -> {
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            
            // Count block entities
            int totalBlockEntities = plugin.getBlockEntityCounter().getTotalBlockEntitiesInChunk(chunk);
            
            if (config.isVerboseLogging()) {
                plugin.getLogger().info("Checking chunk " + chunkKey + ": " + totalBlockEntities + " block entities (threshold: " + config.getForceloadThreshold() + ")");
            }
            
            // Check if below threshold
            if (totalBlockEntities < config.getForceloadThreshold()) {
                plugin.getLogger().info("Chunk " + chunkKey + " below threshold (" + totalBlockEntities + " < " + config.getForceloadThreshold() + "), removing forceload");
                
                // Unforceload on global thread
                plugin.getServer().getGlobalRegionScheduler().run(plugin, (globalTask) -> {
                    chunk.setForceLoaded(false);
                    forceloadedChunks.remove(chunkKey);
                    saveForceloadedChunks();
                });
            }
            
            // Schedule next chunk check with delay on global thread
            long delayTicks = config.getDelayPerChunk();
            plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, (delayTask) -> {
                checkChunksWithDelay(chunks, index + 1);
            }, delayTicks);
        });
    }
    
    public boolean isForceloadedByPlugin(Chunk chunk) {
        return forceloadedChunks.contains(getChunkKey(chunk));
    }
    
    public void clearForceloads() {
        forceloadedChunks.clear();
        saveForceloadedChunks();
    }
    
    public Set<String> getForceloadedChunks() {
        return new HashSet<>(forceloadedChunks);
    }
    
    public void shutdown() {
        saveForceloadedChunks();
    }
}