package my.pikrew.ramadhanEvent.listener;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent;
import my.pikrew.ramadhanEvent.RamadhanEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

public class MobDeathListener implements Listener {

    private final RamadhanEvent plugin;

    public MobDeathListener(RamadhanEvent plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        UUID id = event.getEntity().getUniqueId();

        plugin.getSpawnRateManager().getMobTracker().unregister(id);

        if (plugin.getNetherSpawnManager() != null) {
            plugin.getNetherSpawnManager().releaseChunk(id);
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            String key = event.getMobType().getInternalName();
            if (!plugin.getSpawnRateManager().getSpawnDataMap().containsKey(key)) return;
            if (!(event.getKiller() instanceof Player player)) return;
            plugin.getLogger().info("[Debug] " + player.getName() + " killed " + key
                    + " phase=" + (plugin.getSpawnRateManager().isNightSurgeActive() ? "night" : "day"));
        }
    }

    @EventHandler
    public void onMythicMobDespawn(MythicMobDespawnEvent event) {
        UUID id = event.getEntity().getUniqueId();

        plugin.getSpawnRateManager().getMobTracker().unregister(id);

        if (plugin.getNetherSpawnManager() != null) {
            plugin.getNetherSpawnManager().releaseChunk(id);
        }
    }
}