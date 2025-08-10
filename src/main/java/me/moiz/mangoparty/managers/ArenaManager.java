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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {
    private MangoParty plugin;
    private Map<String, Arena> arenas;
    private Set<String> reservedArenas;
    private YamlConfiguration arenasConfig;
    private File arenasFile;
    private int instanceCounter = 0;
    
    public ArenaManager(MangoParty plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.reservedArenas = ConcurrentHashMap.newKeySet();
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
    
    public void saveArena(Arena arena) {
        if (arena != null) {
            arenas.put(arena.getName(), arena);
            saveArenas();
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
    
    public Arena createArena(String name, String worldName) {
        if (arenas.containsKey(name)) {
            return null;
        }
        
        Arena arena = new Arena(name, worldName);
        arenas.put(name, arena);
        saveArenas();
        return arena;
    }
    
    public boolean deleteArena(String name) {
        Arena arena = arenas.remove(name);
        if (arena != null) {
            reservedArenas.remove(name);
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
            if (arena.isComplete() && !reservedArenas.contains(arena.getName())) {
                return arena;
            }
        }
        return null;
    }
    
    public Arena getAvailableArenaForKit(String kitName) {
        for (Arena arena : arenas.values()) {
            if (arena.isComplete() && !reservedArenas.contains(arena.getName()) && arena.isKitAllowed(kitName)) {
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
        
        Arena instance = new Arena(instanceName, baseArena.getWorldName());
        instance.setInstance(true);
        instance.setBaseArena(baseArena.getName());
        instance.setSchematicName(baseArena.getSchematicName());
        instance.setInstanceNumber(instanceCounter);
        instance.setAllowedKits(new ArrayList<>(baseArena.getAllowedKits()));
        
        // Calculate offset position (200 blocks away)
        Location baseCenter = baseArena.getCenter();
        if (baseCenter == null) {
            plugin.getLogger().warning("Base arena has no center location");
            return null;
        }
        
        // Calculate offset based on instance number to avoid overlaps
        double offsetX = 200.0 * instanceCounter;
        double offsetZ = 0.0;
        
        Location instanceCenter = baseCenter.clone().add(offsetX, 0, offsetZ);
        instance.setCenter(instanceCenter);
        
        // Calculate all positions relative to the new center
        if (baseArena.getCorner1() != null && baseArena.getCorner2() != null) {
            Location corner1Offset = baseArena.getCorner1Offset();
            Location corner2Offset = baseArena.getCorner2Offset();
            
            if (corner1Offset != null && corner2Offset != null) {
                instance.setCorner1(instanceCenter.clone().add(corner1Offset.getX(), corner1Offset.getY(), corner1Offset.getZ()));
                instance.setCorner2(instanceCenter.clone().add(corner2Offset.getX(), corner2Offset.getY(), corner2Offset.getZ()));
            }
        }
        
        if (baseArena.getSpawn1() != null) {
            Location spawn1Offset = baseArena.getSpawn1Offset();
            if (spawn1Offset != null) {
                Location newSpawn1 = instanceCenter.clone().add(spawn1Offset.getX(), spawn1Offset.getY(), spawn1Offset.getZ());
                newSpawn1.setYaw(spawn1Offset.getYaw());
                newSpawn1.setPitch(spawn1Offset.getPitch());
                instance.setSpawn1(newSpawn1);
            }
        }
        
        if (baseArena.getSpawn2() != null) {
            Location spawn2Offset = baseArena.getSpawn2Offset();
            if (spawn2Offset != null) {
                Location newSpawn2 = instanceCenter.clone().add(spawn2Offset.getX(), spawn2Offset.getY(), spawn2Offset.getZ());
                newSpawn2.setYaw(spawn2Offset.getYaw());
                newSpawn2.setPitch(spawn2Offset.getPitch());
                instance.setSpawn2(newSpawn2);
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
    
    public boolean saveSchematic(Arena arena) {
        if (arena == null || !arena.isComplete()) {
            return false;
        }
        
        // This would integrate with WorldEdit to save schematics
        // For now, just set the schematic name and return true
        arena.setSchematicName(arena.getName());
        saveArena(arena);
        plugin.getLogger().info("Saved schematic for arena: " + arena.getName());
        return true;
    }
    
    public void reserveArena(String arenaName) {
        reservedArenas.add(arenaName);
        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            arena.setInUse(true);
        }
        plugin.getLogger().info("Reserved arena: " + arenaName);
    }
    
    public void releaseArena(String arenaName) {
        reservedArenas.remove(arenaName);
        Arena arena = arenas.get(arenaName);
        if (arena != null) {
            arena.setInUse(false);
        }
        plugin.getLogger().info("Released arena: " + arenaName);
    }
    
    public boolean isArenaReserved(String arenaName) {
        return reservedArenas.contains(arenaName);
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
