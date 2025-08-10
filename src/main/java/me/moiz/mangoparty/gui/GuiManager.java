package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
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

public class GuiManager implements Listener {
    private MangoParty plugin;
    private YamlConfiguration splitConfig;
    private YamlConfiguration ffaConfig;
    private YamlConfiguration kits1v1Config;
    private YamlConfiguration kits2v2Config;
    private YamlConfiguration kits3v3Config;
    
    // Add this field at the top of the class
    private Map<UUID, UUID> challengerTargets = new HashMap<>();
    
    public GuiManager(MangoParty plugin) {
        this.plugin = plugin;
        loadConfigs();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    private void loadConfigs() {
        File guiDir = new File(plugin.getDataFolder(), "gui");
        if (!guiDir.exists()) {
            guiDir.mkdirs();
        }
        
        // Load all GUI configs
        splitConfig = loadGuiConfig("split.yml");
        ffaConfig = loadGuiConfig("ffa.yml");
        kits1v1Config = loadGuiConfig("1v1kits.yml");
        kits2v2Config = loadGuiConfig("2v2kits.yml");
        kits3v3Config = loadGuiConfig("3v3kits.yml");
    }
    
    private YamlConfiguration loadGuiConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), "gui/" + fileName);
        if (!file.exists()) {
            plugin.saveResource("gui/" + fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }
    
    public void openMatchTypeGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§6Match Type Selection");
        
        // Split Duels
        ItemStack splitItem = new ItemStack(Material.IRON_SWORD);
        ItemMeta splitMeta = splitItem.getItemMeta();
        splitMeta.setDisplayName("§6Split Duels");
        splitMeta.setLore(List.of("§7Fight in split arenas", "§eClick to select kit"));
        splitItem.setItemMeta(splitMeta);
        gui.setItem(10, splitItem);
        
        // FFA
        ItemStack ffaItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta ffaMeta = ffaItem.getItemMeta();
        ffaMeta.setDisplayName("§cFFA");
        ffaMeta.setLore(List.of("§7Free for all battles", "§eClick to select kit"));
        ffaItem.setItemMeta(ffaMeta);
        gui.setItem(12, ffaItem);
        
        // 1v1 Queue
        ItemStack queue1v1Item = new ItemStack(Material.GOLDEN_SWORD);
        ItemMeta queue1v1Meta = queue1v1Item.getItemMeta();
        queue1v1Meta.setDisplayName("§a1v1 Queue");
        queue1v1Meta.setLore(List.of("§7Join 1v1 ranked queue", "§eClick to select kit"));
        queue1v1Item.setItemMeta(queue1v1Meta);
        gui.setItem(14, queue1v1Item);
        
        // 2v2 Queue
        ItemStack queue2v2Item = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta queue2v2Meta = queue2v2Item.getItemMeta();
        queue2v2Meta.setDisplayName("§b2v2 Queue");
        queue2v2Meta.setLore(List.of("§7Join 2v2 ranked queue", "§eClick to select kit"));
        queue2v2Item.setItemMeta(queue2v2Meta);
        gui.setItem(15, queue2v2Item);
        
        // 3v3 Queue
        ItemStack queue3v3Item = new ItemStack(Material.GOLDEN_PICKAXE);
        ItemMeta queue3v3Meta = queue3v3Item.getItemMeta();
        queue3v3Meta.setDisplayName("§d3v3 Queue");
        queue3v3Meta.setLore(List.of("§7Join 3v3 ranked queue", "§eClick to select kit"));
        queue3v3Item.setItemMeta(queue3v3Meta);
        gui.setItem(16, queue3v3Item);
        
        player.openInventory(gui);
    }
    
    public void openSplitGui(Player player) {
        openKitSelectionGui(player, splitConfig, "split");
    }
    
    public void openFfaGui(Player player) {
        openKitSelectionGui(player, ffaConfig, "ffa");
    }
    
    public void open1v1KitsGui(Player player) {
        openKitSelectionGui(player, kits1v1Config, "1v1");
    }
    
    public void open2v2KitsGui(Player player) {
        openKitSelectionGui(player, kits2v2Config, "2v2");
    }
    
    public void open3v3KitsGui(Player player) {
        openKitSelectionGui(player, kits3v3Config, "3v3");
    }
    
    private void openKitSelectionGui(Player player, YamlConfiguration config, String matchType) {
        String title = config.getString("title", "§6Kit Selection");
        int size = config.getInt("size", 54);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // Add kit items
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        if (kitsSection != null) {
            for (String kitName : kitsSection.getKeys(false)) {
                ConfigurationSection kitSection = kitsSection.getConfigurationSection(kitName);
                if (kitSection != null) {
                    int slot = kitSection.getInt("slot");
                    String material = kitSection.getString("material");
                    String displayName = kitSection.getString("name");
                    List<String> lore = kitSection.getStringList("lore");
                    int customModelData = kitSection.getInt("customModelData", 0);
                    
                    ItemStack item = new ItemStack(Material.valueOf(material));
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(displayName);
                    meta.setLore(lore);
                    if (customModelData > 0) {
                        meta.setCustomModelData(customModelData);
                    }
                    item.setItemMeta(meta);
                    
                    gui.setItem(slot, item);
                }
            }
        }
        
        player.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        event.setCancelled(true);
        
        if (title.equals("§6Match Type Selection")) {
            handleMatchTypeSelection(player, event.getSlot());
        } else if (isKitSelectionGui(title)) {
            handleKitSelection(player, event.getCurrentItem(), title);
        }
    }
    
    private boolean isKitSelectionGui(String title) {
        return title.contains("Split") || title.contains("FFA") || 
               title.contains("1v1") || title.contains("2v2") || title.contains("3v3") ||
               title.contains("Duels") || title.contains("Kit");
    }
    
    private void handleMatchTypeSelection(Player player, int slot) {
        switch (slot) {
            case 10: // Split
                openSplitGui(player);
                break;
            case 12: // FFA
                openFfaGui(player);
                break;
            case 14: // 1v1 Queue
                open1v1KitsGui(player);
                break;
            case 15: // 2v2 Queue
                open2v2KitsGui(player);
                break;
            case 16: // 3v3 Queue
                open3v3KitsGui(player);
                break;
        }
    }
    
    private void handleKitSelection(Player player, ItemStack item, String title) {
        if (item == null || item.getType() == Material.AIR) return;
        
        String kitName = extractKitName(item);
        if (kitName == null) return;
        
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (kit == null) {
            player.sendMessage("§cKit not found!");
            return;
        }
        
        if (title.contains("Split") || title.contains("Duels")) {
            handleSplitKitSelection(player, kit);
        } else if (title.contains("FFA")) {
            handleFfaKitSelection(player, kit);
        } else if (title.contains("1v1") || title.contains("2v2") || title.contains("3v3")) {
            handleQueueKitSelection(player, kit, title);
        }
        
        player.closeInventory();
    }
    
    private void handleSplitKitSelection(Player player, Kit kit) {
        Party party = plugin.getPartyManager().getParty(player);
        if (party == null) {
            player.sendMessage("§cYou must be in a party to start a split duel!");
            return;
        }
        
        if (!party.isLeader(player)) {
            player.sendMessage("§cOnly the party leader can start matches!");
            return;
        }
        
        // Find or create arena for this kit
        Arena arena = findOrCreateArenaForKit(kit.getName());
        if (arena == null) {
            player.sendMessage("§cNo arena available for this kit!");
            return;
        }
        
        // Challenge another party
        plugin.getPartyDuelManager().challengeParty(party, kit);
        player.sendMessage("§aLooking for another party to fight with " + kit.getDisplayName() + "§a...");
    }
    
    private void handleFfaKitSelection(Player player, Kit kit) {
        // Find or create arena for this kit
        Arena arena = findOrCreateArenaForKit(kit.getName());
        if (arena == null) {
            player.sendMessage("§cNo arena available for this kit!");
            return;
        }
        
        // Start FFA match
        player.sendMessage("§aStarting FFA match with " + kit.getDisplayName() + "§a...");
        // TODO: Implement FFA match creation
    }
    
    private void handleQueueKitSelection(Player player, Kit kit, String title) {
        Party party = plugin.getPartyManager().getParty(player);
        
        String queueType;
        if (title.contains("1v1")) {
            queueType = "1v1";
            if (party != null && party.getMembers().size() > 1) {
                player.sendMessage("§cYou cannot join 1v1 queue with a party!");
                return;
            }
        } else if (title.contains("2v2")) {
            queueType = "2v2";
            if (party == null || party.getMembers().size() != 2) {
                player.sendMessage("§cYou need exactly 2 players in your party for 2v2!");
                return;
            }
        } else if (title.contains("3v3")) {
            queueType = "3v3";
            if (party == null || party.getMembers().size() != 3) {
                player.sendMessage("§cYou need exactly 3 players in your party for 3v3!");
                return;
            }
        } else {
            return;
        }
        
        if (party != null && !party.isLeader(player)) {
            player.sendMessage("§cOnly the party leader can join queues!");
            return;
        }
        
        // Join queue
        plugin.getQueueManager().joinQueue(player, queueType, kit.getName());
        player.sendMessage("§aJoined " + queueType + " queue with " + kit.getDisplayName() + "§a!");
    }
    
    private Arena findOrCreateArenaForKit(String kitName) {
        // First try to find an available arena
        Arena arena = plugin.getArenaManager().getAvailableArenaForKit(kitName);
        if (arena != null) {
            plugin.getLogger().info("Found available arena: " + arena.getName() + " for kit: " + kitName);
            return arena;
        }
        
        // If no available arena, try to create an instance
        Map<String, Arena> allArenas = plugin.getArenaManager().getArenas();
        for (Arena baseArena : allArenas.values()) {
            if (!baseArena.isInstance() && baseArena.isComplete() && baseArena.isKitAllowed(kitName)) {
                Arena instance = plugin.getArenaManager().createArenaInstance(baseArena, kitName);
                if (instance != null) {
                    plugin.getLogger().info("Created arena instance: " + instance.getName() + " for kit: " + kitName);
                    return instance;
                }
            }
        }
        
        plugin.getLogger().warning("No arena found or created for kit: " + kitName);
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
}
