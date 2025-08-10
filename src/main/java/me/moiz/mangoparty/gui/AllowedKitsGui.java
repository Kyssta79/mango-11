package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.models.Kit;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllowedKitsGui implements Listener {
    private MangoParty plugin;
    private YamlConfiguration allowedKitsConfig;
    
    public AllowedKitsGui(MangoParty plugin) {
        this.plugin = plugin;
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    private void loadConfig() {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        if (!guiDir.exists()) {
            guiDir.mkdirs();
        }
        
        File allowedKitsFile = new File(guiDir, "allowed_kits.yml");
        
        if (!allowedKitsFile.exists()) {
            plugin.saveResource("gui/allowed_kits.yml", false);
        }
        
        allowedKitsConfig = YamlConfiguration.loadConfiguration(allowedKitsFile);
    }
    
    public void openAllowedKitsGui(Player player, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            player.sendMessage("§cArena not found!");
            return;
        }
        
        String title = allowedKitsConfig.getString("title", "§6Allowed Kits").replace("{arena_name}", arenaName);
        int size = allowedKitsConfig.getInt("size", 54);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // Add all available kits with toggle status
        Map<String, Kit> availableKits = plugin.getKitManager().getKits();
        int slot = 0;
        
        for (Kit kit : availableKits.values()) {
            if (slot >= size - 9) break; // Reserve bottom row for controls
            
            ItemStack item = createKitToggleItem(kit, arena);
            gui.setItem(slot, item);
            slot++;
        }
        
        // Add back button
        ConfigurationSection backButtonConfig = allowedKitsConfig.getConfigurationSection("buttons.back");
        if (backButtonConfig != null) {
            ItemStack backButton = new ItemStack(Material.valueOf(backButtonConfig.getString("material")));
            ItemMeta meta = backButton.getItemMeta();
            meta.setDisplayName(backButtonConfig.getString("name"));
            meta.setLore(backButtonConfig.getStringList("lore"));
            
            int customModelData = backButtonConfig.getInt("customModelData");
            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }
            
            backButton.setItemMeta(meta);
            gui.setItem(backButtonConfig.getInt("slot"), backButton);
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createKitToggleItem(Kit kit, Arena arena) {
        boolean isAllowed = arena.isKitAllowed(kit.getName());
        
        ItemStack item = kit.getIcon() != null ? kit.getIcon().clone() : new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName((isAllowed ? "§a" : "§c") + kit.getDisplayName());
        
        List<String> lore = new ArrayList<>();
        lore.add(isAllowed ? "§aEnabled for this arena" : "§cDisabled for this arena");
        lore.add("§7");
        lore.add("§eClick to " + (isAllowed ? "disable" : "enable") + " this kit");
        meta.setLore(lore);
        
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Check if this is an allowed kits GUI
        String configTitle = allowedKitsConfig.getString("title", "§6Allowed Kits");
        String titlePrefix = configTitle.split(" - ")[0];
        
        if (!title.startsWith(titlePrefix)) {
            return;
        }
        
        event.setCancelled(true);
        
        String arenaName = extractArenaNameFromTitle(title);
        if (arenaName == null) {
            plugin.getLogger().warning("Could not extract arena name from title: " + title);
            return;
        }
        
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            player.sendMessage("§cArena not found: " + arenaName);
            return;
        }
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Handle back button
        ConfigurationSection backButtonConfig = allowedKitsConfig.getConfigurationSection("buttons.back");
        if (backButtonConfig != null && event.getSlot() == backButtonConfig.getInt("slot")) {
            plugin.getArenaEditorGui().openArenaEditorGui(player, arenaName);
            return;
        }
        
        // Handle kit toggle
        String kitName = extractKitName(clicked);
        if (kitName != null) {
            boolean wasAllowed = arena.isKitAllowed(kitName);
            toggleKitAllowed(arena, kitName);
            
            // Save the arena immediately
            plugin.getArenaManager().saveArena(arena);
            
            // Log the change
            plugin.getLogger().info("Player " + player.getName() + " " + 
                (wasAllowed ? "disabled" : "enabled") + " kit " + kitName + 
                " for arena " + arenaName);
            
            // Refresh the GUI to show the change
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                openAllowedKitsGui(player, arenaName);
            }, 1L);
        }
    }
    
    private String extractArenaNameFromTitle(String title) {
        String configTitle = allowedKitsConfig.getString("title", "§6Allowed Kits - {arena_name}");
        String prefix = configTitle.replace(" - {arena_name}", "");
        
        if (title.startsWith(prefix + " - ")) {
            return title.substring((prefix + " - ").length());
        }
        return null;
    }
    
    private String extractKitName(ItemStack item) {
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        // Remove color codes
        displayName = displayName.replaceAll("§[0-9a-fk-or]", "");
        
        // Try to find kit by display name
        for (Kit kit : plugin.getKitManager().getKits().values()) {
            if (displayName.equals(kit.getDisplayName())) {
                return kit.getName();
            }
        }
        
        return null;
    }
    
    private void toggleKitAllowed(Arena arena, String kitName) {
        if (arena.isKitAllowed(kitName)) {
            arena.removeAllowedKit(kitName);
        } else {
            arena.addAllowedKit(kitName);
        }
    }
}
