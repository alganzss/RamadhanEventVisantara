package my.pikrew.ramadhanEvent.manager;

import my.pikrew.ramadhanEvent.RamadhanEvent;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeManager {

    public enum TimePeriod {
        DAY,
        NIGHT
    }

    private final RamadhanEvent plugin;
    private LocalTime dayStartTime;
    private LocalTime nightStartTime;
    private ZoneId timezone;

    public TimeManager(RamadhanEvent plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        String timezoneStr = plugin.getConfig().getString("timezone", "GMT+07:00");
        try {
            timezone = ZoneId.of(timezoneStr);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid timezone: " + timezoneStr + ", falling back to system default.");
            timezone = ZoneId.systemDefault();
        }

        dayStartTime = parseTime(plugin.getConfig().getString("times.day", "8:00"));
        nightStartTime = parseTime(plugin.getConfig().getString("times.night", "18:00"));

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Debug] TimeManager reloaded | Day: " + dayStartTime
                    + " | Night: " + nightStartTime + " | Zone: " + timezone);
        }
    }

    private LocalTime parseTime(String raw) {
        try {
            String[] parts = raw.split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                plugin.getLogger().warning("Invalid time value: " + raw + ", using 00:00.");
                return LocalTime.MIDNIGHT;
            }
            return LocalTime.of(hour, minute);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse time: " + raw + ", using 00:00.");
            return LocalTime.MIDNIGHT;
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
        if (dayStartTime.isBefore(nightStartTime)) {
            boolean isDay = !now.isBefore(dayStartTime) && now.isBefore(nightStartTime);
            return isDay ? TimePeriod.DAY : TimePeriod.NIGHT;
        }
        boolean isNight = !now.isBefore(nightStartTime) || now.isBefore(dayStartTime);
        return isNight ? TimePeriod.NIGHT : TimePeriod.DAY;
    }

    public boolean isRamadhanTime() {
        return getCurrentPeriod() == TimePeriod.DAY;
    }

    public boolean isNightSurge() {
        return getCurrentPeriod() == TimePeriod.NIGHT;
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    public LocalTime getDayStartTime() {
        return dayStartTime;
    }

    public LocalTime getNightStartTime() {
        return nightStartTime;
    }

    public String getTimeRangeString() {
        return String.format("%02d:%02d - %02d:%02d",
                dayStartTime.getHour(), dayStartTime.getMinute(),
                nightStartTime.getHour(), nightStartTime.getMinute());
    }
}