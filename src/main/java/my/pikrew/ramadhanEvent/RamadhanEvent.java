package my.pikrew.ramadhanEvent;

import my.pikrew.ramadhanEvent.commands.CrateCommand;
import my.pikrew.ramadhanEvent.commands.RamadhanCommand;
import my.pikrew.ramadhanEvent.commands.RegionWandCommand;
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
    private HungerListener hungerListener;
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

        CrateCommand crateCmd = new CrateCommand(this);
        getCommand("ramadhanbox").setExecutor(crateCmd);
        getCommand("ramadhanbox").setTabCompleter(crateCmd);

        RegionWandCommand wandCmd = new RegionWandCommand(this, regionWandListener);
        getCommand("ramadhanwand").setExecutor(wandCmd);
        getCommand("ramadhanwand").setTabCompleter(wandCmd);

        // ↓ Tambahan baru saja, tidak ada yang diubah di atas
        RamadhanCommand ramadhanCmd = new RamadhanCommand(this);
        getCommand("ramadhanintegrationmobs").setExecutor(ramadhanCmd);
        getCommand("ramadhanintegrationmobs").setTabCompleter(ramadhanCmd);

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