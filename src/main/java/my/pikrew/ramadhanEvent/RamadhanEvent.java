package my.pikrew.ramadhanEvent;

import my.pikrew.ramadhanEvent.listener.HungerListener;
import my.pikrew.ramadhanEvent.listener.MobDeathListener;
import my.pikrew.ramadhanEvent.listener.TransitionTask;
import my.pikrew.ramadhanEvent.manager.DisplayManager;
import my.pikrew.ramadhanEvent.manager.SpawnRateManager;
import my.pikrew.ramadhanEvent.manager.TimeManager;
import my.pikrew.ramadhanEvent.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class RamadhanEvent extends JavaPlugin {

    private TimeManager timeManager;
    private MessageUtil messageUtil;
    private DisplayManager displayManager;
    private SpawnRateManager spawnRateManager;
    private HungerListener hungerListener;
    private TransitionTask transitionTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        timeManager = new TimeManager(this);
        messageUtil = new MessageUtil(this);
        displayManager = new DisplayManager(this, messageUtil, timeManager);
        spawnRateManager = new SpawnRateManager(this, timeManager);
        hungerListener = new HungerListener(this, timeManager);

        getServer().getPluginManager().registerEvents(hungerListener, this);
        getServer().getPluginManager().registerEvents(new MobDeathListener(this), this);

        transitionTask = new TransitionTask(this, timeManager, messageUtil);
        transitionTask.runTaskTimer(this, 0L, 20L);

        displayManager.start();
        spawnRateManager.start();

        getLogger().info("RamadhanEvent enabled! | Period: " + timeManager.getCurrentPeriod().name()
                + " | Time: " + timeManager.getCurrentTimeString());
    }

    @Override
    public void onDisable() {
        if (transitionTask != null) transitionTask.cancel();
        if (displayManager != null) displayManager.stop();
        if (spawnRateManager != null) spawnRateManager.stop();
        getLogger().info("RamadhanEvent disabled.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("ramadhan")) return false;

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
                reloadConfig();
                messageUtil.reloadMessages();
                timeManager.reload();
                displayManager.reload();
                spawnRateManager.reload();
                hungerListener.reloadValues();
                sender.sendMessage(messageUtil.getMessage("commands.reload"));

                if (getConfig().getBoolean("debug", false)) {
                    sender.sendMessage("§e[Debug] Time: " + timeManager.getCurrentTimeString()
                            + " | Period: " + timeManager.getCurrentPeriod().name());
                }
            }
            case "debug" -> {
                if (!sender.hasPermission("ramadhan.debug")) {
                    sender.sendMessage(messageUtil.getMessage("commands.no-permission"));
                    return true;
                }
                sender.sendMessage("§6=== Ramadhan Debug ===");
                sender.sendMessage("§eServer Time: §f" + timeManager.getCurrentTimeString());
                sender.sendMessage("§ePeriod: §f" + timeManager.getCurrentPeriod().name());
                sender.sendMessage("§eIs Ramadhan Time: §f" + timeManager.isRamadhanTime());
                sender.sendMessage("§eIs Night Surge: §f" + timeManager.isNightSurge());
                sender.sendMessage("§eDay Start: §f" + getConfig().getString("times.day"));
                sender.sendMessage("§eNight Start: §f" + getConfig().getString("times.night"));
                sender.sendMessage("§eTimezone: §f" + getConfig().getString("timezone"));
                sender.sendMessage("§eHunger Multiplier: §f" + getConfig().getDouble("ramadhan-time.hunger-loss-multiplier") + "x");
                sender.sendMessage("§eFood Multiplier: §f" + getConfig().getDouble("ramadhan-time.food-consumption-multiplier") + "x");
                sender.sendMessage("§eAction Bar: §f" + getConfig().getBoolean("displays.action-bar.enabled"));
                sender.sendMessage("§eBoss Bar: §f" + getConfig().getBoolean("displays.boss-bar.enabled"));
            }
            case "spawnstatus" -> {
                if (!sender.hasPermission("ramadhan.spawn")) {
                    sender.sendMessage(messageUtil.getMessage("commands.no-permission"));
                    return true;
                }
                sender.sendMessage("§6=== Ghost Spawn Status ===");
                sender.sendMessage("§eNight Surge Active: §f" + spawnRateManager.isNightSurgeActive());
                spawnRateManager.getSpawnDataMap().values().forEach(data ->
                        sender.sendMessage("§e" + data.getMobKey() + " §7| Chance: §f" + data.getCurrentChance()
                                + " §7| Level: §f" + data.getCurrentMinLevel() + "-" + data.getCurrentMaxLevel()));
            }
            case "spawnrefresh" -> {
                if (!sender.hasPermission("ramadhan.spawn")) {
                    sender.sendMessage(messageUtil.getMessage("commands.no-permission"));
                    return true;
                }
                spawnRateManager.forceRefresh();
                sender.sendMessage("§aGhost spawn rates refreshed.");
            }
            default -> sender.sendMessage(messageUtil.getMessage("commands.help"));
        }

        return true;
    }

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public SpawnRateManager getSpawnRateManager() {
        return spawnRateManager;
    }
}