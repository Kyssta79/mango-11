package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.models.Kit;
import me.moiz.mangoparty.models.Party;
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
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    private void loadGuiConfigs() {
        String[] guiFiles = {"split.yml", "ffa.yml", "1v1kits.yml", "2v2kits.yml", "3v3kits.yml"};
        
        File guiDir = new File(plugin.getDataFolder(), "gui");
        if (!guiDir.exists()) {
            guiDir.mkdirs();
        }
        
        for (String fileName : guiFiles) {
            File file = new File(guiDir, fileName);
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
        if (title.contains("Kit Editor") || title.contains("Kit List")) {
            // Kit editor handles its own events
            return;
        }
        
        // Handle allowed kits GUI
        if (title.contains("Allowed Kits")) {
            // Allowed kits GUI handles its own events
            return;
        }
        
        // Handle arena editor GUI
        if (title.contains("Arena")) {
            // Arena editor GUI handles its own events
            return;
        }
        
        // Handle party duel GUI
        if (title.contains("Party Duel")) {
            event.setCancelled(true);
            Party party = plugin.getPartyManager().getParty(player);
            if (party == null || !party.isLeader(player.getUniqueId())) {
                player.sendMessage("§cYou must be a party leader to use this!");
                return;
            }
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            // Handle party duel kit selection
            Kit kit = getKitFromItem(clicked, "duel");
            if (kit != null) {
                // Open party vs party GUI or handle duel logic
                plugin.getPartyDuelManager().challengeParty(player, player, kit.getName());
            }
            return;
        }
        
        // Handle main GUI clicks (split, ffa, queue modes)
        if (isKitSelectionGui(title)) {
            event.setCancelled(true);
            
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            
            String guiType = getGuiTypeFromTitle(title);
            if (guiType == null) return;
            
            Kit kit = getKitFromItem(clicked, guiType);
            if (kit == null) return;
            
            handleKitSelection(player, kit, guiType);
        }
    }
    
    private boolean isKitSelectionGui(String title) {
        return title.contains("Split") || title.contains("FFA") || 
               title.contains("1v1") || title.contains("2v2") || title.contains("3v3") ||
               title.toLowerCase().contains("split") || title.toLowerCase().contains("ffa") ||
               title.toLowerCase().contains("duels");
    }
    
    private String getGuiTypeFromTitle(String title) {
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("split") || lowerTitle.contains("duels")) return "split";
        if (lowerTitle.contains("ffa")) return "ffa";
        if (lowerTitle.contains("1v1")) return "1v1kits";
        if (lowerTitle.contains("2v2")) return "2v2kits";
        if (lowerTitle.contains("3v3")) return "3v3kits";
        return null;
    }
    
    private Kit getKitFromItem(ItemStack item, String guiType) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return null;
        }
        
        String displayName = item.getItemMeta().getDisplayName();
        
        // Try to find kit by display name
        for (Kit kit : plugin.getKitManager().getKits().values()) {
            if (kit.getDisplayName().equals(displayName) || 
                displayName.contains(kit.getDisplayName()) ||
                displayName.contains(kit.getName())) {
                return kit;
            }
        }
        
        return null;
    }
    
    private void handleKitSelection(Player player, Kit kit, String guiType) {
        plugin.getLogger().info("Player " + player.getName() + " selected kit " + kit.getName() + " in " + guiType + " mode");
        
        if (guiType.equals("split") || guiType.equals("ffa")) {
            handleRegularKitSelection(player, kit, guiType);
        } else if (guiType.endsWith("kits")) {
            handleQueueKitSelection(player, kit, guiType);
        }
    }
    
    private void handleRegularKitSelection(Player player, Kit kit, String matchType) {
        Party party = plugin.getPartyManager().getParty(player);
        if (party == null) {
            player.sendMessage("§cYou must be in a party to start a match!");
            return;
        }
        
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the party leader can start matches!");
            return;
        }
        
        if (party.isInMatch()) {
            player.sendMessage("§cYour party is already in a match!");
            return;
        }
        
        // Find or create arena for this kit
        Arena arena = findOrCreateArenaForKit(kit);
        if (arena == null) {
            player.sendMessage("§cNo arena available for this kit!");
            return;
        }
        
        player.closeInventory();
        plugin.getMatchManager().startMatch(party, arena, kit, matchType);
    }
    
    private void handleQueueKitSelection(Player player, Kit kit, String queueType) {
        String mode = queueType.replace("kits", "");
        
        Party party = plugin.getPartyManager().getParty(player);
        if (party == null) {
            player.sendMessage("§cYou must be in a party to join queue!");
            return;
        }
        
        if (!party.isLeader(player.getUniqueId())) {
            player.sendMessage("§cOnly the party leader can join queue!");
            return;
        }
        
        plugin.getQueueManager().joinQueue(player, mode, kit.getName());
        
        player.closeInventory();
        player.sendMessage("§aJoined " + mode.toUpperCase() + " queue with kit: " + kit.getDisplayName());
    }
    
    private Arena findOrCreateArenaForKit(Kit kit) {
        // First try to find an available arena that allows this kit
        Arena availableArena = plugin.getArenaManager().getAvailableArenaForKit(kit.getName());
        if (availableArena != null) {
            return availableArena;
        }
        
        // If no available arena, try to find a base arena that allows this kit and create an instance
        for (Arena arena : plugin.getArenaManager().getArenas().values()) {
            if (arena.isComplete() && arena.isKitAllowed(kit.getName()) && !arena.isInstance()) {
                // Create an instance of this arena
                Arena instance = plugin.getArenaManager().createArenaInstance(arena, kit.getName());
                if (instance != null) {
                    plugin.getLogger().info("Created arena instance " + instance.getName() + " for kit " + kit.getName());
                    return instance;
                }
            }
        }
        
        plugin.getLogger().warning("No arena found or could be created for kit: " + kit.getName());
        return null;
    }
    
    public void openSplitGui(Player player) {
        openKitGui(player, "split");
    }
    
    public void openFFAGui(Player player) {
        openKitGui(player, "ffa");
    }
    
    public void openQueueKitGui(Player player, String mode) {
        openKitGui(player, mode + "kits");
    }
    
    public void openPartyVsPartyKitGui(Player player, Player target) {
        // This would open a GUI for party vs party duels
        player.sendMessage("§aOpening party vs party kit selection...");
    }
    
    public void openMatchTypeGui(Player player) {
        // This would open a GUI to select match type (split, ffa, etc.)
        player.sendMessage("§aOpening match type selection...");
    }
    
    private void openKitGui(Player player, String guiType) {
        YamlConfiguration config = guiConfigs.get(guiType);
        if (config == null) {
            player.sendMessage("§cGUI configuration not found for: " + guiType);
            return;
        }
        
        String title = config.getString("title", "§6Kit Selection");
        int size = config.getInt("size", 54);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        if (kitsSection != null) {
            for (String kitName : kitsSection.getKeys(false)) {
                ConfigurationSection kitConfig = kitsSection.getConfigurationSection(kitName);
                if (kitConfig != null) {
                    Kit kit = plugin.getKitManager().getKit(kitName);
                    if (kit != null) {
                        ItemStack item = createKitItem(kit, kitConfig);
                        int slot = kitConfig.getInt("slot", 0);
                        if (slot < size) {
                            gui.setItem(slot, item);
                        }
                    }
                }
            }
        }
        
        player.openInventory(gui);
    }
    
    private ItemStack createKitItem(Kit kit, ConfigurationSection config) {
        Material material = Material.valueOf(config.getString("material", "IRON_SWORD"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(config.getString("name", kit.getDisplayName()));
        
        List<String> lore = config.getStringList("lore");
        if (lore.isEmpty()) {
            lore = new ArrayList<>();
            lore.add("§7Click to select this kit");
        }
        meta.setLore(lore);
        
        int customModelData = config.getInt("customModelData", 0);
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    public KitEditorGui getKitEditorGui() {
        return kitEditorGui;
    }
    
    public AllowedKitsGui getAllowedKitsGui() {
        return allowedKitsGui;
    }
    
    public ArenaEditorGui getArenaEditorGui() {
        return arenaEditorGui;
    }
}
