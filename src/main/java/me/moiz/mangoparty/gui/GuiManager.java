package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.models.Party;
import me.moiz.mangoparty.models.QueueEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiManager implements Listener {
    private MangoParty plugin;
    private Map<String, YamlConfiguration> guiConfigs;
    private KitEditorGui kitEditorGui;
    private AllowedKitsGui allowedKitsGui;
    private ArenaEditorGui arenaEditorGui;
    
    public GuiManager(MangoParty plugin) {
        this.plugin = plugin;
        this.guiConfigs = new HashMap<>();
        this.kitEditorGui = new KitEditorGui(plugin);
        this.allowedKitsGui = new AllowedKitsGui(plugin);
        this.arenaEditorGui = new ArenaEditorGui(plugin);
        loadGuiConfigs();
    }
    
    private void loadGuiConfigs() {
        String[] guiFiles = {"split.yml", "ffa.yml", "1v1kits.yml", "2v2kits.yml", "3v3kits.yml", 
                           "kit_list.yml", "arena_list.yml", "kit_editor.yml", "allowed_kits.yml", "arena_editor.yml"};
        
        for (String fileName : guiFiles) {
            File file = new File(plugin.getDataFolder() + "/gui", fileName);
            if (!file.exists()) {
                plugin.saveResource("gui/" + fileName, false);
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            guiConfigs.put(fileName.replace(".yml", ""), config);
        }
    }
    
    public void reloadGuiConfigs() {
        guiConfigs.clear();
        loadGuiConfigs();
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Handle kit editor GUI
        if (title.contains("Kit Editor")) {
            kitEditorGui.handleClick(event);
            return;
        }
        
        // Handle allowed kits GUI
        if (title.contains("Allowed Kits")) {
            allowedKitsGui.handleClick(event);
            return;
        }
        
        // Handle arena editor GUI
        if (title.contains("Arena Editor")) {
            arenaEditorGui.handleClick(event);
            return;
        }
        
        // Check if player is in a party
        Party party = plugin.getPartyManager().getParty(player.getUniqueId());
        if (party == null) {
            player.sendMessage("§cYou must be in a party to use this!");
            event.setCancelled(true);
            return;
        }
        
        // Check if player is party leader for certain actions
        if (!party.isLeader(player.getUniqueId()) && (title.contains("Split") || title.contains("FFA"))) {
            player.sendMessage("§cOnly the party leader can start matches!");
            event.setCancelled(true);
            return;
        }
        
        // Handle party vs party duel GUI
        if (title.contains("Duel")) {
            plugin.getPartyDuelManager().openDuelGui(player);
            event.setCancelled(true);
            return;
        }
        
        // Check if party is already in a match
        if (party.isInMatch()) {
            player.sendMessage("§cYour party is already in a match!");
            event.setCancelled(true);
            return;
        }
        
        if (isKitSelectionGui(title)) {
            handleKitSelection(event, player, party, title);
        } else if (isQueueGui(title)) {
            handleQueueSelection(event, player, title);
        }
    }
    
    private boolean isKitSelectionGui(String title) {
        String lowerTitle = title.toLowerCase();
        return lowerTitle.contains("split") || lowerTitle.contains("ffa") || lowerTitle.contains("duels");
    }
    
    private boolean isQueueGui(String title) {
        return title.contains("1v1") || title.contains("2v2") || title.contains("3v3");
    }
    
    private void handleKitSelection(InventoryClickEvent event, Player player, Party party, String title) {
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        String kitName = getKitNameFromItem(clickedItem);
        if (kitName == null) return;
        
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit not found: " + kitName);
            return;
        }
        
        // Determine match type from title
        String matchType = determineMatchType(title);
        
        // Find or create arena for this kit
        Arena arena = findOrCreateArenaForKit(kit.getName());
        if (arena == null) {
            player.sendMessage("§cNo arena available for this kit!");
            return;
        }
        
        player.closeInventory();
        plugin.getMatchManager().startMatch(party, arena, kit, matchType);
    }
    
    private Arena findOrCreateArenaForKit(String kitName) {
        // First try to find an available arena for this kit
        Arena availableArena = plugin.getArenaManager().getAvailableArenaForKit(kitName);
        if (availableArena != null) {
            return availableArena;
        }
        
        // If no available arena, try to create an instance
        // Find a base arena that allows this kit
        for (Arena arena : plugin.getArenaManager().getArenas().values()) {
            if (arena.isComplete() && arena.isKitAllowed(kitName) && !arena.isInstance()) {
                // Create an instance of this arena
                Arena instance = plugin.getArenaManager().createArenaInstance(arena, kitName);
                if (instance != null) {
                    plugin.getLogger().info("Created arena instance for kit " + kitName + ": " + instance.getName());
                    return instance;
                }
            }
        }
        
        // If still no arena, just get any available arena
        return plugin.getArenaManager().getAvailableArena();
    }
    
    private String determineMatchType(String title) {
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("split")) {
            return "split";
        } else if (lowerTitle.contains("ffa")) {
            return "ffa";
        } else if (lowerTitle.contains("duel")) {
            return "split"; // Duels are typically split matches
        }
        return "ffa"; // Default
    }
    
    private void handleQueueSelection(InventoryClickEvent event, Player player, String title) {
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        String kitName = getKitNameFromItem(clickedItem);
        if (kitName == null) return;
        
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit not found: " + kitName);
            return;
        }
        
        String mode = title.contains("1v1") ? "1v1" : title.contains("2v2") ? "2v2" : "3v3";
        
        player.closeInventory();
        
        QueueEntry entry = new QueueEntry(player, kit, mode);
        plugin.getQueueManager().addToQueue(entry);
        
        player.sendMessage("§aYou have been added to the " + mode + " queue with kit: " + kit.getName());
    }
    
    private String getKitNameFromItem(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        return null;
    }
    
    public void openSplitGui(Player player) {
        openGui(player, "split");
    }
    
    public void openFFAGui(Player player) {
        openGui(player, "ffa");
    }
    
    public void openQueueKitGui(Player player, String mode) {
        openGui(player, mode.toLowerCase() + "kits");
    }
    
    public void openPartyVsPartyKitGui(Player player, Player target) {
        // This would open a GUI for selecting kits for party vs party duels
        openGui(player, "split"); // For now, use split GUI
    }
    
    public void openKitListGui(Player player) {
        openGui(player, "kit_list");
    }
    
    public void openArenaListGui(Player player) {
        openGui(player, "arena_list");
    }
    
    public void openKitEditorGui(Player player, String kitName) {
        kitEditorGui.openGui(player, kitName);
    }
    
    public void openAllowedKitsGui(Player player, String arenaName) {
        allowedKitsGui.openGui(player, arenaName);
    }
    
    public void openArenaEditorGui(Player player, String arenaName) {
        arenaEditorGui.openGui(player, arenaName);
    }
    
    private void openGui(Player player, String guiName) {
        YamlConfiguration config = guiConfigs.get(guiName);
        if (config == null) {
            player.sendMessage("§cGUI configuration not found: " + guiName);
            return;
        }
        
        String title = config.getString("title", "GUI");
        int size = config.getInt("size", 54);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // Load items from config
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = createItemFromConfig(itemSection);
                    int slot = itemSection.getInt("slot", 0);
                    if (slot < gui.getSize()) {
                        gui.setItem(slot, item);
                    }
                }
            }
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createItemFromConfig(ConfigurationSection section) {
        String materialName = section.getString("material", "STONE");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.STONE;
        }
        
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            String displayName = section.getString("name");
            if (displayName != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            }
            
            List<String> lore = section.getStringList("lore");
            if (!lore.isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
}
