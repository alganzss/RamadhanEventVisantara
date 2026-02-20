package my.pikrew.ramadhanEvent.manager;

import my.pikrew.ramadhanEvent.RamadhanEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SpawnRateManager {

    private final RamadhanEvent plugin;
    private final TimeManager timeManager;
    private final Random random = new Random();

    private Map<String, MobSpawnData> spawnDataMap = Collections.emptyMap();
    private boolean nightSurgeActive = false;

    public SpawnRateManager(RamadhanEvent plugin, TimeManager timeManager) {
        this.plugin = plugin;
        this.timeManager = timeManager;
    }

    public void start() {
        loadMobData();
        syncPhase();
    }

    public void stop() {
        spawnDataMap = Collections.emptyMap();
    }

    public void reload() {
        loadMobData();
        syncPhase();
    }

    private void loadMobData() {
        List<String> mobKeys = plugin.getConfig().getStringList("ghost-spawn.mob-keys");
        Map<String, MobSpawnData> fresh = new LinkedHashMap<>(mobKeys.size());

        for (String key : mobKeys) {
            if (!plugin.getConfig().getBoolean("mobs." + key + ".enabled", true)) continue;

            MobProfile profile = new MobProfile(
                    key,
                    plugin.getConfig().getDouble("mobs." + key + ".day-chance", 0.05),
                    plugin.getConfig().getDouble("mobs." + key + ".night-chance", 0.15),
                    plugin.getConfig().getInt("mobs." + key + ".day-min-level", 1),
                    plugin.getConfig().getInt("mobs." + key + ".day-max-level", 3),
                    plugin.getConfig().getInt("mobs." + key + ".night-min-level", 5),
                    plugin.getConfig().getInt("mobs." + key + ".night-max-level", 10)
            );

            fresh.put(key, new MobSpawnData(profile));
        }

        spawnDataMap = Collections.unmodifiableMap(fresh);
    }

    public void syncPhase() {
        boolean isNight = timeManager.isNightSurge();
        if (isNight == nightSurgeActive) return;

        nightSurgeActive = isNight;

        for (MobSpawnData data : spawnDataMap.values()) {
            data.applyPhase(isNight);
        }

        plugin.getLogger().info("Ghost spawn phase -> " + (isNight ? "Night Surge" : "Day")
                + " at " + timeManager.getCurrentTimeString());
    }

    public boolean shouldAllowSpawn(String mobKey) {
        MobSpawnData data = spawnDataMap.get(mobKey);
        if (data == null) return true;

        boolean allowed = random.nextDouble() < data.getCurrentChance();

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Debug] Spawn check for " + mobKey
                    + " | Chance: " + data.getCurrentChance()
                    + " | Allowed: " + allowed);
        }

        return allowed;
    }

    public void forceRefresh() {
        boolean isNight = timeManager.isNightSurge();
        nightSurgeActive = isNight;
        for (MobSpawnData data : spawnDataMap.values()) {
            data.applyPhase(isNight);
        }
    }

    public Map<String, MobSpawnData> getSpawnDataMap() {
        return spawnDataMap;
    }

    public boolean isNightSurgeActive() {
        return nightSurgeActive;
    }
}
