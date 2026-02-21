package my.pikrew.ramadhanEvent.manager;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import my.pikrew.ramadhanEvent.RamadhanEvent;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class NetherSpawnManager {

    private final RamadhanEvent    plugin;
    private final SpawnRateManager spawnRateManager;
    private final Random           rng = new Random();

    private BukkitTask task;

    private final Set<UUID> managedSpawns = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<UUID, Chunk> forcedChunks = new ConcurrentHashMap<>();

    public NetherSpawnManager(RamadhanEvent plugin, SpawnRateManager spawnRateManager) {
        this.plugin           = plugin;
        this.spawnRateManager = spawnRateManager;
    }

    /** Diakses oleh GhostSpawnListener untuk bypass check. */
    public Set<UUID> getManagedSpawns() {
        return managedSpawns;
    }

    public void start() {
        if (!plugin.getConfig().getBoolean("nether-spawn.enabled", false)) return;

        long interval = plugin.getConfig().getLong("nether-spawn.interval-ticks", 400L);

        task = Bukkit.getScheduler().runTaskTimer(plugin, this::runCycle, interval, interval);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        releaseAllChunks();
    }

    public void reload() {
        stop();
        start();
    }

    public void releaseChunk(UUID entityId) {
        Chunk chunk = forcedChunks.remove(entityId);
        if (chunk == null) return;

        if (!plugin.getConfig().getBoolean("nether-spawn.force-load-chunks", true)) return;

        boolean stillNeeded = forcedChunks.values().stream().anyMatch(c ->
                c.getWorld().equals(chunk.getWorld())
                        && c.getX() == chunk.getX()
                        && c.getZ() == chunk.getZ());

        if (!stillNeeded) {
            chunk.setForceLoaded(false);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[NetherSpawn] Released chunk "
                        + chunk.getWorld().getName()
                        + " [" + chunk.getX() + "," + chunk.getZ() + "]");
            }
        }
    }

    public Map<UUID, Chunk> getForcedChunks() {
        return Collections.unmodifiableMap(forcedChunks);
    }

    private void runCycle() {
        List<World> netherWorlds = resolveNetherWorlds();
        if (netherWorlds.isEmpty()) return;

        boolean isNight = spawnRateManager.isNightSurgeActive();

        for (MobSpawnData data : spawnRateManager.getSpawnDataMap().values()) {
            double chance = getNetherChance(data, isNight);
            if (rng.nextDouble() >= chance) continue;

            World world = netherWorlds.get(rng.nextInt(netherWorlds.size()));
            Location loc = findNetherSurface(world);
            if (loc == null) continue;

            int levelRange = data.getCurrentMaxLevel() - data.getCurrentMinLevel();
            int level = data.getCurrentMinLevel() + (levelRange > 0 ? rng.nextInt(levelRange + 1) : 0);

            spawnMob(data, loc, level, isNight);
        }
    }

    private void spawnMob(MobSpawnData data, Location loc, int level, boolean isNight) {
        boolean forceLoad = plugin.getConfig().getBoolean("nether-spawn.force-load-chunks", true);

        Chunk chunk = loc.getChunk();
        if (forceLoad) {
            chunk.setForceLoaded(true);
        }

        try {
            ActiveMob activeMob = (ActiveMob) MythicBukkit.inst()
                    .getMobManager()
                    .spawnMob(data.getMobKey(), loc, level);

            if (activeMob == null) {
                if (forceLoad) chunk.setForceLoaded(false);
                plugin.getLogger().warning(
                        "[NetherSpawn] spawnMob() returned null for " + data.getMobKey()
                                + " — mob may not exist in MythicMobs or spawn was blocked.");
                return;
            }

            Entity entity = null;
            try {
                entity = (Entity) activeMob.getEntity().getBukkitEntity();
            } catch (Exception ignored) {}

            if (entity == null || !entity.isValid()) {
                if (forceLoad) chunk.setForceLoaded(false);
                plugin.getLogger().warning(
                        "[NetherSpawn] Entity invalid after spawn for " + data.getMobKey());
                return;
            }

            // FIX: Tandai UUID agar GhostSpawnListener tidak double-gate spawn ini.
            // managedSpawns dibersihkan setelah 2 tick (setelah MythicMobSpawnEvent terlewati).
            UUID id = entity.getUniqueId();
            managedSpawns.add(id);

            final Entity finalEntity = entity;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                managedSpawns.remove(id);
                // Register ke MobTracker setelah entity fully initialized
                if (finalEntity.isValid() && !finalEntity.isDead()) {
                    spawnRateManager.getMobTracker().register(data.getMobKey(), finalEntity);
                }
            }, 2L);

            // Commit side effects setelah semua validasi lolos
            data.recordSpawn(isNight);

            if (forceLoad) {
                forcedChunks.put(id, chunk);
            }

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(
                        "[NetherSpawn] " + data.getMobKey()
                                + " lv" + level
                                + " -> " + loc.getWorld().getName()
                                + " x=" + loc.getBlockX()
                                + " y=" + loc.getBlockY()
                                + " z=" + loc.getBlockZ()
                                + " [" + (isNight ? "night" : "day") + "]"
                                + (forceLoad ? " [chunk locked]" : "")
                );
            }

        } catch (Exception e) {
            if (forceLoad) chunk.setForceLoaded(false);
            plugin.getLogger().log(Level.WARNING,
                    "[NetherSpawn] Failed to spawn " + data.getMobKey() + ": " + e.getMessage());
        }
    }

    private void releaseAllChunks() {
        if (!plugin.getConfig().getBoolean("nether-spawn.force-load-chunks", true)) {
            forcedChunks.clear();
            return;
        }

        Set<String> released = ConcurrentHashMap.newKeySet();
        for (Chunk chunk : forcedChunks.values()) {
            String key = chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
            if (released.add(key)) {
                chunk.setForceLoaded(false);
            }
        }
        forcedChunks.clear();
        plugin.getLogger().info("[NetherSpawn] Released all forced chunks.");
    }

    private List<World> resolveNetherWorlds() {
        List<String> configured = plugin.getConfig().getStringList("nether-spawn.worlds");
        List<World> result = new ArrayList<>();

        if (!configured.isEmpty()) {
            for (String name : configured) {
                World w = Bukkit.getWorld(name);
                if (w != null) result.add(w);
            }
            return result;
        }

        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NETHER) result.add(w);
        }
        return result;
    }

    private Location findNetherSurface(World world) {
        int attempts = plugin.getConfig().getInt("nether-spawn.location-attempts", 15);
        int minX = plugin.getConfig().getInt("nether-spawn.region.min-x", -200);
        int maxX = plugin.getConfig().getInt("nether-spawn.region.max-x",  200);
        int minZ = plugin.getConfig().getInt("nether-spawn.region.min-z", -200);
        int maxZ = plugin.getConfig().getInt("nether-spawn.region.max-z",  200);
        int minY = plugin.getConfig().getInt("nether-spawn.region.min-y",  32);
        int maxY = plugin.getConfig().getInt("nether-spawn.region.max-y",  100);

        for (int i = 0; i < attempts; i++) {
            int x = minX + rng.nextInt(Math.max(1, maxX - minX + 1));
            int z = minZ + rng.nextInt(Math.max(1, maxZ - minZ + 1));

            for (int y = maxY; y >= minY; y--) {
                Block floor = world.getBlockAt(x, y, z);
                if (!floor.getType().isSolid()) continue;

                Block spot  = world.getBlockAt(x, y + 1, z);
                Block above = world.getBlockAt(x, y + 2, z);

                if (spot.getType().isAir() && above.getType().isAir()
                        && spot.getType() != Material.LAVA
                        && above.getType() != Material.LAVA) {
                    return new Location(world, x + 0.5, y + 1.0, z + 0.5);
                }
            }
        }
        return null;
    }

    private double getNetherChance(MobSpawnData data, boolean isNight) {
        String key = "mobs." + data.getMobKey() + ".nether-"
                + (isNight ? "night" : "day") + "-chance";
        if (plugin.getConfig().contains(key)) {
            return plugin.getConfig().getDouble(key, data.getCurrentChance());
        }
        return data.getCurrentChance();
    }
}