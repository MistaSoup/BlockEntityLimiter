package com.yourname.blockentitylimiter;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BlockEntityCounter {
    
    private final BlockEntityLimiter plugin;
    
    // Cache for chunk counts - uses chunk key (world:x:z) as key
    private final Map<String, ChunkCache> chunkCache;
    
    // Cache duration in milliseconds (500ms = half a second)
    private static final long CACHE_DURATION = 500;
    
    public BlockEntityCounter(BlockEntityLimiter plugin) {
        this.plugin = plugin;
        this.chunkCache = new ConcurrentHashMap<>();
    }
    
    private static class ChunkCache {
        final Map<Material, Integer> counts;
        final long timestamp;
        
        ChunkCache(Map<Material, Integer> counts, long timestamp) {
            this.counts = counts;
            this.timestamp = timestamp;
        }
        
        boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_DURATION;
        }
    }
    
    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }
    
    public Map<Material, Integer> countBlockEntitiesInChunk(Chunk chunk) {
        String key = getChunkKey(chunk);
        
        // Check cache first
        ChunkCache cached = chunkCache.get(key);
        if (cached != null && cached.isValid()) {
            return new HashMap<>(cached.counts);
        }
        
        // Count from scratch if cache is invalid or missing
        Map<Material, Integer> counts = new HashMap<>();
        ConfigManager configManager = plugin.getConfigManager();
        
        // Get all tile entities (block entities) in the chunk
        BlockState[] tileEntities = chunk.getTileEntities();
        
        for (BlockState blockState : tileEntities) {
            Material material = blockState.getType();
            
            // Only count tracked block entities
            if (configManager.isTrackedBlockEntity(material)) {
                counts.put(material, counts.getOrDefault(material, 0) + 1);
            }
        }
        
        // Update cache
        chunkCache.put(key, new ChunkCache(counts, System.currentTimeMillis()));
        
        // Clean old cache entries periodically (every 100 accesses)
        if (chunkCache.size() > 1000) {
            cleanCache();
        }
        
        return counts;
    }
    
    public int getTotalBlockEntitiesInChunk(Chunk chunk) {
        Map<Material, Integer> counts = countBlockEntitiesInChunk(chunk);
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    public boolean canPlaceBlockEntity(Chunk chunk, Material material) {
        ConfigManager configManager = plugin.getConfigManager();
        
        // Check if this block entity type is tracked
        if (!configManager.isTrackedBlockEntity(material)) {
            return true;
        }
        
        // Check if this block entity type is enabled for limiting
        if (!configManager.isBlockEnabled(material)) {
            return true;
        }
        
        Map<Material, Integer> counts = countBlockEntitiesInChunk(chunk);
        
        // Check master limit if enabled
        if (configManager.isUseMasterLimit()) {
            int totalCount = counts.values().stream().mapToInt(Integer::intValue).sum();
            if (totalCount >= configManager.getMasterLimit()) {
                return false;
            }
        }
        
        // Check individual block entity limit
        int currentCount = counts.getOrDefault(material, 0);
        return currentCount < configManager.getBlockLimit(material);
    }
    
    // Invalidate cache for a specific chunk (called when a block is placed)
    public void invalidateChunk(Chunk chunk) {
        String key = getChunkKey(chunk);
        chunkCache.remove(key);
    }
    
    // Clean expired cache entries
    private void cleanCache() {
        long now = System.currentTimeMillis();
        chunkCache.entrySet().removeIf(entry -> 
            now - entry.getValue().timestamp > CACHE_DURATION * 2
        );
    }
    
    // Clear all cache (useful for reload)
    public void clearCache() {
        chunkCache.clear();
    }
}