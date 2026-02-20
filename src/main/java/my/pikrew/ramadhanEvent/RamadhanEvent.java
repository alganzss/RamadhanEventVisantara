package my.pikrew.ramadhanEvent;

import my.pikrew.ramadhanEvent.commands.RamadhanCommand;
import my.pikrew.ramadhanEvent.commands.RamadhanIntegrationMobsCommand;
import my.pikrew.ramadhanEvent.listener.*;
import my.pikrew.ramadhanEvent.manager.CrateManager;
import my.pikrew.ramadhanEvent.manager.DisplayManager;
import my.pikrew.ramadhanEvent.manager.SpawnRateManager;
import my.pikrew.ramadhanEvent.manager.TimeManager;
import my.pikrew.ramadhanEvent.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class RamadhanEvent extends JavaPlugin {

    private TimeManager timeManager;
    private MessageUtil messageUtil;
    private DisplayManager displayManager;
    private SpawnRateManager spawnRateManager;
    private TransitionTask transitionTask;
    private CrateManager crateManager;
    private RegionWandListener regionWandListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        // Core managers
        messageUtil    = new MessageUtil(this);
        timeManager    = new TimeManager(this);
        displayManager = new DisplayManager(this, messageUtil, timeManager);
        crateManager   = new CrateManager(this);

        regionWandListener = new RegionWandListener(this);
        getServer().getPluginManager().registerEvents(new HungerListener(this, timeManager), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(regionWandListener, this);

        transitionTask = new TransitionTask(this, timeManager, messageUtil);
        transitionTask.runTaskTimer(this, 0L, 20L);
        displayManager.start();
        crateManager.startAutoSpawnTask();

        RamadhanCommand ramadhanCmd = new RamadhanCommand(this, regionWandListener);
        getCommand("ramadhan").setExecutor(ramadhanCmd);
        getCommand("ramadhan").setTabCompleter(ramadhanCmd);

        // Separate mobs integration command
        RamadhanIntegrationMobsCommand mobsCmd = new RamadhanIntegrationMobsCommand(this);
        getCommand("ramadhanintegrationmobs").setExecutor(mobsCmd);
        getCommand("ramadhanintegrationmobs").setTabCompleter(mobsCmd);

        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("=== DEBUG MODE ENABLED ===");
            getLogger().info("Day time: "   + getConfig().getString("times.day"));
            getLogger().info("Night time: " + getConfig().getString("times.night"));
            getLogger().info("Timezone: "   + getConfig().getString("timezone"));
            getLogger().info("Hunger Loss Multiplier: "      + getConfig().getDouble("ramadhan-time.hunger-loss-multiplier"));
            getLogger().info("Food Consumption Multiplier: " + getConfig().getDouble("ramadhan-time.food-consumption-multiplier"));
            getLogger().info("Crate spawn-times: "           + getConfig().getStringList("crate.spawn-times"));
        }

        getLogger().info("RamadhanEvent enabled! Day: " + getConfig().getString("times.day")
                + " | Night: " + getConfig().getString("times.night")
                + " (" + getConfig().getString("timezone") + ")");
        getLogger().info("Current period: " + timeManager.getCurrentPeriod().name()
                + " (" + (timeManager.isRamadhanTime() ? "ramadhan" : "vanilla") + ")");
    }

    public TimeManager getTimeManager()           { return timeManager; }
    public MessageUtil getMessageUtil()           { return messageUtil; }
    public SpawnRateManager getSpawnRateManager() { return spawnRateManager; }
    public DisplayManager getDisplayManager()     { return displayManager; }
    public CrateManager getCrateManager()         { return crateManager; }
}