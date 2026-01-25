package com.yourname.blockentitylimiter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

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
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            if (sender.hasPermission("blockentitylimiter.reload")) {
                completions.add("reload");
            }
        }
        
        return completions;
    }
}