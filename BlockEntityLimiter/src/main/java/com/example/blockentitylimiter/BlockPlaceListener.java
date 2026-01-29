package com.yourname.blockentitylimiter;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class BlockPlaceListener implements Listener {
    
    private final BlockEntityLimiter plugin;
    
    public BlockPlaceListener(BlockEntityLimiter plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Material material = block.getType();
        Player player = event.getPlayer();
        
        // Check if this is a tracked block entity
        if (!plugin.getConfigManager().isTrackedBlockEntity(material)) {
            return;
        }
        
        // Check if limiting is enabled for this block entity
        if (!plugin.getConfigManager().isBlockEnabled(material)) {
            return;
        }
        
        Chunk chunk = block.getChunk();
        
        // Check synchronously (events are already on the correct region thread in Folia)
        if (!plugin.getBlockEntityCounter().canPlaceBlockEntity(chunk, material)) {
            event.setCancelled(true);
            
            // Send message to player
            Component message = Component.text("Cannot place ")
                .color(NamedTextColor.RED)
                .append(Component.text(getMaterialName(material)))
                .append(Component.text(": Block entity limit reached in this chunk!"));
            
            player.sendMessage(message);
            
            // Even though placement was blocked, check if chunk should be forceloaded
            // (it might already have enough block entities from before plugin was added)
            plugin.getServer().getRegionScheduler().runDelayed(plugin, event.getBlock().getLocation(), (task) -> {
                plugin.getForceloadManager().checkAndForceload(chunk, player);
            }, 1L);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlaceSuccess(BlockPlaceEvent event) {
        Material material = event.getBlock().getType();
        
        // Only process if a tracked block entity was placed
        if (plugin.getConfigManager().isTrackedBlockEntity(material)) {
            Chunk chunk = event.getBlock().getChunk();
            Player player = event.getPlayer();
            
            // Invalidate cache immediately
            plugin.getBlockEntityCounter().invalidateChunk(chunk);
            
            // Schedule forceload check on next tick to ensure block entity is fully registered
            plugin.getServer().getRegionScheduler().runDelayed(plugin, event.getBlock().getLocation(), (task) -> {
                plugin.getForceloadManager().checkAndForceload(chunk, player);
            }, 1L);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Material material = event.getBlock().getType();
        
        // Invalidate cache when a tracked block entity is broken
        if (plugin.getConfigManager().isTrackedBlockEntity(material)) {
            Chunk chunk = event.getBlock().getChunk();
            plugin.getBlockEntityCounter().invalidateChunk(chunk);
            
            // Check if chunk should be unforceloaded
            plugin.getForceloadManager().unforceloadChunk(chunk);
        }
    }
    
    private String getMaterialName(Material material) {
        String name = material.name().toLowerCase().replace('_', ' ');
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        
        return result.toString().trim();
    }
}