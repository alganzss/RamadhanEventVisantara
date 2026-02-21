package my.pikrew.ramadhanEvent.manager;

/**
 * Mutable runtime state for a single tracked mob type.
 *
 * <p>Holds the currently-active chance and level bracket (which flip on each day/night
 * transition), plus two counters that accumulate every time this mob is actually
 * spawned — one for the day phase and one for the night phase. These counters are
 * intentionally never reset on phase transitions; they only clear on a full
 * plugin reload so they reflect the complete picture since the last config change.</p>
 *
 * <p>The read-only configured values live in {@link MobProfile}. This class owns
 * all state that changes at runtime.</p>
 */
public class MobSpawnData {

    private final MobProfile profile;

    private double  currentChance;
    private int     currentMinLevel;
    private int     currentMaxLevel;

    private int daySpawnCount   = 0;
    private int nightSpawnCount = 0;

    public MobSpawnData(MobProfile profile) {
        this.profile = profile;
        applyPhase(false);
    }

    /**
     * Switches the active chance and level bracket to match the given phase.
     * Called by {@link SpawnRateManager} whenever day/night transitions.
     */
    public void applyPhase(boolean isNight) {
        currentChance   = profile.chanceFor(isNight);
        currentMinLevel = profile.minLevelFor(isNight);
        currentMaxLevel = profile.maxLevelFor(isNight);
    }

    /**
     * Increments the appropriate phase counter by one.
     * Called each time this mob successfully spawns, whether via the active
     * spawner or through the passive MythicMobs gate.
     */
    public void recordSpawn(boolean isNight) {
        if (isNight) nightSpawnCount++;
        else         daySpawnCount++;
    }

    /** Resets both phase counters to zero. Called on plugin reload. */
    public void resetCounts() {
        daySpawnCount   = 0;
        nightSpawnCount = 0;
    }

    public MobProfile getProfile()         { return profile; }
    public String     getMobKey()          { return profile.getMobKey(); }
    public double     getCurrentChance()   { return currentChance; }
    public int        getCurrentMinLevel() { return currentMinLevel; }
    public int        getCurrentMaxLevel() { return currentMaxLevel; }
    public int        getDaySpawnCount()   { return daySpawnCount; }
    public int        getNightSpawnCount() { return nightSpawnCount; }
    public int        getTotalSpawnCount() { return daySpawnCount + nightSpawnCount; }
}