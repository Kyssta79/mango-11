package me.moiz.mangoparty;

import me.moiz.mangoparty.commands.*;
import me.moiz.mangoparty.config.ConfigManager;
import me.moiz.mangoparty.gui.*;
import me.moiz.mangoparty.listeners.*;
import me.moiz.mangoparty.managers.*;
import me.moiz.mangoparty.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public final class MangoParty extends JavaPlugin {
    private PartyManager partyManager;
    private KitManager kitManager;
    private ArenaManager arenaManager;
    private MatchManager matchManager;
    private QueueManager queueManager;
    private PartyDuelManager partyDuelManager;
    private ScoreboardManager scoreboardManager;
    private ConfigManager configManager;
    private GuiManager guiManager;
    private KitEditorGui kitEditorGui;
    private ArenaEditorGui arenaEditorGui;
    private AllowedKitsGui allowedKitsGui;
    private SpectatorListener spectatorListener;
    private Location spawnLocation;

    @Override
    public void onEnable() {
        // Initialize managers
        configManager = new ConfigManager(this);
        partyManager = new PartyManager(this);
        kitManager = new KitManager(this);
        arenaManager = new ArenaManager(this);
        matchManager = new MatchManager(this);
        queueManager = new QueueManager(this);
        partyDuelManager = new PartyDuelManager(this);
        scoreboardManager = new ScoreboardManager(this);
        
        // Initialize GUIs
        guiManager = new GuiManager(this);
        kitEditorGui = new KitEditorGui(this);
        arenaEditorGui = new ArenaEditorGui(this);
        allowedKitsGui = new AllowedKitsGui(this);
        
        // Initialize listeners
        spectatorListener = new SpectatorListener(this);
        
        // Load configurations
        configManager.loadConfigs();
        
        // Register commands
        registerCommands();
        
        // Register listeners
        registerListeners();
        
        // Load data
        kitManager.loadKits();
        arenaManager.loadArenas();
        
        getLogger().info(HexUtils.colorize("&#00FF00MangoParty has been enabled!"));
    }

    @Override
    public void onDisable() {
        // Save data
        if (kitManager != null) {
            kitManager.saveKits();
        }
        if (arenaManager != null) {
            arenaManager.saveArenas();
        }
        
        // Cleanup matches
        if (matchManager != null) {
            matchManager.cleanup();
        }
        
        // Cleanup queues
        if (queueManager != null) {
            queueManager.cleanup();
        }
        
        getLogger().info(HexUtils.colorize("&#FF0000MangoParty has been disabled!"));
    }
    
    private void registerCommands() {
        // Party command
        getCommand("party").setExecutor(new PartyCommand(this));
        getCommand("party").setTabCompleter(new PartyTabCompleter(this));
        
        // Queue commands
        getCommand("queue").setExecutor(new QueueCommand(this));
        getCommand("queue").setTabCompleter(new QueueTabCompleter(this));
        getCommand("leavequeue").setExecutor(new LeaveQueueCommand(this));
        
        // Spectate command
        getCommand("spectate").setExecutor(new SpectateCommand(this));
        getCommand("spectate").setTabCompleter(new SpectateTabCompleter(this));
        
        // Admin command
        getCommand("mango").setExecutor(new MangoCommand(this));
        getCommand("mango").setTabCompleter(new MangoTabCompleter(this));
    }
    
    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerRespawnListener(this), this);
        Bukkit.getPluginManager().registerEvents(new KitRulesListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ArenaBoundsListener(this), this);
        Bukkit.getPluginManager().registerEvents(spectatorListener, this);
    }
    
    // Getters
    public PartyManager getPartyManager() {
        return partyManager;
    }
    
    public KitManager getKitManager() {
        return kitManager;
    }
    
    public ArenaManager getArenaManager() {
        return arenaManager;
    }
    
    public MatchManager getMatchManager() {
        return matchManager;
    }
    
    public QueueManager getQueueManager() {
        return queueManager;
    }
    
    public PartyDuelManager getPartyDuelManager() {
        return partyDuelManager;
    }
    
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public GuiManager getGuiManager() {
        return guiManager;
    }
    
    public KitEditorGui getKitEditorGui() {
        return kitEditorGui;
    }
    
    public ArenaEditorGui getArenaEditorGui() {
        return arenaEditorGui;
    }
    
    public AllowedKitsGui getAllowedKitsGui() {
        return allowedKitsGui;
    }
    
    public SpectatorListener getSpectatorListener() {
        return spectatorListener;
    }
    
    public Location getSpawnLocation() {
        return spawnLocation;
    }
    
    public void setSpawnLocation(Location spawnLocation) {
        this.spawnLocation = spawnLocation;
    }
}
