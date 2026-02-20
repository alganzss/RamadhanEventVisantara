package my.pikrew.ramadhanEvent.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MessageUtil {
    private final JavaPlugin plugin;
    private File messagesFile;
    private FileConfiguration messagesConfig;

    public MessageUtil(JavaPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reloadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG] Messages.yml reloaded");
        }
    }

    public String getMessage(String path) {
        String message = messagesConfig.getString(path, "Message not found: " + path);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public List<String> getMessageList(String path) {
        List<String> messages = messagesConfig.getStringList(path);
        List<String> coloredMessages = new ArrayList<>();

        for (String message : messages) {
            coloredMessages.add(ChatColor.translateAlternateColorCodes('&', message));
        }

        return coloredMessages;
    }

    public void broadcast(String path) {
        String message = getMessage(path);
        Bukkit.broadcastMessage(message);
    }

    public void broadcastList(String path) {
        List<String> messages = getMessageList(path);

        if (messages.isEmpty()) {
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().warning("[DEBUG] No messages found for path: " + path);
            }
            return;
        }

        Bukkit.broadcastMessage(messages.get(0));

        for (int i = 1; i < messages.size(); i++) {
            final String msg = messages.get(i);
            final int delay = i; // Delay in ticks

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Bukkit.broadcastMessage(msg);
            }, delay);
        }
    }

    public void sendMessage(Player player, String path) {
        player.sendMessage(getMessage(path));
    }

    public void sendMessageList(Player player, String path) {
        List<String> messages = getMessageList(path);

        if (messages.isEmpty()) {
            return;
        }

        player.sendMessage(messages.get(0));

        for (int i = 1; i < messages.size(); i++) {
            final String msg = messages.get(i);
            final int delay = i;

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                player.sendMessage(msg);
            }, delay);
        }
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);

        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", replacements[i]);
        }

        return message;
    }
}