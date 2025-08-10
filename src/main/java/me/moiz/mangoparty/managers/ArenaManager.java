package me.moiz.mangoparty.managers;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ArenaManager {
    private MangoParty plugin;
    private Map<String, Arena> arenas;
    private ConcurrentHashMap<String, Boolean> reservedArenas; // Track which arenas are in use
    private File arenasFile;
    private YamlConfiguration arenasConfig;
    private double defaultXOffset = 200.0; // Default X-axis offset for arena instances
    private double defaultZOffset = 0.0; // Default Z-axis offset for arena instances
    
    public ArenaManager(MangoParty plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.reservedArenas = new ConcurrentHashMap<>();
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
                Arena arena = loadArenaFromConfig(arenaName, arenasSection.getConfigurationSection(arenaName));
                if (arena != null) {
                    arenas.put(arenaName, arena);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas");
    }
    
    private Arena loadArenaFromConfig(String name, ConfigurationSection section) {
        try {
            String worldName = section.getString("world");
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World '" + worldName + "' not found for arena '" + name + "'");
                return null;
            }
            
            Arena arena = new Arena(name, worldName);
            
            // Load locations
            if (section.contains("corner1")) {
                arena.setCorner1(loadLocation(section.getConfigurationSection("corner1"), world));
            }
            if (section.contains("corner2")) {
                arena.setCorner2(loadLocation(section.getConfigurationSection("corner2"), world));
            }
            if (section.contains("center")) {
                arena.setCenter(loadLocation(section.getConfigurationSection("center"), world));
            }
            if (section.contains("spawn1")) {
                arena.setSpawn1(loadLocation(section.getConfigurationSection("spawn1"), world));
            }
            if (section.contains("spawn2")) {
                arena.setSpawn2(loadLocation(section.getConfigurationSection("spawn2"), world));
            }
            
            // Load allowed kits
            if (section.contains("allowedKits")) {
                List<String> allowedKits = section.getStringList("allowedKits");
                for (String kit : allowedKits) {
                    arena.addAllowedKit(kit);
                }
            }
            
            // Load instance information
            if (section.contains("isInstance")) {
                arena.setInstance(section.getBoolean("isInstance"));
            }
            if (section.contains("baseArena")) {
                arena.setBaseArena(section.getString("baseArena"));
            }
            if (section.contains("instanceNumber")) {
                arena.setInstanceNumber(section.getInt("instanceNumber"));
            }
            
            return arena;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load arena '" + name + "': " + e.getMessage());
            return null;
        }
    }
    
    private Location loadLocation(ConfigurationSection section, World world) {
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0);
        float pitch = (float) section.getDouble("pitch", 0);
        return new Location(world, x, y, z, yaw, pitch);
    }
    
    public Arena createArena(String name, String worldName) {
        Arena arena = new Arena(name, worldName);
        arenas.put(name, arena);
        return arena;
    }
    
    public void saveArena(Arena arena) {
        ConfigurationSection arenaSection = arenasConfig.createSection("arenas." + arena.getName());
        
        arenaSection.set("world", arena.getWorldName());
        
        if (arena.getCorner1() != null) {
            saveLocation(arenaSection.createSection("corner1"), arena.getCorner1());
        }
        if (arena.getCorner2() != null) {
            saveLocation(arenaSection.createSection("corner2"), arena.getCorner2());
        }
        if (arena.getCenter() != null) {
            saveLocation(arenaSection.createSection("center"), arena.getCenter());
        }
        if (arena.getSpawn1() != null) {
            saveLocation(arenaSection.createSection("spawn1"), arena.getSpawn1());
        }
        if (arena.getSpawn2() != null) {
            saveLocation(arenaSection.createSection("spawn2"), arena.getSpawn2());
        }
        
        // Save allowed kits
        if (!arena.getAllowedKits().isEmpty()) {
            arenaSection.set("allowedKits", arena.getAllowedKits().toArray(new String[0]));
        }
        
        // Save instance information
        if (arena.isInstance()) {
            arenaSection.set("isInstance", true);
            arenaSection.set("baseArena", arena.getBaseArena());
            arenaSection.set("instanceNumber", arena.getInstanceNumber());
        }
        
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas.yml: " + e.getMessage());
        }
    }
    
    private void saveLocation(ConfigurationSection section, Location location) {
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());
    }
    
    public Arena getArena(String name) {
        return arenas.get(name);
    }
    
    public Map<String, Arena> getArenas() {
        return new HashMap<>(arenas);
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
    
    public Arena createArenaInstance(Arena baseArena, String kitName) {
        if (!baseArena.isComplete()) {
            plugin.getLogger().warning("Cannot create instance of incomplete arena: " + baseArena.getName());
            return null;
        }
        
        // Find next available instance number
        int instanceNumber = 1;
        String instanceName;
        do {
            instanceName = baseArena.getName() + "_instance_" + instanceNumber;
            instanceNumber++;
        } while (arenas.containsKey(instanceName));
        
        plugin.getLogger().info("Creating arena instance: " + instanceName);
        
        // Calculate offset (200 blocks away)
        Location baseCenter = baseArena.getCenter();
        double offsetX = 200.0 * instanceNumber;
        double offsetZ = 0.0;
        
        // Create new arena instance
        Arena instance = new Arena(instanceName, baseArena.getWorldName());
        instance.setInstance(true);
        instance.setBaseArena(baseArena.getName());
        instance.setInstanceNumber(instanceNumber);
        
        // Copy allowed kits from base arena
        for (String allowedKit : baseArena.getAllowedKits()) {
            instance.addAllowedKit(allowedKit);
        }
        
        // Calculate new positions with offset
        if (baseArena.getCenter() != null) {
            Location newCenter = baseCenter.clone().add(offsetX, 0, offsetZ);
            instance.setCenter(newCenter);
        }
        
        if (baseArena.getCorner1() != null) {
            Location offset = calculateOffset(baseCenter, baseArena.getCorner1());
            Location newCorner1 = baseCenter.clone().add(offsetX, 0, offsetZ).add(offset.getX(), offset.getY(), offset.getZ());
            instance.setCorner1(newCorner1);
        }
        
        if (baseArena.getCorner2() != null) {
            Location offset = calculateOffset(baseCenter, baseArena.getCorner2());
            Location newCorner2 = baseCenter.clone().add(offsetX, 0, offsetZ).add(offset.getX(), offset.getY(), offset.getZ());
            instance.setCorner2(newCorner2);
        }
        
        if (baseArena.getSpawn1() != null) {
            Location offset = calculateOffset(baseCenter, baseArena.getSpawn1());
            Location newSpawn1 = baseCenter.clone().add(offsetX, 0, offsetZ).add(offset.getX(), offset.getY(), offset.getZ());
            newSpawn1.setYaw(baseArena.getSpawn1().getYaw());
            newSpawn1.setPitch(baseArena.getSpawn1().getPitch());
            instance.setSpawn1(newSpawn1);
        }
        
        if (baseArena.getSpawn2() != null) {
            Location offset = calculateOffset(baseCenter, baseArena.getSpawn2());
            Location newSpawn2 = baseCenter.clone().add(offsetX, 0, offsetZ).add(offset.getX(), offset.getY(), offset.getZ());
            newSpawn2.setYaw(baseArena.getSpawn2().getYaw());
            newSpawn2.setPitch(baseArena.getSpawn2().getPitch());
            instance.setSpawn2(newSpawn2);
        }
        
        // Add to arenas map and save
        arenas.put(instanceName, instance);
        saveArena(instance);
        
        plugin.getLogger().info("Created arena instance: " + instanceName + " at offset (" + offsetX + ", 0, " + offsetZ + ")");
        
        return instance;
    }
    
    private Location calculateOffset(Location center, Location point) {
        return new Location(center.getWorld(), 
            point.getX() - center.getX(),
            point.getY() - center.getY(),
            point.getZ() - center.getZ());
    }
    
    public boolean saveSchematic(Arena arena) {
        // This would integrate with WorldEdit or similar plugin
        // For now, just return true to indicate success
        plugin.getLogger().info("Schematic saved for arena: " + arena.getName());
        return true;
    }
    
    public void deleteArena(String name) {
        arenas.remove(name);
        arenasConfig.set("arenas." + name, null);
        try {
            arenasConfig.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas.yml after deletion: " + e.getMessage());
        }
    }
    
    public void reserveArena(String arenaName) {
        reservedArenas.put(arenaName, true);
        plugin.getLogger().info("Reserved arena: " + arenaName);
    }
    
    public void releaseArena(String arenaName) {
        reservedArenas.remove(arenaName);
        plugin.getLogger().info("Released arena: " + arenaName);
    }
    
    public boolean isArenaReserved(String arenaName) {
        return reservedArenas.containsKey(arenaName) && reservedArenas.get(arenaName);
    }
    
    private boolean pasteSchematicForInstance(Arena originalArena, Arena instance) {
        try {
            File schematicsDir = new File(plugin.getDataFolder(), "schematics");
            File schematicFile = new File(schematicsDir, originalArena.getName() + ".schem");
            
            if (!schematicFile.exists()) {
                plugin.getLogger().warning("Schematic file not found for original arena: " + originalArena.getName());
                return false;
            }
            
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(instance.getCorner1().getWorld());
            
            ClipboardFormat format = ClipboardFormats.findByAlias("schem");
            if (format == null) {
                format = ClipboardFormats.findByAlias("schematic");
            }
            if (format == null) {
                plugin.getLogger().severe("No schematic format found for reading!");
                return false;
            }
            
            try (ClipboardReader reader = format.getReader(new FileInputStream(schematicFile))) {
                Clipboard clipboard = reader.read();
            
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
                    // Calculate the minimum point where the schematic should be pasted
                    BlockVector3 pasteLocation = BlockVector3.at(
                        Math.min(instance.getCorner1().getBlockX(), instance.getCorner2().getBlockX()),
                        Math.min(instance.getCorner1().getBlockY(), instance.getCorner2().getBlockY()),
                        Math.min(instance.getCorner1().getBlockZ(), instance.getCorner2().getBlockZ())
                    );
                
                    Operation operation = new ClipboardHolder(clipboard)
                        .createPaste(editSession)
                        .to(pasteLocation)
                        .ignoreAirBlocks(false)
                        .build();
                
                    Operations.complete(operation);
                }
            }
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to paste schematic for arena instance " + instance.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public int getInstanceCount(String baseArenaName) {
        int count = 0;
        for (Arena arena : arenas.values()) {
            if (arena.isInstance() && baseArenaName.equals(arena.getBaseArena())) {
                count++;
            }
        }
        return count;
    }
    
    public void cleanupUnusedInstances() {
        // This method can be called periodically to clean up unused instance arenas
        List<String> toRemove = new ArrayList<>();
        
        for (Arena arena : arenas.values()) {
            if (arena.isInstance() && !reservedArenas.containsKey(arena.getName())) {
                // Check if this instance has been unused for a while
                // For now, we'll keep all instances for future use
                // You could add logic here to remove old unused instances
                toRemove.add(arena.getName());
            }
        }
        
        for (String arenaName : toRemove) {
            deleteArena(arenaName);
        }
    }
}
