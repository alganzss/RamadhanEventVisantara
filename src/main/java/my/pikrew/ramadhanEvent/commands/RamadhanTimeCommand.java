package my.pikrew.ramadhanEvent.commands;

import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.util.MessageUtil;
import my.pikrew.ramadhanEvent.manager.TimeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RamadhanTimeCommand implements CommandExecutor, TabCompleter {

    private final RamadhanEvent plugin;
    private final MessageUtil messageUtil;
    private final TimeManager timeManager;

    private static final List<String> SUB_COMMANDS = Arrays.asList("reload", "debug");

    public RamadhanTimeCommand(RamadhanEvent plugin) {
        this.plugin = plugin;
        this.messageUtil = plugin.getMessageUtil();
        this.timeManager = plugin.getTimeManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(messageUtil.getMessage("commands.help"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("ramadhan.reload")) {
                    sender.sendMessage(messageUtil.getMessage("commands.no-permission"));
                    return true;
                }

                plugin.reloadConfig();
                messageUtil.reloadMessages();
                timeManager.reload();
                plugin.getDisplayManager().reload();

                sender.sendMessage(messageUtil.getMessage("commands.reload"));

                if (plugin.getConfig().getBoolean("debug", false)) {
                    sender.sendMessage("§e[DEBUG] Config reloaded:");
                    sender.sendMessage("§e[DEBUG] Day: " + plugin.getConfig().getString("times.day"));
                    sender.sendMessage("§e[DEBUG] Night: " + plugin.getConfig().getString("times.night"));
                    sender.sendMessage("§e[DEBUG] Current time: " + timeManager.getCurrentTimeString());
                    sender.sendMessage("§e[DEBUG] Current period: " + timeManager.getCurrentPeriod().name());
                    sender.sendMessage("§e[DEBUG] Hunger multiplier: " + plugin.getConfig().getDouble("ramadhan-time.hunger-loss-multiplier"));
                    sender.sendMessage("§e[DEBUG] Food multiplier: " + plugin.getConfig().getDouble("ramadhan-time.food-consumption-multiplier"));
                }
            }

            case "debug" -> {
                if (!sender.hasPermission("ramadhan.debug")) {
                    sender.sendMessage(messageUtil.getMessage("commands.no-permission"));
                    return true;
                }

                sender.sendMessage("§6=== Ramadhan Debug Info ===");
                sender.sendMessage("§eServer Time: §f" + timeManager.getCurrentTimeString());
                sender.sendMessage("§eCurrent Period: §f" + timeManager.getCurrentPeriod().name());
                sender.sendMessage("§eIs Ramadhan Time: §f" + timeManager.isRamadhanTime());
                sender.sendMessage("§eDay Start: §f" + plugin.getConfig().getString("times.day"));
                sender.sendMessage("§eNight Start: §f" + plugin.getConfig().getString("times.night"));
                sender.sendMessage("§eTimezone: §f" + plugin.getConfig().getString("timezone"));
                sender.sendMessage("§eDebug Mode: §f" + plugin.getConfig().getBoolean("debug", false));
                sender.sendMessage("§6=== Ramadhan Mechanics ===");
                sender.sendMessage("§eHunger Loss Multiplier: §f" + plugin.getConfig().getDouble("ramadhan-time.hunger-loss-multiplier") + "x");
                sender.sendMessage("§eFood Consumption Multiplier: §f" + plugin.getConfig().getDouble("ramadhan-time.food-consumption-multiplier") + "x");
                sender.sendMessage("§6=== Display Settings ===");
                sender.sendMessage("§eAction Bar: §f" + plugin.getConfig().getBoolean("displays.action-bar.enabled"));
                sender.sendMessage("§eBoss Bar: §f" + plugin.getConfig().getBoolean("displays.boss-bar.enabled"));
            }

            default -> sender.sendMessage(messageUtil.getMessage("commands.help"));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUB_COMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}