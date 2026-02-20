package my.pikrew.ramadhanEvent.commands;

import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.listener.RegionWandListener;
import my.pikrew.ramadhanEvent.manager.CrateManager;
import my.pikrew.ramadhanEvent.manager.MobSpawnData;
import my.pikrew.ramadhanEvent.util.MessageUtil;
import my.pikrew.ramadhanEvent.manager.TimeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.stream.Collectors;

public class RamadhanCommand implements CommandExecutor, TabCompleter {

    private final RamadhanEvent plugin;
    private final RegionWandListener regionWandListener;

    // Top-level subcommands
    private static final List<String> ROOT_SUBS = Arrays.asList(
            "reload", "debug", "box", "wand"
    );

    // /ramadhan box <sub>
    private static final List<String> BOX_SUBS = Arrays.asList(
            "spawn", "spawnauto", "remove", "status", "help"
    );

    // /ramadhan wand <sub>
    private static final List<String> WAND_SUBS = Arrays.asList(
            "give", "save", "reset"
    );

    public RamadhanCommand(RamadhanEvent plugin, RegionWandListener regionWandListener) {
        this.plugin = plugin;
        this.regionWandListener = regionWandListener;
    }

    // ---------------------------------------------------------------
    // onCommand
    // ---------------------------------------------------------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "debug"  -> handleDebug(sender);
            case "box"    -> handleBox(sender, label, Arrays.copyOfRange(args, 1, args.length));
            case "wand"   -> handleWand(sender, label, Arrays.copyOfRange(args, 1, args.length));
            default       -> sendHelp(sender);
        }
        return true;
    }

    // ---------------------------------------------------------------
    // /ramadhan reload
    // ---------------------------------------------------------------

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("ramadhan.reload")) {
            send(sender, plugin.getMessageUtil().getMessage("commands.no-permission"));
            return;
        }
        plugin.reloadConfig();
        plugin.getMessageUtil().reloadMessages();
        plugin.getTimeManager().reload();
        plugin.getDisplayManager().reload();
        plugin.getCrateManager().reloadConfig();
        send(sender, plugin.getMessageUtil().getMessage("commands.reload"));
    }

    // ---------------------------------------------------------------
    // /ramadhan debug
    // ---------------------------------------------------------------

    private void handleDebug(CommandSender sender) {
        if (!sender.hasPermission("ramadhan.debug")) {
            send(sender, plugin.getMessageUtil().getMessage("commands.no-permission"));
            return;
        }

        MessageUtil mu = plugin.getMessageUtil();
        TimeManager tm = plugin.getTimeManager();

        send(sender, mu.getMessage("debug.header"));
        send(sender, mu.getMessage("debug.server-time").replace("{time}", tm.getCurrentTimeString()));
        send(sender, mu.getMessage("debug.period").replace("{period}", tm.getCurrentPeriod().name()));
        send(sender, mu.getMessage("debug.is-ramadhan").replace("{value}", String.valueOf(tm.isRamadhanTime())));
        send(sender, mu.getMessage("debug.day-start").replace("{time}", plugin.getConfig().getString("times.day", "?")));
        send(sender, mu.getMessage("debug.night-start").replace("{time}", plugin.getConfig().getString("times.night", "?")));
        send(sender, mu.getMessage("debug.timezone").replace("{value}", plugin.getConfig().getString("timezone", "?")));
        send(sender, mu.getMessage("debug.debug-mode").replace("{value}", String.valueOf(plugin.getConfig().getBoolean("debug", false))));
        send(sender, mu.getMessage("debug.mechanics-header"));
        send(sender, mu.getMessage("debug.hunger-multiplier").replace("{value}", String.valueOf(plugin.getConfig().getDouble("ramadhan-time.hunger-loss-multiplier"))));
        send(sender, mu.getMessage("debug.food-multiplier").replace("{value}", String.valueOf(plugin.getConfig().getDouble("ramadhan-time.food-consumption-multiplier"))));
        send(sender, mu.getMessage("debug.display-header"));
        send(sender, mu.getMessage("debug.action-bar").replace("{value}", String.valueOf(plugin.getConfig().getBoolean("displays.action-bar.enabled"))));
        send(sender, mu.getMessage("debug.boss-bar").replace("{value}", String.valueOf(plugin.getConfig().getBoolean("displays.boss-bar.enabled"))));
        send(sender, mu.getMessage("debug.crate-header"));

        CrateManager cm = plugin.getCrateManager();
        send(sender, mu.getMessage("debug.crate-active").replace("{count}", String.valueOf(cm.getActiveCrates().size())));
        send(sender, mu.getMessage("debug.crate-spawn-times").replace("{value}", String.join(", ", plugin.getConfig().getStringList("crate.spawn-times"))));
        send(sender, mu.getMessage("debug.crate-status").replace("{value}", cm.getActiveCratesStatus()));

        if (sender instanceof Player player) {
            for (CrateManager.CrateData data : cm.getActiveCrates().values()) {
                Location loc = data.getLocation();
                if (loc != null) sendClickableTp(player, loc);
            }
        } else {
            for (CrateManager.CrateData data : cm.getActiveCrates().values()) {
                Location loc = data.getLocation();
                if (loc != null) {
                    String locStr = loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
                    send(sender, mu.getMessage("debug.crate-location").replace("{location}", locStr));
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // /ramadhan box <sub>
    // ---------------------------------------------------------------

    private void handleBox(CommandSender sender, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendBoxHelp(sender, label);
            return;
        }

        if (!sender.hasPermission("ramadhan.crate.admin")) {
            send(sender, plugin.getMessageUtil().getMessage("commands.no-permission"));
            return;
        }

        CrateManager cm = plugin.getCrateManager();

        switch (args[0].toLowerCase()) {
            case "spawn" -> {
                if (sender instanceof Player player) {
                    UUID id = cm.spawnCrateAt(player.getLocation());
                    if (id != null) send(sender, plugin.getMessageUtil().getMessage("commands.box.spawned"));
                    else            send(sender, plugin.getMessageUtil().getMessage("commands.box.spawn-failed"));
                } else {
                    UUID id = cm.spawnRandomCrate();
                    if (id != null) send(sender, plugin.getMessageUtil().getMessage("commands.box.spawned-random"));
                    else            send(sender, plugin.getMessageUtil().getMessage("commands.box.spawn-failed"));
                }
            }

            case "spawnauto" -> {
                cm.manualAutoSpawnOnce();
                send(sender, plugin.getMessageUtil().getMessage("commands.box.spawnauto"));
            }

            case "remove" -> {
                cm.cleanupAll();
                send(sender, plugin.getMessageUtil().getMessage("commands.box.removed"));
            }

            case "status" -> {
                send(sender, plugin.getMessageUtil().getMessage("commands.box.active-count")
                        .replace("{count}", String.valueOf(cm.getActiveCrates().size())));

                if (sender instanceof Player player) {
                    for (CrateManager.CrateData data : cm.getActiveCrates().values()) {
                        Location loc = data.getLocation();
                        if (loc != null) sendClickableTp(player, loc);
                    }
                } else {
                    for (CrateManager.CrateData data : cm.getActiveCrates().values()) {
                        Location loc = data.getLocation();
                        if (loc != null) {
                            String locStr = loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
                            send(sender, plugin.getMessageUtil().getMessage("commands.box.location").replace("{location}", locStr));
                        }
                    }
                }
                send(sender, plugin.getMessageUtil().getMessage("commands.box.next-spawn").replace("{value}", cm.getActiveCratesStatus()));
            }

            case "_tp" -> {
                if (!(sender instanceof Player player) || args.length < 5) return;
                try {
                    World world = Bukkit.getWorld(args[1]);
                    if (world == null) { send(sender, plugin.getMessageUtil().getMessage("commands.box.world-not-found").replace("{world}", args[1])); return; }
                    double x = Double.parseDouble(args[2]) + 0.5;
                    double y = Double.parseDouble(args[3]);
                    double z = Double.parseDouble(args[4]) + 0.5;
                    player.teleport(new Location(world, x, y, z, player.getLocation().getYaw(), player.getLocation().getPitch()));
                    send(sender, plugin.getMessageUtil().getMessage("commands.box.teleported"));
                } catch (NumberFormatException ignored) {}
            }

            default -> sendBoxHelp(sender, label);
        }
    }

    private void sendBoxHelp(CommandSender sender, String label) {
        for (String line : plugin.getMessageUtil().getMessageList("commands.box.help")) {
            sender.sendMessage(line.replace("{label}", label));
        }
    }

    // ---------------------------------------------------------------
    // /ramadhan wand <sub>
    // ---------------------------------------------------------------

    private void handleWand(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            send(sender, plugin.getMessageUtil().getMessage("commands.wand.player-only"));
            return;
        }

        String sub = (args.length == 0) ? "give" : args[0].toLowerCase();

        switch (sub) {
            case "give"  -> handleWandGive(player);
            case "save"  -> handleWandSave(player);
            case "reset" -> handleWandReset(player);
            default      -> send(player, plugin.getMessageUtil().getMessage("commands.wand.usage").replace("{label}", label));
        }
    }

    private void handleWandGive(Player player) {
        if (!player.hasPermission("ramadhan.wand.give")) {
            send(player, plugin.getMessageUtil().getMessage("commands.no-permission"));
            return;
        }
        player.getInventory().addItem(createWandItem());
        for (String line : plugin.getMessageUtil().getMessageList("commands.wand.given")) {
            player.sendMessage(line);
        }
    }

    private void handleWandSave(Player player) {
        if (!player.hasPermission("ramadhan.wand.save")) {
            send(player, plugin.getMessageUtil().getMessage("commands.no-permission"));
            return;
        }

        UUID uuid = player.getUniqueId();
        Location loc1 = regionWandListener.getPos1Map().get(uuid);
        Location loc2 = regionWandListener.getPos2Map().get(uuid);

        if (loc1 == null || loc2 == null) {
            send(player, plugin.getMessageUtil().getMessage("commands.wand.missing-positions"));
            return;
        }

        if (!loc1.getWorld().equals(loc2.getWorld())) {
            send(player, plugin.getMessageUtil().getMessage("commands.wand.different-worlds"));
            return;
        }

        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        FileConfiguration config = plugin.getConfig();
        config.set("crate.region.min-x", minX);
        config.set("crate.region.max-x", maxX);
        config.set("crate.region.min-y", minY);
        config.set("crate.region.max-y", maxY);
        config.set("crate.region.min-z", minZ);
        config.set("crate.region.max-z", maxZ);

        List<String> allowedWorlds = config.getStringList("crate.region.world");
        String worldName = loc1.getWorld().getName();
        if (!allowedWorlds.contains(worldName)) {
            allowedWorlds.add(worldName);
            config.set("crate.region.world", allowedWorlds);
            send(player, plugin.getMessageUtil().getMessage("commands.wand.world-added").replace("{world}", worldName));
        } else {
            send(player, plugin.getMessageUtil().getMessage("commands.wand.world-exists").replace("{world}", worldName));
        }

        plugin.saveConfig();

        String coords = "X: " + minX + " to " + maxX + " | Y: " + minY + " to " + maxY + " | Z: " + minZ + " to " + maxZ;
        for (String line : plugin.getMessageUtil().getMessageList("commands.wand.saved")) {
            player.sendMessage(line.replace("{coords}", coords));
        }

        handleWandReset(player);
    }

    private void handleWandReset(Player player) {
        if (!player.hasPermission("ramadhan.wand.reset")) {
            send(player, plugin.getMessageUtil().getMessage("commands.no-permission"));
            return;
        }
        regionWandListener.getPos1Map().remove(player.getUniqueId());
        regionWandListener.getPos2Map().remove(player.getUniqueId());
        send(player, plugin.getMessageUtil().getMessage("commands.wand.reset"));
    }

    private ItemStack createWandItem() {
        FileConfiguration config = plugin.getConfig();
        Material material = Material.getMaterial(config.getString("wand-item.material", "GOLDEN_AXE"));
        if (material == null) material = Material.GOLDEN_AXE;

        ItemStack wand = new ItemStack(material);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                config.getString("wand-item.name", "&b&lRegion Wand")));

        List<String> lore = config.getStringList("wand-item.lore").stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList());
        meta.setLore(lore);
        wand.setItemMeta(meta);
        return wand;
    }

    // ---------------------------------------------------------------
    // /ramadhan help
    // ---------------------------------------------------------------

    private void sendHelp(CommandSender sender) {
        for (String line : plugin.getMessageUtil().getMessageList("commands.help")) {
            sender.sendMessage(line);
        }
    }

    // ---------------------------------------------------------------
    // Clickable TP helper
    // ---------------------------------------------------------------

    private void sendClickableTp(Player player, Location loc) {
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        String world = loc.getWorld().getName();
        String cmd = "/ramadhan box _tp " + world + " " + x + " " + y + " " + z;

        net.md_5.bungee.api.chat.TextComponent msg = new net.md_5.bungee.api.chat.TextComponent(
                net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&',
                                plugin.getMessageUtil().getMessage("commands.box.tp-prefix")))
        );

        net.md_5.bungee.api.chat.TextComponent locComponent = new net.md_5.bungee.api.chat.TextComponent(
                "[" + world + ": " + x + ", " + y + ", " + z + "]");
        locComponent.setColor(net.md_5.bungee.api.ChatColor.AQUA);

        locComponent.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(
                net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, cmd));
        locComponent.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.hover.content.Text(
                        net.md_5.bungee.api.ChatColor.YELLOW + ChatColor.stripColor(
                                plugin.getMessageUtil().getMessage("commands.box.tp-hover")))));

        msg.addExtra(locComponent);
        player.spigot().sendMessage(msg);
    }

    // ---------------------------------------------------------------
    // Utility
    // ---------------------------------------------------------------

    private void send(CommandSender sender, String msg) {
        sender.sendMessage(msg);
    }

    // ---------------------------------------------------------------
    // Tab Completer
    // ---------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return ROOT_SUBS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "box"  -> BOX_SUBS.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                case "wand" -> WAND_SUBS.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
                default     -> Collections.emptyList();
            };
        }

        return Collections.emptyList();
    }
}