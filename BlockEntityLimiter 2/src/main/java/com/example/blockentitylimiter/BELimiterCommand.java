package com.yourname.blockentitylimiter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BELimiterCommand implements CommandExecutor, TabCompleter {
    
    private final BlockEntityLimiter plugin;
    
    public BELimiterCommand(BlockEntityLimiter plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("blockentitylimiter.reload")) {
                sender.sendMessage(Component.text("You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            plugin.getConfigManager().loadConfig();
            plugin.getBlockEntityCounter().clearCache();
            sender.sendMessage(Component.text("BlockEntityLimiter configuration reloaded successfully!")
                .color(NamedTextColor.GREEN));
            return true;
        }
        
        if (args[0].equalsIgnoreCase("forceloaded")) {
            if (!sender.hasPermission("blockentitylimiter.admin")) {
                sender.sendMessage(Component.text("You don't have permission to use this command!")
                    .color(NamedTextColor.RED));
                return true;
            }
            
            Set<String> forceloaded = plugin.getForceloadManager().getForceloadedChunks();
            
            if (forceloaded.isEmpty()) {
                sender.sendMessage(Component.text("No chunks are currently forceloaded by this plugin.")
                    .color(NamedTextColor.YELLOW));
                return true;
            }
            
            // Determine page number
            int page = 1;
            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("Invalid page number!")
                        .color(NamedTextColor.RED));
                    return true;
                }
            }
            
            // Page 1: Summary only
            if (page == 1) {
                sender.sendMessage(Component.text("=== Forceloaded Chunks Summary ===")
                    .color(NamedTextColor.GOLD));
                sender.sendMessage(Component.text("Total forceloaded chunks: ")
                    .color(NamedTextColor.YELLOW)
                    .append(Component.text(String.valueOf(forceloaded.size()))
                        .color(NamedTextColor.WHITE)));
                sender.sendMessage(Component.text("Use ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text("/belimiter forceloaded 2")
                        .color(NamedTextColor.YELLOW))
                    .append(Component.text(" to see the list")
                        .color(NamedTextColor.GRAY)));
                return true;
            }
            
            // Page 2+: Show chunks with pagination
            List<String> chunkList = new ArrayList<>(forceloaded);
            int chunksPerPage = 10;
            int totalPages = (int) Math.ceil((double) chunkList.size() / chunksPerPage) + 1; // +1 for summary page
            
            if (page > totalPages) {
                sender.sendMessage(Component.text("Page " + page + " does not exist! Max page: " + totalPages)
                    .color(NamedTextColor.RED));
                return true;
            }
            
            // Adjust for list page (page 2 = index 0)
            int listPage = page - 2;
            int startIndex = listPage * chunksPerPage;
            int endIndex = Math.min(startIndex + chunksPerPage, chunkList.size());
            
            sender.sendMessage(Component.text("=== Forceloaded Chunks (Page " + page + "/" + totalPages + ") ===")
                .color(NamedTextColor.GOLD));
            
            for (int i = startIndex; i < endIndex; i++) {
                sender.sendMessage(Component.text("  " + (i + 1) + ". ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text(chunkList.get(i))
                        .color(NamedTextColor.WHITE)));
            }
            
            // Show navigation
            if (page < totalPages) {
                sender.sendMessage(Component.text("Use ")
                    .color(NamedTextColor.GRAY)
                    .append(Component.text("/belimiter forceloaded " + (page + 1))
                        .color(NamedTextColor.YELLOW))
                    .append(Component.text(" for next page")
                        .color(NamedTextColor.GRAY)));
            }
            
            return true;
        }
        
        sendHelp(sender);
        return true;
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== BlockEntityLimiter Commands ===")
            .color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/belimiter reload")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - Reload the configuration")
                .color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("/belimiter forceloaded [page]")
            .color(NamedTextColor.YELLOW)
            .append(Component.text(" - List forceloaded chunks")
                .color(NamedTextColor.WHITE)));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("blockentitylimiter.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("blockentitylimiter.admin")) {
                completions.add("forceloaded");
            }
        }
        
        return completions;
    }
}