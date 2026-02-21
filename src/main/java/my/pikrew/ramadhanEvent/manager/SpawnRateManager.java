package my.pikrew.ramadhanEvent.manager;

import my.pikrew.ramadhanEvent.RamadhanEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Central authority for ghost mob spawn configuration at runtime.
 *
 * <p>Reads each tracked mob's day/night chances and level ranges from config.yml on
 * startup (and on reload), then exposes two points of integration:
 * <ul>
 *   <li>{@link #shouldAllowSpawn(String)} – called by the passive gate listener to
 *       decide whether a MythicMobs-triggered spawn should proceed.</li>
 *   <li>{@link MobSpawnData} values, consumed directly by {@link GhostSpawnManager}
 *       for the plugin's own active spawn cycle.</li>
 * </ul>
 * </p>
 *
 * <p>Both consumers share the same {@link MobSpawnData} instances, so the phase-aware
 * spawn counters ({@link MobSpawnData#getDaySpawnCount()},
 * {@link MobSpawnData#getNightSpawnCount()}) accumulate regardless of which path
 * produced the spawn. That means the test/report command always shows a unified total.</p>
 *
 * <p>{@link #simulateSpawnCheck(String, boolean, int)} performs a statistical dry-run —
 * it rolls the configured chance {@code n} times and returns the success count, letting
 * admins verify their config produces the expected rate without waiting for live spawns.</p>
 */
public class SpawnRateManager {

    private final RamadhanEvent plugin;
    private final TimeManager   timeManager;
    private final Random        rng        = new Random();
    private final MobTracker    mobTracker = new MobTracker();

    private Map<String, MobSpawnData> spawnDataMap     = Collections.emptyMap();
    private boolean                   nightSurgeActive = false;

    public SpawnRateManager(RamadhanEvent plugin, TimeManager timeManager) {
        this.plugin      = plugin;
        this.timeManager = timeManager;
    }

    public void start() {
        loadMobData();
        syncPhase();
    }

    public void reload() {
        mobTracker.reset();
        loadMobData();
        syncPhase();
    }

    private void loadMobData() {
        List<String> keys = plugin.getConfig().getStringList("ghost-spawn.mob-keys");
        Map<String, MobSpawnData> fresh = new LinkedHashMap<>(keys.size());

        for (String key : keys) {
            String path = "mobs." + key;
            if (!plugin.getConfig().getBoolean(path + ".enabled", true)) continue;

            MobProfile profile = new MobProfile(
                    key,
                    plugin.getConfig().getDouble(path + ".day-chance",      0.05),
                    plugin.getConfig().getDouble(path + ".night-chance",    0.15),
                    plugin.getConfig().getInt(path    + ".day-min-level",   1),
                    plugin.getConfig().getInt(path    + ".day-max-level",   3),
                    plugin.getConfig().getInt(path    + ".night-min-level", 5),
                    plugin.getConfig().getInt(path    + ".night-max-level", 10)
            );

            fresh.put(key, new MobSpawnData(profile));
        }

        spawnDataMap = Collections.unmodifiableMap(fresh);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Debug] Loaded " + spawnDataMap.size() + " mob profiles.");
        }
    }

    public void syncPhase() {
        boolean isNight = timeManager.isNightSurge();
        if (isNight == nightSurgeActive && !spawnDataMap.isEmpty()) return;

        nightSurgeActive = isNight;
        spawnDataMap.values().forEach(d -> d.applyPhase(isNight));

        plugin.getLogger().info("Ghost spawn phase -> " + (isNight ? "Night Surge" : "Day")
                + " at " + timeManager.getCurrentTimeString());
    }

    public void forceRefresh() {
        nightSurgeActive = timeManager.isNightSurge();
        spawnDataMap.values().forEach(d -> d.applyPhase(nightSurgeActive));
    }

    /**
     * Evaluates a single spawn gate roll for the given mob type.
     * Records the spawn in the phase counter when the roll succeeds.
     *
     * @return {@code true} if the spawn should proceed, {@code false} to cancel it.
     *         Unknown mob keys always return {@code true} (not our responsibility).
     */
    public boolean shouldAllowSpawn(String mobKey) {
        MobSpawnData data = spawnDataMap.get(mobKey);
        if (data == null) return true;

        boolean allowed = rng.nextDouble() < data.getCurrentChance();
        if (allowed) data.recordSpawn(nightSurgeActive);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Debug] Gate: " + mobKey
                    + " chance=" + String.format("%.2f", data.getCurrentChance())
                    + " pass=" + allowed);
        }

        return allowed;
    }

    /**
     * Runs a statistical simulation of spawn rolls for reporting purposes.
     * Does not spawn anything or mutate any state.
     *
     * @param mobKey        the MythicMobs internal name
     * @param simulateNight {@code true} to use night-phase chance, {@code false} for day
     * @param trials        number of rolls to run, clamped to [1, 10000]
     * @return number of successful rolls, or -1 if the mob key is not tracked
     */
    public int simulateSpawnCheck(String mobKey, boolean simulateNight, int trials) {
        MobSpawnData data = spawnDataMap.get(mobKey);
        if (data == null) return -1;

        double chance    = data.getProfile().chanceFor(simulateNight);
        int    clamped   = Math.max(1, Math.min(10_000, trials));
        int    successes = 0;

        for (int i = 0; i < clamped; i++) {
            if (rng.nextDouble() < chance) successes++;
        }

        return successes;
    }

    public Map<String, MobSpawnData> getSpawnDataMap()  { return spawnDataMap; }
    public boolean                   isNightSurgeActive() { return nightSurgeActive; }
    public MobTracker                getMobTracker()     { return mobTracker; }
}