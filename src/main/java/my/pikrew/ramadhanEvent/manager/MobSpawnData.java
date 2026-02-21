package my.pikrew.ramadhanEvent.manager;

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

    public void applyPhase(boolean isNight) {
        currentChance   = profile.chanceFor(isNight);
        currentMinLevel = profile.minLevelFor(isNight);
        currentMaxLevel = profile.maxLevelFor(isNight);
    }

    public void recordSpawn(boolean isNight) {
        if (isNight) nightSpawnCount++;
        else         daySpawnCount++;
    }

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