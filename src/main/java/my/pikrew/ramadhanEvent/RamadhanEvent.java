package my.pikrew.ramadhanEvent;

import my.pikrew.ramadhanEvent.commands.CrateCommand;
import my.pikrew.ramadhanEvent.commands.RamadhanTimeCommand;
import my.pikrew.ramadhanEvent.commands.RegionWandCommand;
import my.pikrew.ramadhanEvent.listener.HungerListener;
import my.pikrew.ramadhanEvent.listener.MobDeathListener;
import my.pikrew.ramadhanEvent.listener.RegionWandListener;
import my.pikrew.ramadhanEvent.listener.TransitionTask;
import my.pikrew.ramadhanEvent.manager.CrateManager;
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
    private CrateManager crateManager;
    private RegionWandListener regionWandListener;

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

    public TimeManager getTimeManager() {
        return timeManager;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public SpawnRateManager getSpawnRateManager() {
        return spawnRateManager;
    }

    public DisplayManager getDisplayManager() { return displayManager; }
    public CrateManager getCrateManager()     { return crateManager; }
}