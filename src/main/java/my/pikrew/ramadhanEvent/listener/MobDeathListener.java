package my.pikrew.ramadhanEvent.listener;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.manager.SpawnRateManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MobDeathListener implements Listener {

    private final RamadhanEvent plugin;
    private final SpawnRateManager SpawnRateManager;

    public MobDeathListener(RamadhanEvent plugin) {
        this.plugin = plugin;
        this.SpawnRateManager = plugin.getSpawnRateManager();
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent event) {
        String internalName = event.getMobType().getInternalName();
        if (!SpawnRateManager.getSpawnDataMap().containsKey(internalName)) return;
        if (!(event.getKiller() instanceof Player player)) return;

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[Debug] " + player.getName() + " killed " + internalName
                    + " | Night Surge: " + SpawnRateManager.isNightSurgeActive());
        }
    }
}