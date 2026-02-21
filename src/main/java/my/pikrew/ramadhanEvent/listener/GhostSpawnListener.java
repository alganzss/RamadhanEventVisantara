package my.pikrew.ramadhanEvent.listener;

import io.lumine.mythic.bukkit.events.MythicMobSpawnEvent;
import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.manager.SpawnRateManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Passive chance gate for MythicMobs-triggered spawns.
 *
 * <p>Intercepts {@link MythicMobSpawnEvent} for mob types tracked in
 * {@link SpawnRateManager}. If the mob fails the configured chance roll
 * the event is cancelled before the entity enters the world. If it passes,
 * registration into {@link my.pikrew.ramadhanEvent.manager.MobTracker} is
 * deferred by one tick to ensure the entity is fully initialised in the world
 * before we store a reference — some MythicMobs builds fire the event before
 * the entity is considered valid by Bukkit.</p>
 *
 * <p>This listener only covers spawns that MythicMobs initiates (natural
 * spawners, {@code /mm m spawn}, skill spawns, etc.). Spawns produced by
 * {@link my.pikrew.ramadhanEvent.manager.GhostSpawnManager} bypass this
 * listener entirely; they record themselves directly after a successful call
 * to the MythicMobs spawn API.</p>
 */
public class GhostSpawnListener implements Listener {

    private final RamadhanEvent plugin;

    public GhostSpawnListener(RamadhanEvent plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onMythicMobSpawn(MythicMobSpawnEvent event) {
        String           mobKey = event.getMobType().getInternalName();
        SpawnRateManager srm    = plugin.getSpawnRateManager();

        if (!srm.getSpawnDataMap().containsKey(mobKey)) return;

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Debug] Gate check: '" + mobKey + "' entity="
                    + (event.getEntity() != null ? event.getEntity().getUniqueId() : "null"));
        }

        if (!srm.shouldAllowSpawn(mobKey)) {
            event.setCancelled(true);
            return;
        }

        Entity spawned = event.getEntity();
        if (spawned == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!spawned.isValid() || spawned.isDead()) return;

            srm.getMobTracker().register(mobKey, spawned);

            if (plugin.getConfig().getBoolean("debug", false)) {
                Location loc = spawned.getLocation();
                plugin.getLogger().info("[Debug] Registered " + mobKey + " " + spawned.getUniqueId()
                        + " @ " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            }
        }, 1L);
    }
}