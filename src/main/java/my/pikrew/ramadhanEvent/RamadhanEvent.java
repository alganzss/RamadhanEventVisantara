package my.pikrew.ramadhanEvent;

import my.pikrew.ramadhanEvent.listener.HungerListener;
import my.pikrew.ramadhanEvent.listener.TransitionTask;
import my.pikrew.ramadhanEvent.manager.DisplayManager;
import my.pikrew.ramadhanEvent.manager.TimeManager;
import my.pikrew.ramadhanEvent.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class RamadhanEvent extends JavaPlugin {
    private TimeManager timeManager;
    private MessageUtil messageUtil;
    private DisplayManager displayManager;
    private TransitionTask transitionTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        messageUtil = new MessageUtil(this);
        timeManager = new TimeManager(this);
        displayManager = new DisplayManager(this, messageUtil, timeManager);

        getServer().getPluginManager().registerEvents(new HungerListener(this, timeManager), this);

        transitionTask = new TransitionTask(this, timeManager, messageUtil);
        transitionTask.runTaskTimer(this, 0L, 20L);

        displayManager.start();

        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("=== DEBUG MODE ENABLED ===");
            getLogger().info("Day time: " + getConfig().getString("times.day"));
            getLogger().info("Night time: " + getConfig().getString("times.night"));
            getLogger().info("Timezone: " + getConfig().getString("timezone"));
            getLogger().info("Hunger Loss Multiplier: " + getConfig().getDouble("ramadhan-time.hunger-loss-multiplier"));
            getLogger().info("Food Consumption Multiplier: " + getConfig().getDouble("ramadhan-time.food-consumption-multiplier"));
        }

        getLogger().info("RamadhanEvent enabled! Day: " + getConfig().getString("times.day") +
                " | Night: " + getConfig().getString("times.night") +
                " (" + getConfig().getString("timezone") + ")");
        getLogger().info("Current period: " + timeManager.getCurrentPeriod().name() +
                " (" + (timeManager.isRamadhanTime() ? "ramadhan" : "vanilla") + ")");
    }

    @Override
    public void onDisable() {
        if (transitionTask != null) {
            transitionTask.cancel();
        }

        if (displayManager != null) {
            displayManager.stop();
        }

        getLogger().info("RamadhanEvent disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("ramadhan")) {
            if (args.length == 0) {
                sender.sendMessage(messageUtil.getMessage("commands.help"));
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("ramadhan.reload")) {
                    sender.sendMessage(messageUtil.getMessage("commands.no-permission"));
                    return true;
                }

                reloadConfig();
                messageUtil.reloadMessages();
                timeManager.reload();
                displayManager.reload();

                sender.sendMessage(messageUtil.getMessage("commands.reload"));

                if (getConfig().getBoolean("debug", false)) {
                    sender.sendMessage("§e[DEBUG] Config reloaded:");
                    sender.sendMessage("§e[DEBUG] Day: " + getConfig().getString("times.day"));
                    sender.sendMessage("§e[DEBUG] Night: " + getConfig().getString("times.night"));
                    sender.sendMessage("§e[DEBUG] Current time: " + timeManager.getCurrentTimeString());
                    sender.sendMessage("§e[DEBUG] Current period: " + timeManager.getCurrentPeriod().name());
                    sender.sendMessage("§e[DEBUG] Hunger multiplier: " + getConfig().getDouble("ramadhan-time.hunger-loss-multiplier"));
                    sender.sendMessage("§e[DEBUG] Food multiplier: " + getConfig().getDouble("ramadhan-time.food-consumption-multiplier"));
                }

                return true;
            }

            if (args[0].equalsIgnoreCase("debug")) {
                if (!sender.hasPermission("ramadhan.debug")) {
                    sender.sendMessage(messageUtil.getMessage("commands.no-permission"));
                    return true;
                }

                sender.sendMessage("§6=== Ramadhan Debug Info ===");
                sender.sendMessage("§eServer Time: §f" + timeManager.getCurrentTimeString());
                sender.sendMessage("§eCurrent Period: §f" + timeManager.getCurrentPeriod().name());
                sender.sendMessage("§eIs Ramadhan Time: §f" + timeManager.isRamadhanTime());
                sender.sendMessage("§eDay Start: §f" + getConfig().getString("times.day"));
                sender.sendMessage("§eNight Start: §f" + getConfig().getString("times.night"));
                sender.sendMessage("§eTimezone: §f" + getConfig().getString("timezone"));
                sender.sendMessage("§eDebug Mode: §f" + getConfig().getBoolean("debug", false));
                sender.sendMessage("§6=== Ramadhan Mechanics ===");
                sender.sendMessage("§eHunger Loss Multiplier: §f" + getConfig().getDouble("ramadhan-time.hunger-loss-multiplier") + "x");
                sender.sendMessage("§eFood Consumption Multiplier: §f" + getConfig().getDouble("ramadhan-time.food-consumption-multiplier") + "x");
                sender.sendMessage("§6=== Display Settings ===");
                sender.sendMessage("§eAction Bar: §f" + getConfig().getBoolean("displays.action-bar.enabled"));
                sender.sendMessage("§eBoss Bar: §f" + getConfig().getBoolean("displays.boss-bar.enabled"));

                return true;
            }

            sender.sendMessage(messageUtil.getMessage("commands.help"));
            return true;
        }

        return false;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }
}