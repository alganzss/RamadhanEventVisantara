package my.pikrew.ramadhanEvent.commands;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.manager.MobSpawnData;
import my.pikrew.ramadhanEvent.manager.MobTracker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RamadhanIntegrationMobsCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("status", "refresh", "mobs", "scan", "test");

    private final RamadhanEvent plugin;

    public RamadhanIntegrationMobsCommand(RamadhanEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("ramadhanevent.admin")) {
            sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "status"  -> handleStatus(sender);
            case "refresh" -> handleRefresh(sender);
            case "mobs"    -> handleMobs(sender, args);
            case "scan"    -> handleScan(sender);
            case "test"    -> handleTest(sender, args);
            case "_tp"     -> handleTp(sender, args);
            default        -> sendHelp(sender);
        }

        return true;
    }

    private void handleStatus(CommandSender sender) {
        boolean night = plugin.getSpawnRateManager().isNightSurgeActive();

        sender.sendMessage(Component.text("=== Ghost Mob Status ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Time  : " + plugin.getTimeManager().getCurrentTimeString())
                .color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Phase : " + (night ? "Night Surge" : "Day"))
                .color(night ? NamedTextColor.DARK_PURPLE : NamedTextColor.AQUA));

        for (MobSpawnData data : plugin.getSpawnRateManager().getSpawnDataMap().values()) {
            String pct  = String.format("%.1f%%", data.getCurrentChance() * 100);
            String line = "  " + data.getMobKey()
                    + "  chance=" + pct
                    + "  level=" + data.getCurrentMinLevel() + "-" + data.getCurrentMaxLevel();
            sender.sendMessage(Component.text(line).color(NamedTextColor.WHITE));
        }
    }

    private void handleRefresh(CommandSender sender) {
        plugin.getSpawnRateManager().forceRefresh();
        sender.sendMessage(Component.text("Spawn rates refreshed.").color(NamedTextColor.GREEN));
    }

    private void handleMobs(CommandSender sender, String[] args) {
        MobTracker   tracker = plugin.getSpawnRateManager().getMobTracker();
        Set<String>  tracked = plugin.getSpawnRateManager().getSpawnDataMap().keySet();

        tracker.syncFromWorld(tracked);

        String       filter = args.length >= 2 ? args[1] : null;
        List<String> keys   = filter != null ? List.of(filter) : new ArrayList<>(tracked);

        sender.sendMessage(Component.text("=== Live Ghost Mobs ===").color(NamedTextColor.GOLD));

        int grandLive    = 0;
        int grandDay     = 0;
        int grandNight   = 0;

        for (String mobKey : keys) {
            MobSpawnData data = plugin.getSpawnRateManager().getSpawnDataMap().get(mobKey);
            if (data == null) {
                sender.sendMessage(Component.text("Unknown: " + mobKey).color(NamedTextColor.RED));
                continue;
            }

            List<MobTracker.TrackedMob> live = tracker.getLiveFor(mobKey);

            grandLive  += live.size();
            grandDay   += data.getDaySpawnCount();
            grandNight += data.getNightSpawnCount();

            sender.sendMessage(Component.text(
                    mobKey + "  alive=" + live.size()
                            + "  day=" + data.getDaySpawnCount()
                            + "  night=" + data.getNightSpawnCount()
                            + "  total=" + data.getTotalSpawnCount()
            ).color(NamedTextColor.AQUA));

            if (live.isEmpty()) {
                sender.sendMessage(Component.text("  (none alive)").color(NamedTextColor.GRAY));
                continue;
            }

            for (int i = 0; i < live.size(); i++) {
                MobTracker.TrackedMob mob = live.get(i);
                if (!mob.isValid()) continue;

                String label = "#" + (i + 1) + " " + mobKey;
                if (sender instanceof Player player) {
                    sendTpLink(player, label, mob.location());
                } else {
                    Location loc = mob.location();
                    sender.sendMessage(Component.text(
                            "  " + label + " -> " + loc.getWorld().getName()
                                    + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()
                    ).color(NamedTextColor.WHITE));
                }
            }
        }

        sender.sendMessage(Component.text(
                "Totals  alive=" + grandLive
                        + "  day=" + grandDay
                        + "  night=" + grandNight
                        + "  all=" + (grandDay + grandNight)
        ).color(NamedTextColor.YELLOW));
    }

    private void handleScan(CommandSender sender) {
        Set<String> tracked = plugin.getSpawnRateManager().getSpawnDataMap().keySet();

        sender.sendMessage(Component.text("=== Ghost Mob Scan ===").color(NamedTextColor.GOLD));

        int found = 0;
        for (ActiveMob am : MythicBukkit.inst().getMobManager().getActiveMobs()) {
            String key = am.getType().getInternalName();
            if (!tracked.contains(key)) continue;

            Entity entity = extractEntity(am);
            if (entity == null) continue;

            found++;
            Location loc = entity.getLocation();
            String label = key + " (" + loc.getWorld().getName() + ")";

            if (sender instanceof Player player) {
                sendTpLink(player, label, loc);
            } else {
                sender.sendMessage(Component.text(
                        "  " + key + " -> " + loc.getWorld().getName()
                                + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()
                ).color(NamedTextColor.WHITE));
            }
        }

        if (found == 0) {
            sender.sendMessage(Component.text("No tracked ghost mobs found in the world.").color(NamedTextColor.GRAY));
        } else {
            sender.sendMessage(Component.text("Found " + found + " ghost mob(s).").color(NamedTextColor.YELLOW));
        }
    }

    private void handleTest(CommandSender sender, String[] args) {
        if (args.length < 3) {
            printSpawnSummary(sender);
            return;
        }

        String  mobKey       = args[1];
        String  phaseArg     = args[2].toLowerCase(Locale.ROOT);
        int     trials       = 100;

        if (!phaseArg.equals("day") && !phaseArg.equals("night")) {
            sender.sendMessage(Component.text("Phase must be 'day' or 'night'.").color(NamedTextColor.RED));
            return;
        }

        if (args.length >= 4) {
            try {
                trials = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Trials must be a number (1-10000).").color(NamedTextColor.RED));
                return;
            }
        }

        boolean night     = phaseArg.equals("night");
        int     successes = plugin.getSpawnRateManager().simulateSpawnCheck(mobKey, night, trials);

        if (successes == -1) {
            sender.sendMessage(Component.text("Unknown mob: " + mobKey
                    + " — run /ramadhanintegrationmobs scan to see active internal names.").color(NamedTextColor.RED));
            return;
        }

        MobSpawnData data        = plugin.getSpawnRateManager().getSpawnDataMap().get(mobKey);
        double       cfgChance   = data.getProfile().chanceFor(night);
        double       actualRate  = (double) successes / trials * 100;

        sender.sendMessage(Component.text("=== Spawn Sim: " + mobKey + " ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Phase      : " + (night ? "Night" : "Day")).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Configured : " + String.format("%.1f%%", cfgChance * 100)).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Trials     : " + trials).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Passed     : " + successes + "/" + trials
                + " (" + String.format("%.1f%%", actualRate) + ")").color(NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Level range: " + data.getProfile().minLevelFor(night)
                + "-" + data.getProfile().maxLevelFor(night)).color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("---").color(NamedTextColor.DARK_GRAY));
        sender.sendMessage(Component.text("Spawned day   : " + data.getDaySpawnCount()).color(NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Spawned night : " + data.getNightSpawnCount()).color(NamedTextColor.DARK_PURPLE));
        sender.sendMessage(Component.text("Spawned total : " + data.getTotalSpawnCount()).color(NamedTextColor.YELLOW));
    }

    private void printSpawnSummary(CommandSender sender) {
        sender.sendMessage(Component.text("=== Spawn Summary (since last reload) ===").color(NamedTextColor.GOLD));

        int grandDay   = 0;
        int grandNight = 0;

        for (MobSpawnData data : plugin.getSpawnRateManager().getSpawnDataMap().values()) {
            grandDay   += data.getDaySpawnCount();
            grandNight += data.getNightSpawnCount();

            sender.sendMessage(Component.text(
                    "  " + data.getMobKey()
                            + "  day=" + data.getDaySpawnCount()
                            + "  night=" + data.getNightSpawnCount()
                            + "  total=" + data.getTotalSpawnCount()
            ).color(NamedTextColor.WHITE));
        }

        sender.sendMessage(Component.text(
                "Grand total  day=" + grandDay
                        + "  night=" + grandNight
                        + "  all=" + (grandDay + grandNight)
        ).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Run: test <mob> <day|night> [trials] to simulate rolls.").color(NamedTextColor.GRAY));
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player) || args.length < 5) return;
        try {
            World world = Bukkit.getWorld(args[1]);
            if (world == null) {
                player.sendMessage(Component.text("World not found: " + args[1]).color(NamedTextColor.RED));
                return;
            }
            double x = Double.parseDouble(args[2]) + 0.5;
            double y = Double.parseDouble(args[3]);
            double z = Double.parseDouble(args[4]) + 0.5;
            player.teleport(new Location(world, x, y, z,
                    player.getLocation().getYaw(), player.getLocation().getPitch()));
            player.sendMessage(Component.text("Teleported.").color(NamedTextColor.GREEN));
        } catch (NumberFormatException ignored) {}
    }

    private Entity extractEntity(ActiveMob am) {
        try {
            return (Entity) am.getEntity().getBukkitEntity();
        } catch (Exception e) {
            return null;
        }
    }

    private void sendTpLink(Player player, String label, Location loc) {
        int    bx    = loc.getBlockX();
        int    by    = loc.getBlockY();
        int    bz    = loc.getBlockZ();
        String world = loc.getWorld().getName();
        String cmd   = "/ramadhanintegrationmobs _tp " + world + " " + bx + " " + by + " " + bz;

        TextComponent line = new TextComponent("  ");
        TextComponent link = new TextComponent(label + " [" + bx + " " + by + " " + bz + "]");
        link.setColor(ChatColor.WHITE);
        link.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(ChatColor.YELLOW + "Click to teleport")));
        line.addExtra(link);
        player.spigot().sendMessage(line);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Ghost Mob Commands ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("  status                          Current phase & rates").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  refresh                         Re-apply spawn rates now").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  mobs [mobKey]                   Live mobs + coords (clickable)").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  scan                            List ghost mobs in world").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  test                            Day/night spawn totals").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  test <mob> <day|night> [n]      Simulate chance rolls").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) return filter(SUBCOMMANDS, args[0]);

        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("test") || sub.equals("mobs")) {
                return filter(new ArrayList<>(plugin.getSpawnRateManager().getSpawnDataMap().keySet()), args[1]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("test")) {
            return filter(Arrays.asList("day", "night"), args[2]);
        }

        return List.of();
    }

    private List<String> filter(List<String> options, String input) {
        String       prefix = input.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase(Locale.ROOT).startsWith(prefix)) result.add(s);
        }
        return result;
    }
}