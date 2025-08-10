package me.moiz.mangoparty.gui;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import me.moiz.mangoparty.models.Kit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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

public class ArenaEditorGui implements Listener {
    private MangoParty plugin;
    private YamlConfiguration config;
    
    public ArenaEditorGui(MangoParty plugin) {
        this.plugin = plugin;
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "gui/arena_editor.yml");
        if (!configFile.exists()) {
            plugin.saveResource("gui/arena_editor.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }
    
    public void openArenaEditor(Player player, Arena arena) {
        String title = config.getString("title", "§6Arena Editor").replace("{arena}", arena.getName());
        int size = config.getInt("size", 54);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // Arena info item
        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§eArena Information");
        List<String> infoLore = new ArrayList<>();
        infoLore.add("§7Name: §f" + arena.getName());
        infoLore.add("§7Complete: " + (arena.isComplete() ? "§aYes" : "§cNo"));
        infoLore.add("§7Spawn 1: " + (arena.getSpawn1() != null ? "§aSet" : "§cNot Set"));
        infoLore.add("§7Spawn 2: " + (arena.getSpawn2() != null ? "§aSet" : "§cNot Set"));
        infoLore.add("§7Center: " + (arena.getCenter() != null ? "§aSet" : "§cNot Set"));
        infoLore.add("§7Min Corner: " + (arena.getMinCorner() != null ? "§aSet" : "§cNot Set"));
        infoLore.add("§7Max Corner: " + (arena.getMaxCorner() != null ? "§aSet" : "§cNot Set"));
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        gui.setItem(4, infoItem);
        
        // Set spawn 1 button
        ItemStack spawn1Item = new ItemStack(Material.RED_BED);
        ItemMeta spawn1Meta = spawn1Item.getItemMeta();
        spawn1Meta.setDisplayName("§cSet Spawn 1");
        List<String> spawn1Lore = new ArrayList<>();
        spawn1Lore.add("§7Click to set spawn point 1");
        spawn1Lore.add("§7to your current location");
        spawn1Meta.setLore(spawn1Lore);
        spawn1Item.setItemMeta(spawn1Meta);
        gui.setItem(19, spawn1Item);
        
        // Set spawn 2 button
        ItemStack spawn2Item = new ItemStack(Material.BLUE_BED);
        ItemMeta spawn2Meta = spawn2Item.getItemMeta();
        spawn2Meta.setDisplayName("§9Set Spawn 2");
        List<String> spawn2Lore = new ArrayList<>();
        spawn2Lore.add("§7Click to set spawn point 2");
        spawn2Lore.add("§7to your current location");
        spawn2Meta.setLore(spawn2Lore);
        spawn2Item.setItemMeta(spawn2Meta);
        gui.setItem(21, spawn2Item);
        
        // Set center button
        ItemStack centerItem = new ItemStack(Material.BEACON);
        ItemMeta centerMeta = centerItem.getItemMeta();
        centerMeta.setDisplayName("§eSet Center");
        List<String> centerLore = new ArrayList<>();
        centerLore.add("§7Click to set center point");
        centerLore.add("§7to your current location");
        centerMeta.setLore(centerLore);
        centerItem.setItemMeta(centerMeta);
        gui.setItem(22, centerItem);
        
        // Set min corner button
        ItemStack minItem = new ItemStack(Material.STONE_BUTTON);
        ItemMeta minMeta = minItem.getItemMeta();
        minMeta.setDisplayName("§7Set Min Corner");
        List<String> minLore = new ArrayList<>();
        minLore.add("§7Click to set minimum corner");
        minLore.add("§7to your current location");
        minMeta.setLore(minLore);
        minItem.setItemMeta(minMeta);
        gui.setItem(37, minItem);
        
        // Set max corner button
        ItemStack maxItem = new ItemStack(Material.STONE_BUTTON);
        ItemMeta maxMeta = maxItem.getItemMeta();
        maxMeta.setDisplayName("§fSet Max Corner");
        List<String> maxLore = new ArrayList<>();
        maxLore.add("§7Click to set maximum corner");
        maxLore.add("§7to your current location");
        maxMeta.setLore(maxLore);
        maxItem.setItemMeta(maxMeta);
        gui.setItem(43, maxItem);
        
        // Allowed kits button
        ItemStack kitsItem = new ItemStack(Material.CHEST);
        ItemMeta kitsMeta = kitsItem.getItemMeta();
        kitsMeta.setDisplayName("§6Manage Allowed Kits");
        List<String> kitsLore = new ArrayList<>();
        kitsLore.add("§7Click to manage which kits");
        kitsLore.add("§7are allowed in this arena");
        kitsLore.add("§7Currently allowed: §f" + arena.getAllowedKits().size());
        kitsMeta.setLore(kitsLore);
        kitsItem.setItemMeta(kitsMeta);
        gui.setItem(31, kitsItem);
        
        // Save button
        ItemStack saveItem = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = saveItem.getItemMeta();
        saveMeta.setDisplayName("§aSave Arena");
        List<String> saveLore = new ArrayList<>();
        saveLore.add("§7Click to save all changes");
        saveLore.add("§7to this arena");
        saveMeta.setLore(saveLore);
        saveItem.setItemMeta(saveMeta);
        gui.setItem(45, saveItem);
        
        // Delete button
        ItemStack deleteItem = new ItemStack(Material.REDSTONE);
        ItemMeta deleteMeta = deleteItem.getItemMeta();
        deleteMeta.setDisplayName("§cDelete Arena");
        List<String> deleteLore = new ArrayList<>();
        deleteLore.add("§7Click to delete this arena");
        deleteLore.add("§c§lWARNING: This cannot be undone!");
        deleteMeta.setLore(deleteLore);
        deleteItem.setItemMeta(deleteMeta);
        gui.setItem(53, deleteItem);
        
        // Back button
        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.setDisplayName("§7Back to Arena List");
        backItem.setItemMeta(backMeta);
        gui.setItem(49, backItem);
        
        player.openInventory(gui);
    }
    
    public void openArenaList(Player player) {
        String title = "§6Arena List";
        int size = 54;
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        Map<String, Arena> arenas = plugin.getArenaManager().getArenas();
        int slot = 0;
        
        for (Arena arena : arenas.values()) {
            if (slot >= 45) break; // Leave space for navigation
            
            ItemStack arenaItem = new ItemStack(arena.isComplete() ? Material.GREEN_WOOL : Material.RED_WOOL);
            ItemMeta meta = arenaItem.getItemMeta();
            meta.setDisplayName("§e" + arena.getName());
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Status: " + (arena.isComplete() ? "§aComplete" : "§cIncomplete"));
            lore.add("§7Allowed Kits: §f" + arena.getAllowedKits().size());
            lore.add("§7");
            lore.add("§eClick to edit this arena");
            meta.setLore(lore);
            
            arenaItem.setItemMeta(meta);
            gui.setItem(slot, arenaItem);
            slot++;
        }
        
        // Create new arena button
        ItemStack createItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta createMeta = createItem.getItemMeta();
        createMeta.setDisplayName("§aCreate New Arena");
        List<String> createLore = new ArrayList<>();
        createLore.add("§7Click to create a new arena");
        createMeta.setLore(createLore);
        createItem.setItemMeta(createMeta);
        gui.setItem(49, createItem);
        
        player.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        if (title.equals("§6Arena List")) {
            event.setCancelled(true);
            handleArenaListClick(player, event);
        } else if (title.contains("Arena Editor")) {
            event.setCancelled(true);
            handleEditorClick(player, event, title);
        }
    }
    
    private void handleArenaListClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        if (clicked.getType() == Material.LIME_WOOL) {
            // Create new arena
            player.closeInventory();
            player.sendMessage("§aPlease type the name for the new arena in chat:");
            // TODO: Implement chat input handler for arena creation
        } else if (clicked.getType() == Material.GREEN_WOOL || clicked.getType() == Material.RED_WOOL) {
            // Edit existing arena
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                String arenaName = meta.getDisplayName().substring(2); // Remove color code
                Arena arena = plugin.getArenaManager().getArena(arenaName);
                if (arena != null) {
                    openArenaEditor(player, arena);
                }
            }
        }
    }
    
    private void handleEditorClick(Player player, InventoryClickEvent event, String title) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        // Extract arena name from title
        String arenaName = title.replace("§6Arena Editor - ", "").replace("Arena Editor", "").trim();
        if (arenaName.startsWith("§6")) {
            arenaName = arenaName.substring(2);
        }
        
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) return;
        
        handleEditorButtonClick(player, clicked, arena);
    }
    
    private void handleEditorButtonClick(Player player, ItemStack clicked, Arena arena) {
        Location playerLoc = player.getLocation();
        
        switch (clicked.getType()) {
            case RED_BED:
                arena.setSpawn1(playerLoc);
                player.sendMessage("§aSpawn 1 set to your current location!");
                break;
                
            case BLUE_BED:
                arena.setSpawn2(playerLoc);
                player.sendMessage("§aSpawn 2 set to your current location!");
                break;
                
            case BEACON:
                arena.setCenter(playerLoc);
                player.sendMessage("§aCenter set to your current location!");
                break;
                
            case STONE_BUTTON:
                ItemMeta meta = clicked.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    if (meta.getDisplayName().contains("Min")) {
                        arena.setMinCorner(playerLoc);
                        player.sendMessage("§aMin corner set to your current location!");
                    } else if (meta.getDisplayName().contains("Max")) {
                        arena.setMaxCorner(playerLoc);
                        player.sendMessage("§aMax corner set to your current location!");
                    }
                }
                break;
                
            case CHEST:
                player.closeInventory();
                plugin.getAllowedKitsGui().openAllowedKitsGui(player, arena);
                return;
                
            case EMERALD:
                plugin.getArenaManager().saveArenas();
                player.sendMessage("§aArena saved successfully!");
                break;
                
            case REDSTONE:
                plugin.getArenaManager().deleteArena(arena.getName());
                player.sendMessage("§cArena deleted!");
                player.closeInventory();
                return;
                
            case ARROW:
                openArenaList(player);
                return;
        }
        
        // Refresh the GUI
        openArenaEditor(player, arena);
    }
}
