package my.pikrew.ramadhanEvent.listener;

import my.pikrew.ramadhanEvent.RamadhanEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.UUID;

public class RegionWandListener implements Listener {

    private final RamadhanEvent plugin;
    private final HashMap<UUID, Location> pos1Map = new HashMap<>();
    private final HashMap<UUID, Location> pos2Map = new HashMap<>();

    public RegionWandListener(RamadhanEvent plugin) {
        this.plugin = plugin;
    }

    public HashMap<UUID, Location> getPos1Map() { return pos1Map; }
    public HashMap<UUID, Location> getPos2Map() { return pos2Map; }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        if (!isRegionWand(item) || !player.hasPermission("ramadhan.wand.use")) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_AIR) {
            event.setCancelled(true);
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            Location loc = clickedBlock.getLocation();
            pos1Map.put(player.getUniqueId(), loc);
            player.sendMessage(plugin.getMessageUtil().getMessage("commands.wand.pos1-set")
                    .replace("{location}", formatLocation(loc)));

        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            if (player.isSneaking()) {
                player.sendMessage(plugin.getMessageUtil().getMessage("commands.wand.save-hint"));
                return;
            }
            Location loc = clickedBlock.getLocation();
            pos2Map.put(player.getUniqueId(), loc);
            player.sendMessage(plugin.getMessageUtil().getMessage("commands.wand.pos2-set")
                    .replace("{location}", formatLocation(loc)));
        }
    }

    private boolean isRegionWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        FileConfiguration config = plugin.getConfig();
        Material required = Material.getMaterial(config.getString("wand-item.material", "GOLDEN_AXE"));
        if (item.getType() != required) return false;
        ItemMeta meta = item.getItemMeta();
        String requiredName = ChatColor.translateAlternateColorCodes('&',
                config.getString("wand-item.name", "&b&lRegion Wand"));
        return meta.hasDisplayName() && meta.getDisplayName().equals(requiredName);
    }

    private String formatLocation(Location loc) {
        return loc.getWorld().getName()
                + " (" + loc.getBlockX()
                + ", " + loc.getBlockY()
                + ", " + loc.getBlockZ() + ")";
    }
}