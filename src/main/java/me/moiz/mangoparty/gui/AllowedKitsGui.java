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
    private YamlConfiguration config;
    
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
        
        File configFile = new File(guiDir, "allowed_kits.yml");
        if (!configFile.exists()) {
            plugin.saveResource("gui/allowed_kits.yml", false);
        }
        
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public void openAllowedKitsGui(Player player, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            player.sendMessage("§cArena not found!");
            return;
        }
        
        String title = config.getString("title", "§6Allowed Kits").replace("{arena}", arenaName);
        int size = config.getInt("size", 54);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        Map<String, Kit> allKits = plugin.getKitManager().getKits();
        int slot = 0;
        
        for (Kit kit : allKits.values()) {
            if (slot >= size) break;
            
            ItemStack item = createKitItem(kit, arena);
            gui.setItem(slot, item);
            slot++;
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createKitItem(Kit kit, Arena arena) {
        boolean isAllowed = arena.isKitAllowed(kit.getName());
        
        ConfigurationSection kitConfig = config.getConfigurationSection("kit_item");
        
        String materialKey = isAllowed ? "material_enabled" : "material_disabled";
        String nameKey = isAllowed ? "name_enabled" : "name_disabled";
        String loreKey = isAllowed ? "lore_enabled" : "lore_disabled";
        
        Material material = Material.valueOf(kitConfig.getString(materialKey, "IRON_SWORD"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        String name = kitConfig.getString(nameKey, "§a{kit_name}");
        name = name.replace("{kit_name}", kit.getDisplayName());
        meta.setDisplayName(name);
        
        List<String> lore = new ArrayList<>(kitConfig.getStringList(loreKey));
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, lore.get(i).replace("{kit_name}", kit.getDisplayName()));
        }
        meta.setLore(lore);
        
        int customModelData = kitConfig.getInt("customModelData", 0);
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (!title.startsWith(config.getString("title", "§6Allowed Kits").split(" ")[0])) return;
        
        event.setCancelled(true);
        
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        String arenaName = extractArenaName(title);
        if (arenaName == null) return;
        
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) return;
        
        String kitName = extractKitName(clicked);
        if (kitName == null) return;
        
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) return;
        
        // Toggle kit allowance
        boolean wasAllowed = arena.isKitAllowed(kit.getName());
        if (wasAllowed) {
            arena.removeAllowedKit(kit.getName());
            player.sendMessage("§cDisabled kit: " + kit.getDisplayName() + " for arena: " + arenaName);
            plugin.getLogger().info("Player " + player.getName() + " disabled kit " + kit.getName() + " for arena " + arenaName);
        } else {
            arena.addAllowedKit(kit.getName());
            player.sendMessage("§aEnabled kit: " + kit.getDisplayName() + " for arena: " + arenaName);
            plugin.getLogger().info("Player " + player.getName() + " enabled kit " + kit.getName() + " for arena " + arenaName);
        }
        
        // Save the arena immediately
        plugin.getArenaManager().saveArena(arena);
        plugin.getLogger().info("Saved arena " + arenaName + " with updated kit allowances");
        
        // Refresh the GUI after a short delay to show the updated state
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            openAllowedKitsGui(player, arenaName);
        }, 1L);
    }
    
    private String extractArenaName(String title) {
        String prefix = config.getString("title", "§6Allowed Kits").replace("{arena}", "");
        if (title.startsWith(prefix)) {
            return title.substring(prefix.length()).trim();
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
            if (displayName.contains(kit.getDisplayName()) || displayName.contains(kit.getName())) {
                return kit.getName();
            }
        }
        
        return null;
    }
}
