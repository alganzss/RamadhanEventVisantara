package my.pikrew.ramadhanEvent.manager;

import io.lumine.mythic.bukkit.MythicBukkit;
import my.pikrew.ramadhanEvent.RamadhanEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GhostSpawnManager {

    private final RamadhanEvent   plugin;
    private final SpawnRateManager spawnRateManager;
    private final Random           rng = new Random();

    private BukkitTask task;

    public GhostSpawnManager(RamadhanEvent plugin, SpawnRateManager spawnRateManager) {
        this.plugin           = plugin;
        this.spawnRateManager = spawnRateManager;
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
            MythicBukkit.inst().getMobManager().spawnMob(data.getMobKey(), loc, level);
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

    private Location findSurface(Location origin) {
        int radius   = plugin.getConfig().getInt("ghost-spawn.spawn-radius",       32);
        int attempts = plugin.getConfig().getInt("ghost-spawn.location-attempts",  10);
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