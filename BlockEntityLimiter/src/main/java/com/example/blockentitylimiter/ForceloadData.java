package com.yourname.blockentitylimiter;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForceloadData {
    
    private final BlockEntityLimiter plugin;
    private File dataFile;
    private FileConfiguration dataConfig;
    
    public ForceloadData(BlockEntityLimiter plugin) {
        this.plugin = plugin;
        loadDataFile();
    }
    
    private void loadDataFile() {
        dataFile = new File(plugin.getDataFolder(), "forceloaded-chunks.yml");
        
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create forceloaded-chunks.yml!");
                e.printStackTrace();
            }
        }
        
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }
    
    public Set<String> loadForceloadedChunks() {
        Set<String> chunks = new HashSet<>();
        
        List<String> chunkList = dataConfig.getStringList("forceloaded-chunks");
        if (chunkList != null) {
            chunks.addAll(chunkList);
        }
        
        return chunks;
    }
    
    public void saveForceloadedChunks(Set<String> chunks) {
        dataConfig.set("forceloaded-chunks", new ArrayList<>(chunks));
        
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save forceloaded-chunks.yml!");
            e.printStackTrace();
        }
    }
    
    public void reload() {
        loadDataFile();
    }
}