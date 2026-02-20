package my.pikrew.ramadhanEvent.commands;

import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.manager.MobSpawnData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class RamadhanCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("reload", "status", "refresh");

    private final RamadhanEvent plugin;

    public RamadhanCommand(RamadhanEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("ramadhanevent.admin")) {
            sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload"  -> handleReload(sender);
            case "status"  -> handleStatus(sender);
            case "refresh" -> handleRefresh(sender);
            default        -> sendHelp(sender);
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getSpawnRateManager().reload();
        sender.sendMessage(Component.text("[RamadhanEvent] Configuration reloaded.").color(NamedTextColor.GREEN));
    }

    private void handleStatus(CommandSender sender) {
        String currentTime = plugin.getTimeManager().getCurrentTimeString();
        boolean nightSurge = plugin.getSpawnRateManager().isNightSurgeActive();

        sender.sendMessage(Component.text("=== RamadhanEvent Status ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Current Time : " + currentTime).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Phase        : " + (nightSurge ? "Night Surge" : "Day")).color(
                nightSurge ? NamedTextColor.DARK_PURPLE : NamedTextColor.AQUA));

        for (MobSpawnData data : plugin.getSpawnRateManager().getSpawnDataMap().values()) {
            String line = data.getMobKey()
                    + " | Chance: " + data.getCurrentChance()
                    + " | Level: " + data.getCurrentMinLevel() + "-" + data.getCurrentMaxLevel();
            sender.sendMessage(Component.text("  " + line).color(NamedTextColor.WHITE));
        }
    }

    private void handleRefresh(CommandSender sender) {
        plugin.getSpawnRateManager().forceRefresh();
        sender.sendMessage(Component.text("[RamadhanEvent] Spawn rates refreshed.").color(NamedTextColor.GREEN));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== RamadhanEvent Commands ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/re reload  - Reload configuration").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/re status  - Show current spawn status").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/re refresh - Force apply spawn rates now").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS;
        }
        return List.of();
    }
}
