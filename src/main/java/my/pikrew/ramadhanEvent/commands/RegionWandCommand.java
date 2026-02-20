package my.pikrew.ramadhanEvent.commands;

import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.listener.RegionWandListener;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RegionWandCommand implements CommandExecutor, TabCompleter {

    private final RamadhanEvent plugin;
    private final RegionWandListener listener;

    private static final List<String> SUBCOMMANDS = Arrays.asList("give", "save", "reset");

    public RegionWandCommand(RamadhanEvent plugin, RegionWandListener listener) {
        this.plugin = plugin;
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        String sub = (args.length == 0) ? "give" : args[0].toLowerCase();

        switch (sub) {
            case "give"  -> handleGive(player);
            case "save"  -> handleSave(player);
            case "reset" -> handleReset(player);
            default      -> sendMessage(player, "&cUsage: /ramadhanwand <give|save|reset>");
        }

        return true;
    }

    private void handleGive(Player player) {
        if (!player.hasPermission("ramadhan.wand.give")) {
            sendMessage(player, "&cYou don't have permission to use this command.");
            return;
        }
        player.getInventory().addItem(createWandItem());
        sendMessage(player, "&7You received the &b&lRegion Wand&7.");
        sendMessage(player, "&7Left-click = Pos 1 &8| &7Right-click = Pos 2");
        sendMessage(player, "&7Use &b/ramadhanwand save &7when both positions are set.");
    }

    private void handleSave(Player player) {
        if (!player.hasPermission("ramadhan.wand.save")) {
            sendMessage(player, "&cYou don't have permission to use this command.");
            return;
        }

        UUID uuid = player.getUniqueId();
        Location loc1 = listener.getPos1Map().get(uuid);
        Location loc2 = listener.getPos2Map().get(uuid);

        if (loc1 == null || loc2 == null) {
            sendMessage(player, "&cPlease set both Position 1 and Position 2 first.");
            return;
        }

        if (!loc1.getWorld().equals(loc2.getWorld())) {
            sendMessage(player, "&cBoth positions must be in the same world.");
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
            sendMessage(player, "&7World &e" + worldName + " &7added to the spawn region.");
        } else {
            sendMessage(player, "&7World &e" + worldName + " &7is already in the spawn region.");
        }

        plugin.saveConfig();

        sendMessage(player, "&a&lRegion saved successfully!");
        sendMessage(player, "&7X: &f" + minX + " to " + maxX
                + " &7| Y: &f" + minY + " to " + maxY
                + " &7| Z: &f" + minZ + " to " + maxZ);

        handleReset(player);
    }

    private void handleReset(Player player) {
        if (!player.hasPermission("ramadhan.wand.reset")) {
            sendMessage(player, "&cYou don't have permission to use this command.");
            return;
        }
        UUID uuid = player.getUniqueId();
        listener.getPos1Map().remove(uuid);
        listener.getPos2Map().remove(uuid);
        sendMessage(player, "&7Wand positions have been &areset&7.");
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

    private void sendMessage(CommandSender sender, String msg) {
        String prefix = plugin.getConfig().getString("prefix", "&6[RamadhanEvent] &r");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}