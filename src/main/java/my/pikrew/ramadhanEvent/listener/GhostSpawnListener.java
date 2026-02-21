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

/**
 * Passive chance gate untuk spawn yang diinisiasi MythicMobs sendiri
 * (natural spawners, /mm m spawn, skill spawns, dsb.).
 *
 * <p>BUG #3 FIX: Spawn yang berasal dari {@link my.pikrew.ramadhanEvent.manager.GhostSpawnManager}
 * kita sendiri ditandai di {@code managedSpawns} set. Listener ini akan melewati
 * chance-gate untuk UUID tersebut agar tidak terjadi double-gate yang menyebabkan
 * mob plugin kita sendiri di-cancel setelah sudah lolos roll pertama.</p>
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

        // Bukan mob yang kita track — biarkan lewat
        if (!srm.getSpawnDataMap().containsKey(mobKey)) return;

        // BUG #3 FIX: Cek apakah entity ini di-spawn oleh GhostSpawnManager kita.
        // Jika ya, skip gate — chance sudah dievaluasi di manager, jangan di-cancel.
        Entity spawned = event.getEntity();
        if (spawned != null && plugin.getGhostSpawnManager() != null) {
            UUID id = spawned.getUniqueId();
            if (plugin.getGhostSpawnManager().getManagedSpawns().contains(id)) {
                // Spawn dari manager kita — langsung register tanpa chance-roll ulang
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[Debug] Managed spawn bypass for: " + mobKey + " " + id);
                }
                return;
            }
        }

        // Spawn dari luar (MythicMobs natural/manual) — lakukan chance-gate normal
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Debug] Gate check: '" + mobKey + "' entity="
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
                plugin.getLogger().info("[Debug] Registered " + mobKey + " " + spawned.getUniqueId()
                        + " @ " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            }
        }, 1L);
    }
}