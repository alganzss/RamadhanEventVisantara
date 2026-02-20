package my.pikrew.ramadhanEvent;

import my.pikrew.ramadhanEvent.commands.CrateCommand;
import my.pikrew.ramadhanEvent.commands.RamadhanTimeCommand;
import my.pikrew.ramadhanEvent.commands.RegionWandCommand;
import my.pikrew.ramadhanEvent.listener.HungerListener;
import my.pikrew.ramadhanEvent.listener.InteractListener;
import my.pikrew.ramadhanEvent.listener.RegionWandListener;
import my.pikrew.ramadhanEvent.listener.TransitionTask;
import my.pikrew.ramadhanEvent.manager.CrateManager;
import my.pikrew.ramadhanEvent.manager.DisplayManager;
import my.pikrew.ramadhanEvent.manager.TimeManager;
import my.pikrew.ramadhanEvent.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public class RamadhanEvent extends JavaPlugin {

    private TimeManager timeManager;
    private MessageUtil messageUtil;
    private DisplayManager displayManager;
    private TransitionTask transitionTask;
    private CrateManager crateManager;
    private RegionWandListener regionWandListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        messageUtil  = new MessageUtil(this);
        timeManager  = new TimeManager(this);
        displayManager = new DisplayManager(this, messageUtil, timeManager);
        crateManager = new CrateManager(this);

        regionWandListener = new RegionWandListener(this);
        getServer().getPluginManager().registerEvents(new HungerListener(this, timeManager), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(regionWandListener, this);

        transitionTask = new TransitionTask(this, timeManager, messageUtil);
        transitionTask.runTaskTimer(this, 0L, 20L);
        displayManager.start();
        crateManager.startAutoSpawnTask();

        RamadhanTimeCommand ramadhanCmd = new RamadhanTimeCommand(this);
        getCommand("ramadhan").setExecutor(ramadhanCmd);
        getCommand("ramadhan").setTabCompleter(ramadhanCmd);

        CrateCommand crateCmd = new CrateCommand(this);
        getCommand("ramadhanbox").setExecutor(crateCmd);
        getCommand("ramadhanbox").setTabCompleter(crateCmd);

        RegionWandCommand wandCmd = new RegionWandCommand(this, regionWandListener);
        getCommand("ramadhanwand").setExecutor(wandCmd);
        getCommand("ramadhanwand").setTabCompleter(wandCmd);

        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("=== DEBUG MODE ENABLED ===");
            getLogger().info("Day time: "  + getConfig().getString("times.day"));
            getLogger().info("Night time: " + getConfig().getString("times.night"));
            getLogger().info("Timezone: "  + getConfig().getString("timezone"));
            getLogger().info("Hunger Loss Multiplier: "    + getConfig().getDouble("ramadhan-time.hunger-loss-multiplier"));
            getLogger().info("Food Consumption Multiplier: " + getConfig().getDouble("ramadhan-time.food-consumption-multiplier"));
            getLogger().info("Crate spawn-times: " + getConfig().getStringList("crate.spawn-times"));
        }

        getLogger().info("RamadhanEvent enabled! Day: " + getConfig().getString("times.day")
                + " | Night: " + getConfig().getString("times.night")
                + " (" + getConfig().getString("timezone") + ")");
        getLogger().info("Current period: " + timeManager.getCurrentPeriod().name()
                + " (" + (timeManager.isRamadhanTime() ? "ramadhan" : "vanilla") + ")");
    }

    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);

        if (transitionTask != null)  transitionTask.cancel();
        if (displayManager != null)  displayManager.stop();
        if (crateManager != null)    crateManager.cleanupAll();

        getLogger().info("RamadhanEvent disabled.");
    }

    public MessageUtil getMessageUtil()       { return messageUtil; }
    public TimeManager getTimeManager()       { return timeManager; }
    public DisplayManager getDisplayManager() { return displayManager; }
    public CrateManager getCrateManager()     { return crateManager; }
}