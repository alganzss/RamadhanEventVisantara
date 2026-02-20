package my.pikrew.ramadhanEvent.manager;

public class MobSpawnData {

    private final MobProfile profile;
    private double currentChance;
    private boolean nightSurgeActive;
    private int currentMinLevel;
    private int currentMaxLevel;

    public MobSpawnData(MobProfile profile) {
        this.profile = profile;
        this.currentChance = profile.getDayChance();
        this.currentMinLevel = profile.getDayMinLevel();
        this.currentMaxLevel = profile.getDayMaxLevel();
        this.nightSurgeActive = false;
    }

    public void applyPhase(boolean nightSurge) {
        this.nightSurgeActive = nightSurge;
        this.currentChance = profile.chanceFor(nightSurge);
        this.currentMinLevel = profile.minLevelFor(nightSurge);
        this.currentMaxLevel = profile.maxLevelFor(nightSurge);
    }

    public MobProfile getProfile() { return profile; }
    public String getMobKey() { return profile.getMobKey(); }
    public double getCurrentChance() { return currentChance; }
    public boolean isNightSurgeActive() { return nightSurgeActive; }
    public int getCurrentMinLevel() { return currentMinLevel; }
    public int getCurrentMaxLevel() { return currentMaxLevel; }
}