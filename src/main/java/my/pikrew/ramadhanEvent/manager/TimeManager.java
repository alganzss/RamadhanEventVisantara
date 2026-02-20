package my.pikrew.ramadhanEvent.manager;

import my.pikrew.ramadhanEvent.RamadhanEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeManager {
    private final JavaPlugin plugin;
    private LocalTime dayStartTime;
    private LocalTime nightStartTime;
    private ZoneId timezone;
    private boolean debugMode;

    public enum TimePeriod {
        DAY,
        NIGHT
    }

    public TimeManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String timezoneStr = plugin.getConfig().getString("timezone", "GMT+07:00");
        try {
            timezone = ZoneId.of(timezoneStr.replace("GMT", "GMT"));
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid timezone: " + timezoneStr + ", using system default");
            timezone = ZoneId.systemDefault();
        }

        dayStartTime = parseTime(plugin.getConfig().getString("times.day", "8:00"));
        nightStartTime = parseTime(plugin.getConfig().getString("times.night", "18:00"));

        debugMode = plugin.getConfig().getBoolean("debug", false);

        if (debugMode) {
            plugin.getLogger().info("[DEBUG] TimeManager reloaded:");
            plugin.getLogger().info("[DEBUG] Day start: " + dayStartTime);
            plugin.getLogger().info("[DEBUG] Night start: " + nightStartTime);
            plugin.getLogger().info("[DEBUG] Timezone: " + timezone);
            plugin.getLogger().info("[DEBUG] Current time: " + getCurrentTimeString());
        }
    }

    private LocalTime parseTime(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;

            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                plugin.getLogger().warning("Invalid time: " + timeStr + ", using 0:00");
                return LocalTime.of(0, 0);
            }

            return LocalTime.of(hour, minute);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse time: " + timeStr + ", using 0:00");
            return LocalTime.of(0, 0);
        }
    }

    public LocalTime getCurrentTime() {
        return ZonedDateTime.now(timezone).toLocalTime();
    }

    public String getCurrentTimeString() {
        LocalTime now = getCurrentTime();
        return String.format("%02d:%02d", now.getHour(), now.getMinute());
    }

    public TimePeriod getCurrentPeriod() {
        LocalTime now = getCurrentTime();

        if (debugMode) {
            plugin.getLogger().info("[DEBUG] Checking period - Current: " + now +
                    ", Day start: " + dayStartTime +
                    ", Night start: " + nightStartTime);
        }

        if (dayStartTime.isBefore(nightStartTime)) {
            boolean isDayPeriod = !now.isBefore(dayStartTime) && now.isBefore(nightStartTime);
            if (debugMode) {
                plugin.getLogger().info("[DEBUG] Normal case - Is day period: " + isDayPeriod);
            }
            return isDayPeriod ? TimePeriod.DAY : TimePeriod.NIGHT;
        }
        else {
            boolean isNightPeriod = !now.isBefore(nightStartTime) && now.isBefore(dayStartTime);
            if (debugMode) {
                plugin.getLogger().info("[DEBUG] Overnight case - Is night period: " + isNightPeriod);
            }
            return isNightPeriod ? TimePeriod.NIGHT : TimePeriod.DAY;
        }
    }

    public boolean isRamadhanTime() {
        return getCurrentPeriod() == TimePeriod.DAY;
    }

    public String getTimeRangeString() {
        return String.format("%02d:%02d - %02d:%02d",
                dayStartTime.getHour(), dayStartTime.getMinute(),
                nightStartTime.getHour(), nightStartTime.getMinute());
    }

    public LocalTime getDayStartTime() {
        return dayStartTime;
    }

    public LocalTime getNightStartTime() {
        return nightStartTime;
    }

    public ZoneId getTimezone() {
        return timezone;
    }
}