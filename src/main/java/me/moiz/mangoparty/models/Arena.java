package me.moiz.mangoparty.models;

import org.bukkit.Location;
import java.util.ArrayList;
import java.util.List;

public class Arena {
    private String name;
    private String worldName;
    private Location corner1;
    private Location corner2;
    private Location center;
    private Location spawn1;
    private Location spawn2;
    private List<String> allowedKits;
    private boolean isInstance;
    private String baseArena;
    private int instanceNumber;
    private double xOffset;
    private double zOffset;
    private boolean inUse;
    
    public Arena(String name, String worldName) {
        this.name = name;
        this.worldName = worldName;
        this.allowedKits = new ArrayList<>();
        this.isInstance = false;
        this.baseArena = null;
        this.instanceNumber = 0;
        this.xOffset = 0;
        this.zOffset = 0;
        this.inUse = false;
    }
    
    public String getName() {
        return name;
    }
    
    public String getWorldName() {
        return worldName;
    }
    
    public String getWorld() {
        return worldName;
    }
    
    public Location getCorner1() {
        return corner1;
    }
    
    public void setCorner1(Location corner1) {
        this.corner1 = corner1;
    }
    
    public Location getCorner2() {
        return corner2;
    }
    
    public void setCorner2(Location corner2) {
        this.corner2 = corner2;
    }
    
    public Location getCenter() {
        return center;
    }
    
    public void setCenter(Location center) {
        this.center = center;
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
    
    public boolean isComplete() {
        return corner1 != null && corner2 != null && center != null && 
               spawn1 != null && spawn2 != null;
    }
    
    public List<String> getAllowedKits() {
        return allowedKits;
    }
    
    public void setAllowedKits(List<String> allowedKits) {
        this.allowedKits = allowedKits;
    }
    
    public boolean isKitAllowed(String kitName) {
        return allowedKits.contains(kitName);
    }
    
    public void addAllowedKit(String kitName) {
        if (!allowedKits.contains(kitName)) {
            allowedKits.add(kitName);
        }
    }
    
    public void removeAllowedKit(String kitName) {
        allowedKits.remove(kitName);
    }
    
    public boolean isInstance() {
        return isInstance;
    }
    
    public void setInstance(boolean instance) {
        isInstance = instance;
    }
    
    public String getBaseArena() {
        return baseArena;
    }
    
    public void setBaseArena(String baseArena) {
        this.baseArena = baseArena;
    }
    
    public String getOriginalArena() {
        return baseArena;
    }
    
    public void setOriginalArena(String originalArena) {
        this.baseArena = originalArena;
    }
    
    public int getInstanceNumber() {
        return instanceNumber;
    }
    
    public void setInstanceNumber(int instanceNumber) {
        this.instanceNumber = instanceNumber;
    }
    
    public double getXOffset() {
        return xOffset;
    }
    
    public void setXOffset(double xOffset) {
        this.xOffset = xOffset;
    }
    
    public double getZOffset() {
        return zOffset;
    }
    
    public void setZOffset(double zOffset) {
        this.zOffset = zOffset;
    }
    
    public boolean isInUse() {
        return inUse;
    }
    
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }
    
    // Calculate relative offsets from center
    public Location getSpawn1Offset() {
        if (spawn1 == null || center == null) return null;
        return new Location(
            spawn1.getWorld(),
            spawn1.getX() - center.getX(),
            spawn1.getY() - center.getY(),
            spawn1.getZ() - center.getZ(),
            spawn1.getYaw(),
            spawn1.getPitch()
        );
    }
    
    public Location getSpawn2Offset() {
        if (spawn2 == null || center == null) return null;
        return new Location(
            spawn2.getWorld(),
            spawn2.getX() - center.getX(),
            spawn2.getY() - center.getY(),
            spawn2.getZ() - center.getZ(),
            spawn2.getYaw(),
            spawn2.getPitch()
        );
    }
    
    public Location getCorner1Offset() {
        if (corner1 == null || center == null) return null;
        return new Location(
            corner1.getWorld(),
            corner1.getX() - center.getX(),
            corner1.getY() - center.getY(),
            corner1.getZ() - center.getZ()
        );
    }
    
    public Location getCorner2Offset() {
        if (corner2 == null || center == null) return null;
        return new Location(
            corner2.getWorld(),
            corner2.getX() - center.getX(),
            corner2.getY() - center.getY(),
            corner2.getZ() - center.getZ()
        );
    }
}
