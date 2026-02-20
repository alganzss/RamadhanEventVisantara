package my.pikrew.ramadhanEvent.commands;

import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.manager.CrateManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class CrateCommand implements CommandExecutor, TabCompleter {

    private final RamadhanEvent plugin;
    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "spawn", "spawnauto", "remove", "status", "reload", "help"
    );

    public CrateCommand(RamadhanEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendMessage(sender, "&e&l--- Crate Commands ---");
            sendMessage(sender, "&b/" + label + " spawn &7- Spawn a crate at your location.");
            sendMessage(sender, "&b/" + label + " spawnauto &7- Trigger the auto-spawn task now.");
            sendMessage(sender, "&b/" + label + " remove &7- Remove all active crates.");
            sendMessage(sender, "&b/" + label + " status &7- Show active crates & next spawn time.");
            sendMessage(sender, "&b/" + label + " reload &7- Reload plugin configuration.");
            sendMessage(sender, "&b/ramadhanwand &7- Open the region wand tool.");
            return true;
        }

        if (!sender.hasPermission("ramadhan.crate.admin")) {
            sendMessage(sender, "&cYou don't have permission to use this command.");
            return true;
        }

        CrateManager cm = plugin.getCrateManager();

        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                if (sender instanceof Player player) {
                    UUID id = cm.spawnCrateAt(player.getLocation());
                    if (id != null) sendMessage(sender, "&aCrate spawned at your location.");
                    else            sendMessage(sender, "&cFailed to spawn crate at your location.");
                } else {
                    UUID id = cm.spawnRandomCrate();
                    if (id != null) sendMessage(sender, "&aCrate spawned at a random location.");
                    else            sendMessage(sender, "&cFailed to find a valid spawn location.");
                }
            }

            case "spawnauto" -> {
                cm.manualAutoSpawnOnce();
                sendMessage(sender, "&aAuto-spawn triggered!");
            }

            case "remove" -> {
                cm.cleanupAll();
                sendMessage(sender, "&aAll active crates removed.");
            }

            case "status" -> {
                sendMessage(sender, "&7Active crates: &a" + cm.getActiveCrates().size());

                if (sender instanceof Player player) {
                    for (CrateManager.CrateData data : cm.getActiveCrates().values()) {
                        Location loc = data.getLocation();
                        if (loc != null) sendClickableTp(player, loc);
                    }
                } else {
                    for (CrateManager.CrateData data : cm.getActiveCrates().values()) {
                        Location loc = data.getLocation();
                        if (loc != null) {
                            sendMessage(sender, "&7 - " + loc.getWorld().getName()
                                    + " " + loc.getBlockX()
                                    + " " + loc.getBlockY()
                                    + " " + loc.getBlockZ());
                        }
                    }
                }
                sendMessage(sender, "&7" + cm.getActiveCratesStatus());
            }

            case "reload" -> {
                plugin.reloadConfig();
                plugin.getMessageUtil().reloadMessages();
                plugin.getTimeManager().reload();
                plugin.getDisplayManager().reload();
                sendMessage(sender, "&aAll configurations reloaded!");
            }

            case "_tp" -> {
                if (!(sender instanceof Player player) || args.length < 5) return true;
                try {
                    World world = Bukkit.getWorld(args[1]);
                    if (world == null) { sendMessage(sender, "&cWorld not found: " + args[1]); return true; }
                    double x = Double.parseDouble(args[2]) + 0.5;
                    double y = Double.parseDouble(args[3]);
                    double z = Double.parseDouble(args[4]) + 0.5;
                    player.teleport(new Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch()));
                    sendMessage(sender, "&aTeleported to crate location.");
                } catch (NumberFormatException ignored) {}
            }

            default -> sendMessage(sender, "&cUnknown subcommand. Use /" + label + " help.");
        }

        return true;
    }

    private void sendClickableTp(Player player, Location loc) {
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        String world = loc.getWorld().getName();
        String cmd = "/ramadhanbox _tp " + world + " " + x + " " + y + " " + z;

        String prefixStr = plugin.getConfig().getString("prefix", "&6[RamadhanEvent] &r");

        net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefixStr))
        );

        net.md_5.bungee.api.chat.TextComponent locComponent = new net.md_5.bungee.api.chat.TextComponent("[" + world + ": " + x + ", " + y + ", " + z + "]");
        locComponent.setColor(net.md_5.bungee.api.ChatColor.AQUA);
        locComponent.setBold(false);

        locComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, cmd));

        locComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.hover.content.Text(net.md_5.bungee.api.ChatColor.YELLOW + "Click to teleport")
        ));

        msg.addExtra(locComponent);
        player.spigot().sendMessage(msg);
    }

    private void sendMessage(CommandSender sender, String msg) {
        String prefix = plugin.getConfig().getString("prefix", "&6[RamadhanEvent] &r");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ramadhan.crate.admin")) return Collections.emptyList();
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (String sub : SUB_COMMANDS) {
                if (sub.startsWith(input)) completions.add(sub);
            }
            return completions;
        }
        return Collections.emptyList();
    }
}