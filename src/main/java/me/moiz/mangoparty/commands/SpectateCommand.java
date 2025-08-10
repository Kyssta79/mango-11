package me.moiz.mangoparty.commands;

import me.moiz.mangoparty.MangoParty;
import me.moiz.mangoparty.models.Match;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpectateCommand implements CommandExecutor {
    private MangoParty plugin;
    
    public SpectateCommand(MangoParty plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage("§cUsage: /spectate <player>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cPlayer not found or not online!");
            return true;
        }
        
        if (target.equals(player)) {
            player.sendMessage("§cYou cannot spectate yourself!");
            return true;
        }
        
        // Check if target is in a match
        Match targetMatch = plugin.getMatchManager().getPlayerMatch(target);
        if (targetMatch == null) {
            player.sendMessage("§cThat player is not in a match!");
            return true;
        }
        
        // Check if target is alive in the match
        if (!targetMatch.isPlayerAlive(target.getUniqueId())) {
            player.sendMessage("§cThat player is not alive in their match!");
            return true;
        }
        
        // Check if spectator is in a different match (prevent cheating)
        Match spectatorMatch = plugin.getMatchManager().getPlayerMatch(player);
        if (spectatorMatch != null && !spectatorMatch.equals(targetMatch)) {
            player.sendMessage("§cYou cannot spectate players in other matches while you're in a match!");
            return true;
        }
        
        // If spectator is in the same match, they can spectate
        // If spectator is not in any match, they can spectate anyone
        
        // Set spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(target.getLocation());
        
        player.sendMessage("§aYou are now spectating " + target.getName());
        
        return true;
    }
}
