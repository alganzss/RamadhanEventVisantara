package my.pikrew.ramadhanEvent.manager;

public class MobProfile {

    private final String mobKey;
    private final double dayChance;
    private final double nightChance;
    private final int dayMinLevel;
    private final int dayMaxLevel;
    private final int nightMinLevel;
    private final int nightMaxLevel;

    public MobProfile(String mobKey, double dayChance, double nightChance,
                      int dayMinLevel, int dayMaxLevel, int nightMinLevel, int nightMaxLevel) {
        this.mobKey = mobKey;
        this.dayChance = dayChance;
        this.nightChance = nightChance;
        this.dayMinLevel = dayMinLevel;
        this.dayMaxLevel = dayMaxLevel;
        this.nightMinLevel = nightMinLevel;
        this.nightMaxLevel = nightMaxLevel;
    }

    public String getMobKey() { return mobKey; }
    public double getDayChance() { return dayChance; }
    public double getNightChance() { return nightChance; }
    public int getDayMinLevel() { return dayMinLevel; }
    public int getDayMaxLevel() { return dayMaxLevel; }
    public int getNightMinLevel() { return nightMinLevel; }
    public int getNightMaxLevel() { return nightMaxLevel; }

    public double chanceFor(boolean nightSurge) {
        return nightSurge ? nightChance : dayChance;
    }

    public int minLevelFor(boolean nightSurge) {
        return nightSurge ? nightMinLevel : dayMinLevel;
    }

    public int maxLevelFor(boolean nightSurge) {
        return nightSurge ? nightMaxLevel : dayMaxLevel;
    }
}