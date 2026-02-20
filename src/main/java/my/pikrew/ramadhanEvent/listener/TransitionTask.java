package my.pikrew.ramadhanEvent.listener;

import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.manager.TimeManager;
import my.pikrew.ramadhanEvent.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TransitionTask extends BukkitRunnable {
    private final JavaPlugin plugin;
    private final TimeManager timeManager;
    private final MessageUtil messageUtil;
    private TimeManager.TimePeriod lastPeriod;
    private boolean hasAnnounced;

    public TransitionTask(JavaPlugin plugin, TimeManager timeManager, MessageUtil messageUtil) {
        this.plugin = plugin;
        this.timeManager = timeManager;
        this.messageUtil = messageUtil;
        this.lastPeriod = timeManager.getCurrentPeriod();
        this.hasAnnounced = false;
    }

    @Override
    public void run() {
        TimeManager.TimePeriod currentPeriod = timeManager.getCurrentPeriod();

        if (currentPeriod != lastPeriod) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[DEBUG] Period transition detected: " + lastPeriod + " -> " + currentPeriod);
                plugin.getLogger().info("[DEBUG] Current time: " + timeManager.getCurrentTimeString());
            }

            if (currentPeriod == TimeManager.TimePeriod.DAY) {
                if (plugin.getConfig().getBoolean("messages.announcements.day.enabled", true)) {
                    messageUtil.broadcastList("messages.announcements.day.text");
                    playSound("messages.announcements.day");
                }
            } else {
                if (plugin.getConfig().getBoolean("messages.announcements.night.enabled", true)) {
                    messageUtil.broadcastList("messages.announcements.night.text");
                    playSound("messages.announcements.night");
                }
            }

            lastPeriod = currentPeriod;
            hasAnnounced = true;
        }
    }

    private void playSound(String configPath) {
        if (!plugin.getConfig().getBoolean(configPath + ".sound.enabled", false)) {
            return;
        }

        String soundName = plugin.getConfig().getString(configPath + ".sound.type", "ENTITY_PLAYER_LEVELUP");
        float volume = (float) plugin.getConfig().getDouble(configPath + ".sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble(configPath + ".sound.pitch", 1.0);

        Sound sound;
        try {
            sound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName + ", using ENTITY_PLAYER_LEVELUP");
            sound = Sound.ENTITY_PLAYER_LEVELUP;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
        }
    }
}