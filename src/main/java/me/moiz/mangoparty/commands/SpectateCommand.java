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
            sender.sendMessage("§cThis command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length != 1) {
            player.sendMessage("§cUsage: /spectate <player>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§cPlayer not found or not online!");
            return true;
        }
        
        // Check if target is in a match
        Match targetMatch = plugin.getMatchManager().getPlayerMatch(target);
        if (targetMatch == null) {
            player.sendMessage("§c" + target.getName() + " is not currently in a match!");
            return true;
        }
        
        // Check if target is alive in the match
        if (!targetMatch.isPlayerAlive(target.getUniqueId())) {
            player.sendMessage("§c" + target.getName() + " is not alive in their match!");
            return true;
        }
        
        // Check if spectator is in a different match (prevent cross-match spectating for anti-cheat)
        Match spectatorMatch = plugin.getMatchManager().getPlayerMatch(player);
        if (spectatorMatch != null && !spectatorMatch.equals(targetMatch)) {
            player.sendMessage("§cYou cannot spectate players in other matches while you're in a match!");
            return true;
        }
        
        // Set spectator mode and teleport
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(target.getLocation());
        player.setSpectatorTarget(target);
        
        player.sendMessage("§aYou are now spectating " + target.getName() + "!");
        
        return true;
    }
}
