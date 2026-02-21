package my.pikrew.ramadhanEvent.manager;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import my.pikrew.ramadhanEvent.RamadhanEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GhostSpawnManager {

    private final RamadhanEvent    plugin;
    private final SpawnRateManager spawnRateManager;
    private final Random           rng = new Random();

    private final Set<UUID> managedSpawns = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private BukkitTask task;

    public GhostSpawnManager(RamadhanEvent plugin, SpawnRateManager spawnRateManager) {
        this.plugin           = plugin;
        this.spawnRateManager = spawnRateManager;
    }

    public Set<UUID> getManagedSpawns() {
        return managedSpawns;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("ghost-spawn.plugin-spawner", true)) return;

        long interval = plugin.getConfig().getLong("ghost-spawn.interval-ticks", 200L);

        task = new BukkitRunnable() {
            @Override
            public void run() {
                runCycle();
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void reload() {
        stop();
        start();
    }

    private void runCycle() {
        List<? extends org.bukkit.entity.Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (online.isEmpty()) return;

        org.bukkit.entity.Player target = online.get(rng.nextInt(online.size()));
        boolean isNight = spawnRateManager.isNightSurgeActive();

        for (MobSpawnData data : spawnRateManager.getSpawnDataMap().values()) {
            if (rng.nextDouble() >= data.getCurrentChance()) continue;

            Location loc = findSurface(target.getLocation());
            if (loc == null) continue;

            int levelRange = data.getCurrentMaxLevel() - data.getCurrentMinLevel();
            int level      = data.getCurrentMinLevel() + (levelRange > 0 ? rng.nextInt(levelRange + 1) : 0);

            trySpawn(data, loc, level, isNight);
        }
    }

    private void trySpawn(MobSpawnData data, Location loc, int level, boolean isNight) {
        try {
            ActiveMob activeMob = MythicBukkit.inst().getMobManager().spawnMob(data.getMobKey(), loc, level);

            if (activeMob == null) {
                plugin.getLogger().warning("[GhostSpawn] spawnMob returned null for: " + data.getMobKey());
                return;
            }

            Entity entity = extractEntity(activeMob);
            if (entity != null) {
                UUID id = entity.getUniqueId();
                managedSpawns.add(id);

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    managedSpawns.remove(id);
                    if (entity.isValid() && !entity.isDead()) {
                        spawnRateManager.getMobTracker().register(data.getMobKey(), entity);
                    }
                }, 2L);
            }

            data.recordSpawn(isNight);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[GhostSpawn] " + data.getMobKey()
                        + " lv" + level + " @ "
                        + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()
                        + " [" + (isNight ? "night" : "day") + "]");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[GhostSpawn] Could not spawn " + data.getMobKey() + ": " + e.getMessage());
        }
    }

    private Entity extractEntity(ActiveMob am) {
        try {
            return (Entity) am.getEntity().getBukkitEntity();
        } catch (Exception e) {
            return null;
        }
    }

    private Location findSurface(Location origin) {
        int radius   = plugin.getConfig().getInt("ghost-spawn.spawn-radius",      32);
        int attempts = plugin.getConfig().getInt("ghost-spawn.location-attempts", 10);
        World world  = origin.getWorld();

        for (int i = 0; i < attempts; i++) {
            int dx = rng.nextInt(radius * 2 + 1) - radius;
            int dz = rng.nextInt(radius * 2 + 1) - radius;

            int x = origin.getBlockX() + dx;
            int z = origin.getBlockZ() + dz;
            int y = world.getHighestBlockYAt(x, z);

            if (y <= world.getMinHeight()) continue;

            Location candidate = new Location(world, x + 0.5, y + 1.0, z + 0.5);

            if (!candidate.getBlock().getType().isAir()) continue;
            if (!candidate.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) continue;

            return candidate;
        }

        return null;
    }
}