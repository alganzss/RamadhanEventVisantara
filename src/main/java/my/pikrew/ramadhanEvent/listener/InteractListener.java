package my.pikrew.ramadhanEvent.listener;

import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.manager.CrateManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public class InteractListener implements Listener {

    private final RamadhanEvent plugin;

    public InteractListener(RamadhanEvent plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;

        CrateManager cm = plugin.getCrateManager();
        Optional<CrateManager.CrateData> crateOpt = cm.getCrateByBlock(block);

        if (crateOpt.isPresent()) {
            Player player = event.getPlayer();
            cm.claimCrate(crateOpt.get().getId(), player);
            event.setCancelled(true);
        }
    }
}