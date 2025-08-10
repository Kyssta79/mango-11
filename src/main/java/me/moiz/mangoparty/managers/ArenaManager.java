package me.moiz.mangoparty.managers;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaManager {
    private MangoParty plugin;
    private Map<String, Arena> arenas;
    private YamlConfiguration arenasConfig;
    private File arenasFile;
    private int instanceCounter = 0;
    
    public ArenaManager(MangoParty plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        loadArenas();
    }
    
    private void loadArenas() {
        arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        if (!arenasFile.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
        
        arenasConfig = YamlConfiguration.loadConfiguration(arenasFile);
        
        ConfigurationSection arenasSection = arenasConfig.getConfigurationSection("arenas");
        if (arenasSection != null) {
            for (String arenaName : arenasSection.getKeys(false)) {
                ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaName);
                if (arenaSection != null) {
                    Arena arena = Arena.loadFromConfig(arenaName, arenaSection);
                    arenas.put(arenaName, arena);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas");
    }
    
    public void saveArenas() {
        try {
            ConfigurationSection arenasSection = arenasConfig.createSection("arenas");
            
            for (Arena arena : arenas.values()) {
                ConfigurationSection arenaSection = arenasSection.createSection(arena.getName());
                arena.saveToConfig(arenaSection);
            }
            
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas: " + e.getMessage());
        }
    }
    
    public void reloadArenas() {
        arenas.clear();
        loadArenas();
    }
    
    public Arena createArena(String name) {
        if (arenas.containsKey(name)) {
            return null;
        }
        
        Arena arena = new Arena(name);
        arenas.put(name, arena);
        saveArenas();
        return arena;
    }
    
    public boolean deleteArena(String name) {
        Arena arena = arenas.remove(name);
        if (arena != null) {
            saveArenas();
            return true;
        }
        return false;
    }
    
    public Arena getArena(String name) {
        return arenas.get(name);
    }
    
    public Map<String, Arena> getArenas() {
        return new HashMap<>(arenas);
    }
    
    public List<String> getArenaNames() {
        return new ArrayList<>(arenas.keySet());
    }
    
    public Arena getAvailableArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isComplete() && !arena.isInUse()) {
                return arena;
            }
        }
        return null;
    }
    
    public Arena getAvailableArenaForKit(String kitName) {
        for (Arena arena : arenas.values()) {
            if (arena.isComplete() && !arena.isInUse() && arena.isKitAllowed(kitName)) {
                return arena;
            }
        }
        return null;
    }
    
    public List<Arena> getArenasForKit(String kitName) {
        List<Arena> result = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.isKitAllowed(kitName)) {
                result.add(arena);
            }
        }
        return result;
    }
    
    public Arena createArenaInstance(Arena baseArena, String kitName) {
        if (baseArena == null || !baseArena.isComplete()) {
            plugin.getLogger().warning("Cannot create instance of incomplete arena: " + (baseArena != null ? baseArena.getName() : "null"));
            return null;
        }
        
        instanceCounter++;
        String instanceName = baseArena.getName() + "_instance_" + instanceCounter;
        
        Arena instance = new Arena(instanceName);
        instance.setInstance(true);
        instance.setBaseArena(baseArena.getName());
        instance.setSchematicName(baseArena.getSchematicName());
        instance.setWorldName(baseArena.getWorldName());
        instance.setAllowedKits(new ArrayList<>(baseArena.getAllowedKits()));
        
        // Calculate offset position (200 blocks away)
        Location baseCenter = baseArena.getCenter();
        if (baseCenter == null) {
            plugin.getLogger().warning("Base arena has no center location");
            return null;
        }
        
        // Calculate offset based on instance number to avoid overlaps
        int offsetX = 200 * (instanceCounter % 10);
        int offsetZ = 200 * (instanceCounter / 10);
        
        Location instanceCenter = baseCenter.clone().add(offsetX, 0, offsetZ);
        
        // Calculate all positions relative to the new center
        Location baseCorner1 = baseArena.getCorner1();
        Location baseCorner2 = baseArena.getCorner2();
        Location baseSpawn1 = baseArena.getSpawn1();
        Location baseSpawn2 = baseArena.getSpawn2();
        Location baseSpectatorSpawn = baseArena.getSpectatorSpawn();
        
        if (baseCorner1 != null && baseCorner2 != null) {
            Location offset = instanceCenter.subtract(baseCenter);
            
            instance.setCorner1(baseCorner1.clone().add(offset));
            instance.setCorner2(baseCorner2.clone().add(offset));
            
            if (baseSpawn1 != null) {
                instance.setSpawn1(baseSpawn1.clone().add(offset));
            }
            
            if (baseSpawn2 != null) {
                instance.setSpawn2(baseSpawn2.clone().add(offset));
            }
            
            if (baseSpectatorSpawn != null) {
                instance.setSpectatorSpawn(baseSpectatorSpawn.clone().add(offset));
            }
        }
        
        arenas.put(instanceName, instance);
        saveArenas();
        
        plugin.getLogger().info("Created arena instance: " + instanceName + " at offset (" + offsetX + ", " + offsetZ + ")");
        
        // Paste schematic at new location if available
        pasteSchematic(instance);
        
        return instance;
    }
    
    public void pasteSchematic(Arena arena) {
        if (arena.getSchematicName() == null || arena.getSchematicName().isEmpty()) {
            return;
        }
        
        // This would integrate with WorldEdit or similar plugin to paste schematics
        // For now, just log the action
        plugin.getLogger().info("Would paste schematic " + arena.getSchematicName() + " for arena " + arena.getName());
    }
    
    public void setArenaInUse(String arenaName, boolean inUse) {
        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            arena.setInUse(inUse);
        }
    }
    
    public boolean isLocationInArena(Location location, Arena arena) {
        if (location == null || arena == null || arena.getCorner1() == null || arena.getCorner2() == null) {
            return false;
        }
        
        if (!location.getWorld().equals(arena.getCorner1().getWorld())) {
            return false;
        }
        
        double minX = Math.min(arena.getCorner1().getX(), arena.getCorner2().getX());
        double maxX = Math.max(arena.getCorner1().getX(), arena.getCorner2().getX());
        double minY = Math.min(arena.getCorner1().getY(), arena.getCorner2().getY());
        double maxY = Math.max(arena.getCorner1().getY(), arena.getCorner2().getY());
        double minZ = Math.min(arena.getCorner1().getZ(), arena.getCorner2().getZ());
        double maxZ = Math.max(arena.getCorner1().getZ(), arena.getCorner2().getZ());
        
        return location.getX() >= minX && location.getX() <= maxX &&
               location.getY() >= minY && location.getY() <= maxY &&
               location.getZ() >= minZ && location.getZ() <= maxZ;
    }
    
    public Arena getArenaByLocation(Location location) {
        for (Arena arena : arenas.values()) {
            if (isLocationInArena(location, arena)) {
                return arena;
            }
        }
        return null;
    }
    
    public List<Arena> getCompleteArenas() {
        List<Arena> complete = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.isComplete()) {
                complete.add(arena);
            }
        }
        return complete;
    }
    
    public List<Arena> getIncompleteArenas() {
        List<Arena> incomplete = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (!arena.isComplete()) {
                incomplete.add(arena);
            }
        }
        return incomplete;
    }
    
    public void cleanupInstances() {
        List<String> instancesToRemove = new ArrayList<>();
        
        for (Arena arena : arenas.values()) {
            if (arena.isInstance() && !arena.isInUse()) {
                // Check if base arena still exists
                String baseArenaName = arena.getBaseArena();
                if (baseArenaName == null || !arenas.containsKey(baseArenaName)) {
                    instancesToRemove.add(arena.getName());
                }
            }
        }
        
        for (String instanceName : instancesToRemove) {
            arenas.remove(instanceName);
            plugin.getLogger().info("Cleaned up orphaned arena instance: " + instanceName);
        }
        
        if (!instancesToRemove.isEmpty()) {
            saveArenas();
        }
    }
}
