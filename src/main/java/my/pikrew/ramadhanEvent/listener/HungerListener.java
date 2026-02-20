package my.pikrew.ramadhanEvent.listener;

import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.manager.TimeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class HungerListener implements Listener {
    private final RamadhanEvent plugin;
    private final TimeManager timeManager;

    public HungerListener(RamadhanEvent plugin, TimeManager timeManager) {
        this.plugin = plugin;
        this.timeManager = timeManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        int currentLevel = player.getFoodLevel();
        int newLevel = event.getFoodLevel();

        if (timeManager.isRamadhanTime()) {
            if (newLevel < currentLevel) {
                double multiplier = plugin.getConfig().getDouble("ramadhan-time.hunger-loss-multiplier", 2.0);
                int difference = currentLevel - newLevel;
                int multipliedDifference = (int) Math.ceil(difference * multiplier);

                int finalLevel = Math.max(0, currentLevel - multipliedDifference);
                event.setFoodLevel(finalLevel);

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[DEBUG] Hunger loss multiplied: " + difference + " -> " + multipliedDifference + " (x" + multiplier + ")");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        if (timeManager.isRamadhanTime()) {
            double foodMultiplier = plugin.getConfig().getDouble("ramadhan-time.food-consumption-multiplier", 0.5);

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                int currentFood = player.getFoodLevel();
                float currentSaturation = player.getSaturation();

                int reducedFood = (int) Math.ceil(currentFood * foodMultiplier);
                float reducedSaturation = currentSaturation * (float) foodMultiplier;

                player.setFoodLevel(Math.min(20, reducedFood));
                player.setSaturation(Math.min(20f, reducedSaturation));

                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("[DEBUG] Food consumption reduced: Food=" + currentFood + " -> " + reducedFood + ", Sat=" + currentSaturation + " -> " + reducedSaturation);
                }
            }, 1L);
        }
    }
}
