package my.pikrew.ramadhanEvent;

import my.pikrew.ramadhanEvent.commands.NetherSpawnTestCommand;
import my.pikrew.ramadhanEvent.commands.RamadhanCommand;
import my.pikrew.ramadhanEvent.commands.RamadhanIntegrationMobsCommand;
import my.pikrew.ramadhanEvent.listener.*;
import my.pikrew.ramadhanEvent.manager.*;
import my.pikrew.ramadhanEvent.util.MessageUtil;
import org.bukkit.plugin.java.JavaPlugin;

public final class RamadhanEvent extends JavaPlugin {

    private TimeManager        timeManager;
    private MessageUtil        messageUtil;
    private DisplayManager     displayManager;
    private SpawnRateManager   spawnRateManager;
    private GhostSpawnManager  ghostSpawnManager;
    private NetherSpawnManager netherSpawnManager;
    private CrateManager       crateManager;
    private RegionWandListener regionWandListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        messageUtil      = new MessageUtil(this);
        timeManager      = new TimeManager(this);
        spawnRateManager = new SpawnRateManager(this, timeManager);
        displayManager   = new DisplayManager(this, messageUtil, timeManager);
        crateManager     = new CrateManager(this);

        spawnRateManager.start();

        regionWandListener = new RegionWandListener(this);

        getServer().getPluginManager().registerEvents(new HungerListener(this, timeManager), this);
        getServer().getPluginManager().registerEvents(new InteractListener(this), this);
        getServer().getPluginManager().registerEvents(regionWandListener, this);

        if (getServer().getPluginManager().getPlugin("MythicMobs") != null) {
            getServer().getPluginManager().registerEvents(new GhostSpawnListener(this), this);
            getServer().getPluginManager().registerEvents(new MobDeathListener(this), this);

            ghostSpawnManager = new GhostSpawnManager(this, spawnRateManager);
            ghostSpawnManager.start();

            netherSpawnManager = new NetherSpawnManager(this, spawnRateManager);
            netherSpawnManager.start();

            NetherSpawnTestCommand netherTestCmd = new NetherSpawnTestCommand(this);
            getCommand("netherspawntest").setExecutor(netherTestCmd);
            getCommand("netherspawntest").setTabCompleter(netherTestCmd);

            getLogger().info("MythicMobs found — ghost & nether spawn integration active.");
        } else {
            getLogger().warning("MythicMobs not found — ghost/nether spawn integration disabled.");
        }

        TransitionTask transition = new TransitionTask(this, timeManager, messageUtil);
        transition.runTaskTimer(this, 0L, 20L);

        displayManager.start();
        crateManager.startAutoSpawnTask();

        RamadhanCommand ramadhanCmd = new RamadhanCommand(this, regionWandListener);
        getCommand("ramadhan").setExecutor(ramadhanCmd);
        getCommand("ramadhan").setTabCompleter(ramadhanCmd);

        RamadhanIntegrationMobsCommand mobsCmd = new RamadhanIntegrationMobsCommand(this);
        getCommand("ramadhanintegrationmobs").setExecutor(mobsCmd);
        getCommand("ramadhanintegrationmobs").setTabCompleter(mobsCmd);

        if (getConfig().getBoolean("debug", false)) {
            getLogger().info("=== DEBUG MODE ENABLED ===");
            getLogger().info("Day:      " + getConfig().getString("times.day"));
            getLogger().info("Night:    " + getConfig().getString("times.night"));
            getLogger().info("Timezone: " + getConfig().getString("timezone"));
        }

        getLogger().info("RamadhanEvent enabled. Phase: " + timeManager.getCurrentPeriod().name());
    }

    @Override
    public void onDisable() {
        if (ghostSpawnManager  != null) ghostSpawnManager.stop();
        if (netherSpawnManager != null) netherSpawnManager.stop();  // FIX: stop saat disable
        if (displayManager     != null) displayManager.stop();
        if (crateManager       != null) crateManager.cleanupAll();
        getLogger().info("RamadhanEvent disabled.");
    }

    public TimeManager        getTimeManager()        { return timeManager; }
    public MessageUtil        getMessageUtil()         { return messageUtil; }
    public SpawnRateManager   getSpawnRateManager()   { return spawnRateManager; }
    public GhostSpawnManager  getGhostSpawnManager()  { return ghostSpawnManager; }
    public NetherSpawnManager getNetherSpawnManager() { return netherSpawnManager; } // FIX: getter yang hilang
    public DisplayManager     getDisplayManager()     { return displayManager; }
    public CrateManager       getCrateManager()       { return crateManager; }
}