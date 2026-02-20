package my.pikrew.ramadhanEvent.manager;

import my.pikrew.ramadhanEvent.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class DisplayManager {
    private final JavaPlugin plugin;
    private final MessageUtil messageUtil;
    private final TimeManager timeManager;
    private BossBar bossBar;
    private BukkitRunnable actionBarTask;
    private BukkitRunnable bossBarTask;

    public DisplayManager(JavaPlugin plugin, MessageUtil messageUtil, TimeManager timeManager) {
        this.plugin = plugin;
        this.messageUtil = messageUtil;
        this.timeManager = timeManager;
    }

    public void start() {
        if (plugin.getConfig().getBoolean("displays.action-bar.enabled", false)) {
            startActionBar();
        }

        if (plugin.getConfig().getBoolean("displays.boss-bar.enabled", false)) {
            startBossBar();
        }
    }

    public void stop() {
        if (actionBarTask != null && !actionBarTask.isCancelled()) {
            actionBarTask.cancel();
        }

        if (bossBarTask != null && !bossBarTask.isCancelled()) {
            bossBarTask.cancel();
        }

        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
    }

    public void reload() {
        stop();
        start();
    }

    private void startActionBar() {
        long updateInterval = plugin.getConfig().getLong("displays.action-bar.update-interval", 20L);

        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!timeManager.isRamadhanTime()) {
                    return;
                }

                String message = messageUtil.getMessage("displays.action-bar.text")
                        .replace("{time}", timeManager.getCurrentTimeString())
                        .replace("{period}", timeManager.getCurrentPeriod().name());

                for (Player player : Bukkit.getOnlinePlayers()) {
                    sendActionBar(player, message);
                }
            }
        };

        actionBarTask.runTaskTimer(plugin, 0L, updateInterval);
    }

    private void startBossBar() {
        String title = messageUtil.getMessage("displays.boss-bar.title")
                .replace("{time}", timeManager.getCurrentTimeString());

        String colorStr = plugin.getConfig().getString("displays.boss-bar.color", "YELLOW");
        String styleStr = plugin.getConfig().getString("displays.boss-bar.style", "SOLID");

        BarColor color;
        try {
            color = BarColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BarColor.YELLOW;
            plugin.getLogger().warning("Invalid boss bar color: " + colorStr + ", using YELLOW");
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(styleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
            plugin.getLogger().warning("Invalid boss bar style: " + styleStr + ", using SOLID");
        }

        // Create boss bar
        bossBar = Bukkit.createBossBar(title, color, style);
        bossBar.setVisible(false); // Start hidden

        long updateInterval = plugin.getConfig().getLong("displays.boss-bar.update-interval", 20L);

        bossBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeManager.isRamadhanTime()) {
                    if (!bossBar.isVisible()) {
                        bossBar.setVisible(true);
                    }

                    String updatedTitle = messageUtil.getMessage("displays.boss-bar.title")
                            .replace("{time}", timeManager.getCurrentTimeString())
                            .replace("{period}", timeManager.getCurrentPeriod().name());
                    bossBar.setTitle(updatedTitle);

                    double progress = calculateProgress();
                    bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (!bossBar.getPlayers().contains(player)) {
                            bossBar.addPlayer(player);
                        }
                    }
                } else {
                    if (bossBar.isVisible()) {
                        bossBar.setVisible(false);
                        bossBar.removeAll();
                    }
                }
            }
        };

        bossBarTask.runTaskTimer(plugin, 0L, updateInterval);
    }

    private double calculateProgress() {
        try {
            java.time.LocalTime now = timeManager.getCurrentTime();
            java.time.LocalTime dayStart = timeManager.getDayStartTime();
            java.time.LocalTime nightStart = timeManager.getNightStartTime();

            long totalSeconds = java.time.Duration.between(dayStart, nightStart).getSeconds();
            long elapsedSeconds = java.time.Duration.between(dayStart, now).getSeconds();

            if (totalSeconds <= 0) return 0.0;

            return 1.0 - ((double) elapsedSeconds / (double) totalSeconds);
        } catch (Exception e) {
            return 0.5;
        }
    }

    private void sendActionBar(Player player, String message) {
        try {
            try {
                player.spigot().sendMessage(
                        net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message)
                );
                return;
            } catch (Exception ignored) {}

            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + version + ".entity.CraftPlayer");
            Object craftPlayer = craftPlayerClass.cast(player);

            Class<?> packetClass = Class.forName("net.minecraft.server." + version + ".PacketPlayOutChat");
            Class<?> chatComponentClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent");
            Class<?> chatSerializerClass = Class.forName("net.minecraft.server." + version + ".IChatBaseComponent$ChatSerializer");

            Method serializeMethod = chatSerializerClass.getDeclaredMethod("a", String.class);
            Object chatComponent = serializeMethod.invoke(null, "{\"text\":\"" + message + "\"}");

            Constructor<?> packetConstructor = packetClass.getConstructor(chatComponentClass, byte.class);
            Object packet = packetConstructor.newInstance(chatComponent, (byte) 2);

            Method getHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
            Object entityPlayer = getHandleMethod.invoke(craftPlayer);

            Object playerConnection = entityPlayer.getClass().getDeclaredField("playerConnection").get(entityPlayer);
            Method sendPacketMethod = playerConnection.getClass().getDeclaredMethod("sendPacket",
                    Class.forName("net.minecraft.server." + version + ".Packet"));
            sendPacketMethod.invoke(playerConnection, packet);
        } catch (Exception e) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("[DEBUG] Failed to send action bar: " + e.getMessage());
            }
        }
    }
}
