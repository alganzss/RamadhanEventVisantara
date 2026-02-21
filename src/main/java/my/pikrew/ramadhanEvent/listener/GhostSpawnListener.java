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

import java.util.UUID;

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

        Entity spawned = event.getEntity();

        if (spawned != null && plugin.getGhostSpawnManager() != null) {
            UUID id = spawned.getUniqueId();
            if (plugin.getGhostSpawnManager().getManagedSpawns().contains(id)) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Debug] GhostSpawnManager bypass: " + mobKey + " " + id);
                }
                return;
            }
        }

        if (spawned != null && plugin.getNetherSpawnManager() != null) {
            UUID id = spawned.getUniqueId();
            if (plugin.getNetherSpawnManager().getManagedSpawns().contains(id)) {
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Debug] NetherSpawnManager bypass: " + mobKey + " " + id);
                }
                return;
            }
        }

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Debug] External gate check: '" + mobKey + "' entity="
                    + (spawned != null ? spawned.getUniqueId() : "null"));
        }

        if (!srm.shouldAllowSpawn(mobKey)) {
            event.setCancelled(true);
            return;
        }

        if (spawned == null) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!spawned.isValid() || spawned.isDead()) return;

            srm.getMobTracker().register(mobKey, spawned);

            if (plugin.getConfig().getBoolean("debug", false)) {
                Location loc = spawned.getLocation();
                plugin.getLogger().info("[Debug] Registered external spawn: " + mobKey
                        + " " + spawned.getUniqueId()
                        + " @ " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            }
        }, 1L);
    }
}