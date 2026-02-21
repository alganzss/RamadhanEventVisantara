package my.pikrew.ramadhanEvent.manager;

import my.pikrew.ramadhanEvent.RamadhanEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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