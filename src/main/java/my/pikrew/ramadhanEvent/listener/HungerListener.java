package my.pikrew.ramadhanEvent.listener;

import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.manager.TimeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class HungerListener implements Listener {

    private final RamadhanEvent plugin;
    private final TimeManager timeManager;

    public HungerListener(RamadhanEvent plugin, TimeManager timeManager) {
        this.plugin = plugin;
        this.timeManager = timeManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!timeManager.isRamadhanTime()) return;

        int currentLevel = player.getFoodLevel();
        int proposedLevel = event.getFoodLevel();

        if (proposedLevel <= currentLevel) return;

        double multiplier = plugin.getConfig().getDouble("ramadhan-time.food-consumption-multiplier", 0.5);

        int gain = proposedLevel - currentLevel;
        int reducedGain = (int) Math.ceil(gain * multiplier);
        int finalLevel = Math.min(20, currentLevel + reducedGain);

        event.setFoodLevel(finalLevel);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(
                    "[DEBUG] Food gain reduced: %d + %d -> %d + %d = %d (multiplier: %.2fx)"
                            .formatted(currentLevel, gain, currentLevel, reducedGain, finalLevel, multiplier)
            );
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExhaustion(EntityExhaustionEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!timeManager.isRamadhanTime()) return;

        double multiplier = plugin.getConfig().getDouble("ramadhan-time.hunger-loss-multiplier", 2.0);

        float originalExhaustion = event.getExhaustion();
        float scaledExhaustion = (float) (originalExhaustion * multiplier);

        event.setExhaustion(scaledExhaustion);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info(
                    "[DEBUG] Exhaustion scaled: %.4f -> %.4f for %s (multiplier: %.2fx)"
                            .formatted(originalExhaustion, scaledExhaustion, player.getName(), multiplier)
            );
        }
    }
}