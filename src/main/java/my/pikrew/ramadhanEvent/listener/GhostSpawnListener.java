package my.pikrew.ramadhanEvent.listener;

import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.manager.SpawnRateManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class GhostSpawnListener implements Listener {

    private final SpawnRateManager spawnRateManager;

    public GhostSpawnListener(RamadhanEvent plugin) {
        this.spawnRateManager = plugin.getSpawnRateManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        String mobKey = event.getMobType().getInternalName();

        if (!spawnRateManager.getSpawnDataMap().containsKey(mobKey)) return;

        if (!spawnRateManager.shouldAllowSpawn(mobKey)) {
            event.setCancelled(true);
        }
    }
}