package me.moiz.mangoparty.models;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class Arena {
    private String name;
    private String worldName;
    private Location corner1;
    private Location corner2;
    private Location spawn1;
    private Location spawn2;
    private Location spectatorSpawn;
    private List<String> allowedKits;
    private boolean inUse;
    private String schematicName;
    private boolean isInstance;
    private String baseArena;
    
    public Arena(String name) {
        this.name = name;
        this.allowedKits = new ArrayList<>();
        this.inUse = false;
        this.isInstance = false;
    }
    
    // Basic getters and setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }
    
    public World getWorld() {
        return worldName != null ? Bukkit.getWorld(worldName) : null;
    }
    
    public Location getCorner1() {
        return corner1;
    }
    
    public void setCorner1(Location corner1) {
        this.corner1 = corner1;
        if (corner1 != null) {
            this.worldName = corner1.getWorld().getName();
        }
    }
    
    public Location getCorner2() {
        return corner2;
    }
    
    public void setCorner2(Location corner2) {
        this.corner2 = corner2;
        if (corner2 != null) {
            this.worldName = corner2.getWorld().getName();
        }
    }
    
    public Location getSpawn1() {
        return spawn1;
    }
    
    public void setSpawn1(Location spawn1) {
        this.spawn1 = spawn1;
    }
    
    public Location getSpawn2() {
        return spawn2;
    }
    
    public void setSpawn2(Location spawn2) {
        this.spawn2 = spawn2;
    }
    
    public Location getSpectatorSpawn() {
        return spectatorSpawn;
    }
    
    public void setSpectatorSpawn(Location spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }
    
    public List<String> getAllowedKits() {
        return allowedKits;
    }
    
    public void setAllowedKits(List<String> allowedKits) {
        this.allowedKits = allowedKits != null ? allowedKits : new ArrayList<>();
    }
    
    public void addAllowedKit(String kitName) {
        if (!allowedKits.contains(kitName)) {
            allowedKits.add(kitName);
        }
    }
    
    public void removeAllowedKit(String kitName) {
        allowedKits.remove(kitName);
    }
    
    public boolean isKitAllowed(String kitName) {
        return allowedKits.contains(kitName);
    }
    
    public boolean isInUse() {
        return inUse;
    }
    
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
    
    public String getSchematicName() {
        return schematicName;
    }
    
    public void setSchematicName(String schematicName) {
        this.schematicName = schematicName;
    }
    
    public boolean isInstance() {
        return isInstance;
    }
    
    public void setInstance(boolean instance) {
        this.isInstance = instance;
    }
    
    public String getBaseArena() {
        return baseArena;
    }
    
    public void setBaseArena(String baseArena) {
        this.baseArena = baseArena;
    }
    
    public boolean isComplete() {
        return corner1 != null && corner2 != null && spawn1 != null && spawn2 != null;
    }
    
    public Location getCenter() {
        if (corner1 == null || corner2 == null) return null;
        
        double x = (corner1.getX() + corner2.getX()) / 2;
        double y = (corner1.getY() + corner2.getY()) / 2;
        double z = (corner1.getZ() + corner2.getZ()) / 2;
        
        return new Location(corner1.getWorld(), x, y, z);
    }
    
    public void saveToConfig(ConfigurationSection section) {
        section.set("worldName", worldName);
        section.set("isInstance", isInstance);
        section.set("baseArena", baseArena);
        section.set("schematicName", schematicName);
        
        if (corner1 != null) {
            section.set("corner1.x", corner1.getX());
            section.set("corner1.y", corner1.getY());
            section.set("corner1.z", corner1.getZ());
        }
        
        if (corner2 != null) {
            section.set("corner2.x", corner2.getX());
            section.set("corner2.y", corner2.getY());
            section.set("corner2.z", corner2.getZ());
        }
        
        if (spawn1 != null) {
            section.set("spawn1.x", spawn1.getX());
            section.set("spawn1.y", spawn1.getY());
            section.set("spawn1.z", spawn1.getZ());
            section.set("spawn1.yaw", spawn1.getYaw());
            section.set("spawn1.pitch", spawn1.getPitch());
        }
        
        if (spawn2 != null) {
            section.set("spawn2.x", spawn2.getX());
            section.set("spawn2.y", spawn2.getY());
            section.set("spawn2.z", spawn2.getZ());
            section.set("spawn2.yaw", spawn2.getYaw());
            section.set("spawn2.pitch", spawn2.getPitch());
        }
        
        if (spectatorSpawn != null) {
            section.set("spectatorSpawn.x", spectatorSpawn.getX());
            section.set("spectatorSpawn.y", spectatorSpawn.getY());
            section.set("spectatorSpawn.z", spectatorSpawn.getZ());
            section.set("spectatorSpawn.yaw", spectatorSpawn.getYaw());
            section.set("spectatorSpawn.pitch", spectatorSpawn.getPitch());
        }
        
        section.set("allowedKits", allowedKits);
    }
    
    public static Arena loadFromConfig(String name, ConfigurationSection section) {
        Arena arena = new Arena(name);
        
        arena.worldName = section.getString("worldName");
        arena.isInstance = section.getBoolean("isInstance", false);
        arena.baseArena = section.getString("baseArena");
        arena.schematicName = section.getString("schematicName");
        
        World world = arena.getWorld();
        if (world != null) {
            if (section.contains("corner1")) {
                arena.corner1 = new Location(world,
                    section.getDouble("corner1.x"),
                    section.getDouble("corner1.y"),
                    section.getDouble("corner1.z"));
            }
            
            if (section.contains("corner2")) {
                arena.corner2 = new Location(world,
                    section.getDouble("corner2.x"),
                    section.getDouble("corner2.y"),
                    section.getDouble("corner2.z"));
            }
            
            if (section.contains("spawn1")) {
                arena.spawn1 = new Location(world,
                    section.getDouble("spawn1.x"),
                    section.getDouble("spawn1.y"),
                    section.getDouble("spawn1.z"),
                    (float) section.getDouble("spawn1.yaw"),
                    (float) section.getDouble("spawn1.pitch"));
            }
            
            if (section.contains("spawn2")) {
                arena.spawn2 = new Location(world,
                    section.getDouble("spawn2.x"),
                    section.getDouble("spawn2.y"),
                    section.getDouble("spawn2.z"),
                    (float) section.getDouble("spawn2.yaw"),
                    (float) section.getDouble("spawn2.pitch"));
            }
            
            if (section.contains("spectatorSpawn")) {
                arena.spectatorSpawn = new Location(world,
                    section.getDouble("spectatorSpawn.x"),
                    section.getDouble("spectatorSpawn.y"),
                    section.getDouble("spectatorSpawn.z"),
                    (float) section.getDouble("spectatorSpawn.yaw"),
                    (float) section.getDouble("spectatorSpawn.pitch"));
            }
        }
        
        arena.allowedKits = section.getStringList("allowedKits");
        if (arena.allowedKits == null) {
            arena.allowedKits = new ArrayList<>();
        }
        
        return arena;
    }
}
