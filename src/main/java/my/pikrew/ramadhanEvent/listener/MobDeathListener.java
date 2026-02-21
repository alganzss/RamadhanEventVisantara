package my.pikrew.ramadhanEvent.listener;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import io.lumine.mythic.bukkit.events.MythicMobDespawnEvent;
import my.pikrew.ramadhanEvent.RamadhanEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Removes ghost mobs from {@link my.pikrew.ramadhanEvent.manager.MobTracker}
 * when they die or despawn, keeping the live count accurate.
 *
 * <p>Both events are handled here because MythicMobs fires {@link MythicMobDespawnEvent}
 * when a mob unloads without dying (chunk unload, cleanup commands, etc.), which would
 * otherwise leave a stale entry in the tracker indefinitely.</p>
 */
public class MobDeathListener implements Listener {

    private final RamadhanEvent plugin;

    public MobDeathListener(RamadhanEvent plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        plugin.getSpawnRateManager().getMobTracker().unregister(event.getEntity().getUniqueId());

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
        plugin.getSpawnRateManager().getMobTracker().unregister(event.getEntity().getUniqueId());
    }
}