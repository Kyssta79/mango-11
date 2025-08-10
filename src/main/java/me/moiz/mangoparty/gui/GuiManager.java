package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.models.Party;
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
import java.util.UUID;
import java.io.IOException;

public class GuiManager implements Listener {
    private MangoParty plugin;
    private KitEditorGui kitEditorGui;
    private AllowedKitsGui allowedKitsGui;
    
    // Add this field at the top of the class
    private Map<UUID, UUID> challengerTargets = new HashMap<>();
    
    public GuiManager(MangoParty plugin) {
        this.plugin = plugin;
        this.kitEditorGui = new KitEditorGui(plugin);
        this.allowedKitsGui = new AllowedKitsGui(plugin);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // Debug logging
        plugin.getLogger().info("Inventory click - Player: " + player.getName() + ", Title: " + title + ", Slot: " + event.getSlot());
        
        // Handle different GUI types
        if (title.equals("§6Select Match Type")) {
            event.setCancelled(true);
            handleMatchTypeSelection(player, event.getSlot());
        } else if (isKitSelectionGui(title)) {
            event.setCancelled(true);
            handleRegularKitSelection(player, event.getSlot(), title);
        } else if (title.contains("Kit Selection")) {
            event.setCancelled(true);
            handleQueueKitSelection(player, event.getSlot(), title);
        } else if (title.equals("§6Kit Editor")) {
            kitEditorGui.handleClick(event);
        } else if (title.equals("§6Arena Editor")) {
            // Let ArenaEditorGui handle this
            return;
        } else if (title.equals("§6Allowed Kits")) {
            allowedKitsGui.handleClick(event);
        }
    }
    
    private boolean isKitSelectionGui(String title) {
        String lowerTitle = title.toLowerCase();
        return lowerTitle.contains("split") || lowerTitle.contains("ffa") || lowerTitle.contains("duels");
    }
    
    private void handleMatchTypeSelection(Player player, int slot) {
        Party party = plugin.getPartyManager().getParty(player);
        if (party == null || !party.isLeader(player)) {
            player.sendMessage("§cYou must be a party leader to start matches!");
            return;
        }
        
        switch (slot) {
            case 10: // Split
                openKitSelectionGui(player, "split");
                break;
            case 12: // FFA
                openKitSelectionGui(player, "ffa");
                break;
            case 14: // Party Duel
                plugin.getPartyDuelManager().openDuelGui(player);
                break;
        }
    }
    
    private void handleRegularKitSelection(Player player, int slot, String title) {
        plugin.getLogger().info("DEBUG: Handling kit selection - Player: " + player.getName() + ", Slot: " + slot + ", Title: " + title);
        
        Party party = plugin.getPartyManager().getParty(player);
        if (party == null || !party.isLeader(player)) {
            plugin.getLogger().info("DEBUG: Player is not party leader");
            player.sendMessage("§cYou must be a party leader to start matches!");
            return;
        }
        
        // Determine match type from title
        String matchType;
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("split")) {
            matchType = "split";
        } else if (lowerTitle.contains("ffa")) {
            matchType = "ffa";
        } else {
            plugin.getLogger().info("DEBUG: Could not determine match type from title: " + title);
            return;
        }
        
        plugin.getLogger().info("DEBUG: Match type determined: " + matchType);
        
        // Load GUI configuration
        File guiFile = new File(plugin.getDataFolder(), "gui/" + matchType + ".yml");
        if (!guiFile.exists()) {
            plugin.getLogger().info("DEBUG: GUI file not found: " + guiFile.getPath());
            player.sendMessage("§cGUI configuration not found!");
            return;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        
        if (kitsSection == null) {
            plugin.getLogger().info("DEBUG: No kits section found in config");
            player.sendMessage("§cNo kits configured for this match type!");
            return;
        }
        
        // Find kit by slot
        String kitName = null;
        for (String key : kitsSection.getKeys(false)) {
            if (kitsSection.getInt(key + ".slot") == slot) {
                kitName = kitsSection.getString(key + ".kit");
                break;
            }
        }
        
        plugin.getLogger().info("DEBUG: Kit name found for slot " + slot + ": " + kitName);
        
        if (kitName == null) {
            plugin.getLogger().info("DEBUG: No kit found for slot " + slot);
            player.sendMessage("§cNo kit configured for this slot!");
            return;
        }
        
        // Get kit from manager
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            plugin.getLogger().info("DEBUG: Kit not found in KitManager: " + kitName);
            player.sendMessage("§cKit not found: " + kitName);
            return;
        }
        
        plugin.getLogger().info("DEBUG: Kit found: " + kit.getName());
        
        // Start match preparation
        startMatchPreparation(player, party, kit, matchType);
    }
    
    private void startMatchPreparation(Player player, Party party, Kit kit, String matchType) {
        plugin.getLogger().info("DEBUG: Starting match preparation - Kit: " + kit.getName() + ", Type: " + matchType);
        
        // Find available arena or create instance
        Arena arena = findOrCreateArenaForKit(kit);
        
        if (arena == null) {
            plugin.getLogger().info("DEBUG: No arena available for kit: " + kit.getName());
            player.sendMessage("§cNo arena available for this kit!");
            return;
        }
        
        plugin.getLogger().info("DEBUG: Using arena: " + arena.getName());
        
        player.closeInventory();
        player.sendMessage("§aStarting " + matchType + " match with kit: " + kit.getDisplayName());
        
        // Start the match
        plugin.getMatchManager().startMatch(party, arena, kit, matchType);
    }
    
    private Arena findOrCreateArenaForKit(Kit kit) {
        // First, try to find an available arena that allows this kit
        Arena availableArena = plugin.getArenaManager().getAvailableArenaForKit(kit.getName());
        if (availableArena != null) {
            plugin.getLogger().info("DEBUG: Found available arena: " + availableArena.getName());
            return availableArena;
        }
        
        // If no available arena, find a base arena that allows this kit and create an instance
        Arena baseArena = null;
        for (Arena arena : plugin.getArenaManager().getArenas().values()) {
            if (arena.isComplete() && arena.isKitAllowed(kit.getName()) && !arena.isInstance()) {
                baseArena = arena;
                break;
            }
        }
        
        if (baseArena != null) {
            plugin.getLogger().info("DEBUG: Creating instance of arena: " + baseArena.getName());
            Arena instance = plugin.getArenaManager().createArenaInstance(baseArena, kit.getName());
            if (instance != null) {
                plugin.getLogger().info("DEBUG: Created arena instance: " + instance.getName());
                return instance;
            }
        }
        
        plugin.getLogger().info("DEBUG: No suitable arena found for kit: " + kit.getName());
        return null;
    }
    
    private void handleQueueKitSelection(Player player, int slot, String title) {
        // Extract mode from title (1v1, 2v2, 3v3)
        String mode = null;
        if (title.contains("1V1")) mode = "1v1";
        else if (title.contains("2V2")) mode = "2v2";
        else if (title.contains("3V3")) mode = "3v3";
        
        if (mode == null) return;
        
        // Load queue GUI configuration
        File guiFile = new File(plugin.getDataFolder(), "gui/" + mode + "kits.yml");
        if (!guiFile.exists()) return;
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        
        if (kitsSection == null) return;
        
        // Find kit by slot
        String kitName = null;
        for (String key : kitsSection.getKeys(false)) {
            if (kitsSection.getInt(key + ".slot") == slot) {
                kitName = key;
                break;
            }
        }
        
        if (kitName == null) return;
        
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit not found: " + kitName);
            return;
        }
        
        // Add to queue
        QueueEntry entry = new QueueEntry(player, kit, mode);
        plugin.getQueueManager().addToQueue(entry);
        
        player.closeInventory();
        player.sendMessage("§aAdded to " + mode.toUpperCase() + " queue with kit: " + kit.getDisplayName());
    }
    
    public void openMatchTypeGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6Select Match Type");
        
        // Split item
        ItemStack splitItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta splitMeta = splitItem.getItemMeta();
        splitMeta.setDisplayName("§cParty Split");
        List<String> splitLore = new ArrayList<>();
        splitLore.add("§7Split your party into teams");
        splitLore.add("§7and fight against each other");
        splitMeta.setLore(splitLore);
        splitItem.setItemMeta(splitMeta);
        gui.setItem(10, splitItem);
        
        // FFA item
        ItemStack ffaItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta ffaMeta = ffaItem.getItemMeta();
        ffaMeta.setDisplayName("§6Party FFA");
        List<String> ffaLore = new ArrayList<>();
        ffaLore.add("§7Free for all battle");
        ffaLore.add("§7Last player standing wins");
        ffaMeta.setLore(ffaLore);
        ffaItem.setItemMeta(ffaMeta);
        gui.setItem(12, ffaItem);
        
        // Party Duel item
        ItemStack duelItem = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta duelMeta = duelItem.getItemMeta();
        duelMeta.setDisplayName("§eParty Duel");
        List<String> duelLore = new ArrayList<>();
        duelLore.add("§7Challenge another party");
        duelLore.add("§7to a team vs team battle");
        duelMeta.setLore(duelLore);
        duelItem.setItemMeta(duelMeta);
        gui.setItem(14, duelItem);
        
        player.openInventory(gui);
    }
    
    public void openKitSelectionGui(Player player, String matchType) {
        File guiFile = new File(plugin.getDataFolder(), "gui/" + matchType + ".yml");
        if (!guiFile.exists()) {
            player.sendMessage("§cGUI configuration not found!");
            return;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
        String title = ChatColor.translateAlternateColorCodes('&', config.getString("title", "§6Kit Selection"));
        int size = config.getInt("size", 27);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        if (kitsSection != null) {
            for (String key : kitsSection.getKeys(false)) {
                int slot = kitsSection.getInt(key + ".slot");
                String kitName = kitsSection.getString(key + ".kit");
                String displayName = ChatColor.translateAlternateColorCodes('&', kitsSection.getString(key + ".name", kitName));
                List<String> lore = kitsSection.getStringList(key + ".lore");
                
                // Get kit from manager to use its icon
                Kit kit = plugin.getKitManager().getKit(kitName);
                ItemStack item;
                if (kit != null && kit.getIcon() != null) {
                    item = kit.getIcon().clone();
                } else {
                    item = new ItemStack(Material.IRON_SWORD);
                }
                
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(displayName);
                
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
                
                // Set custom model data if specified
                if (kitsSection.contains(key + ".customModelData")) {
                    meta.setCustomModelData(kitsSection.getInt(key + ".customModelData"));
                }
                
                item.setItemMeta(meta);
                gui.setItem(slot, item);
            }
        }
        
        player.openInventory(gui);
    }
    
    public void openQueueKitSelectionGui(Player player, String mode) {
        File guiFile = new File(plugin.getDataFolder(), "gui/" + mode + "kits.yml");
        if (!guiFile.exists()) {
            player.sendMessage("§cQueue GUI configuration not found!");
            return;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
        String title = ChatColor.translateAlternateColorCodes('&', config.getString("title", "§6" + mode.toUpperCase() + " Kit Selection"));
        int size = config.getInt("size", 27);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        if (kitsSection != null) {
            for (String kitName : kitsSection.getKeys(false)) {
                int slot = kitsSection.getInt(kitName + ".slot");
                String displayName = ChatColor.translateAlternateColorCodes('&', kitsSection.getString(kitName + ".name", kitName));
                List<String> lore = kitsSection.getStringList(kitName + ".lore");
                String materialName = kitsSection.getString(kitName + ".material", "IRON_SWORD");
                
                Material material;
                try {
                    material = Material.valueOf(materialName);
                } catch (IllegalArgumentException e) {
                    material = Material.IRON_SWORD;
                }
                
                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(displayName);
                
                List<String> coloredLore = new ArrayList<>();
                for (String line : lore) {
                    String processedLine = ChatColor.translateAlternateColorCodes('&', line);
                    // Replace queue placeholder
                    int queueCount = plugin.getQueueManager().getQueueCount(mode, kitName);
                    processedLine = processedLine.replace("{queued}", String.valueOf(queueCount));
                    coloredLore.add(processedLine);
                }
                meta.setLore(coloredLore);
                
                // Set custom model data if specified
                if (kitsSection.contains(kitName + ".customModelData")) {
                    meta.setCustomModelData(kitsSection.getInt(kitName + ".customModelData"));
                }
                
                item.setItemMeta(meta);
                gui.setItem(slot, item);
            }
        }
        
        player.openInventory(gui);
    }
    
    public void openPartyDuelGui(Player player) {
        Party playerParty = plugin.getPartyManager().getParty(player);
        if (playerParty == null) {
            player.sendMessage("§cYou are not in a party!");
            return;
        }
        
        Inventory gui = Bukkit.createInventory(null, 54, "§6Challenge Party");
        
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (slot >= 54) break;
            
            Party otherParty = plugin.getPartyManager().getParty(online);
            if (otherParty != null && 
                otherParty.isLeader(online.getUniqueId()) && 
                !otherParty.equals(playerParty) &&
                !otherParty.isInMatch()) {
                
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                ItemMeta meta = head.getItemMeta();
                meta.setDisplayName("§e" + online.getName() + "'s Party");
                
                List<String> lore = new ArrayList<>();
                lore.add("§7Members: §f" + otherParty.getSize());
                for (Player member : otherParty.getOnlineMembers()) {
                    if (lore.size() < 8) { // Limit lore size
                        lore.add("§8• §7" + member.getName());
                    }
                }
                lore.add("§aClick to challenge!");
                meta.setLore(lore);
                head.setItemMeta(meta);
                
                gui.setItem(slot, head);
                slot++;
            }
        }
        
        player.openInventory(gui);
    }
}
