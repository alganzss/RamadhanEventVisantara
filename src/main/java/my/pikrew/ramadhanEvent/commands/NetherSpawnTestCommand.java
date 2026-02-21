package my.pikrew.ramadhanEvent.commands;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import my.pikrew.ramadhanEvent.RamadhanEvent;
import my.pikrew.ramadhanEvent.manager.MobSpawnData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class NetherSpawnTestCommand implements CommandExecutor, TabCompleter {

    private static final List<String> PHASES = Arrays.asList("day", "night");
    private static final int MAX_CYCLES = 50;

    private final RamadhanEvent plugin;

    public NetherSpawnTestCommand(RamadhanEvent plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!sender.hasPermission("ramadhanevent.admin")) {
            sender.sendMessage(Component.text("No permission.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender, label);
            return true;
        }

        String phaseArg = args[0].toLowerCase(Locale.ROOT);
        if (!phaseArg.equals("day") && !phaseArg.equals("night")) {
            sender.sendMessage(Component.text("Phase must be 'day' or 'night'.").color(NamedTextColor.RED));
            return true;
        }

        int cycles = 10;
        try {
            cycles = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Cycles must be a number (1-" + MAX_CYCLES + ").").color(NamedTextColor.RED));
            return true;
        }

        cycles = Math.max(1, Math.min(MAX_CYCLES, cycles));
        boolean simulateNight = phaseArg.equals("night");

        runTest(sender, simulateNight, cycles);
        return true;
    }

    private void runTest(CommandSender sender, boolean simulateNight, int cycles) {
        sender.sendMessage(Component.text("=== Nether Spawn Test ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Phase : " + (simulateNight ? "Night" : "Day")).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Cycles: " + cycles).color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Clearing tracked mobs...").color(NamedTextColor.GRAY));

        int cleared = clearTrackedMobs();
        sender.sendMessage(Component.text("Cleared " + cleared + " mob(s).").color(NamedTextColor.AQUA));

        plugin.getSpawnRateManager().getMobTracker().reset();
        for (MobSpawnData data : plugin.getSpawnRateManager().getSpawnDataMap().values()) {
            data.resetCounts();
        }

        boolean wasNight = plugin.getSpawnRateManager().isNightSurgeActive();
        plugin.getSpawnRateManager().getSpawnDataMap().values()
                .forEach(d -> d.applyPhase(simulateNight));

        sender.sendMessage(Component.text("Running " + cycles + " cycle(s)...").color(NamedTextColor.YELLOW));

        List<SpawnRecord> spawned = Collections.synchronizedList(new ArrayList<>());

        scheduleCycles(sender, simulateNight, cycles, wasNight, spawned);
    }

    private void scheduleCycles(CommandSender sender, boolean simulateNight, int remaining,
                                boolean originalNight, List<SpawnRecord> spawned) {
        if (remaining <= 0) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    reportResults(sender, simulateNight, originalNight, spawned), 5L);
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            runSingleCycle(simulateNight, spawned);
            scheduleCycles(sender, simulateNight, remaining - 1, originalNight, spawned);
        }, 2L);
    }

    private void runSingleCycle(boolean isNight, List<SpawnRecord> records) {
        List<World> netherWorlds = new ArrayList<>();
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NETHER) netherWorlds.add(w);
        }

        List<String> configured = plugin.getConfig().getStringList("nether-spawn.worlds");
        if (!configured.isEmpty()) {
            netherWorlds.clear();
            for (String name : configured) {
                World w = Bukkit.getWorld(name);
                if (w != null) netherWorlds.add(w);
            }
        }

        if (netherWorlds.isEmpty()) return;

        boolean forceLoad = plugin.getConfig().getBoolean("nether-spawn.force-load-chunks", true);
        Random rng = new Random();

        for (MobSpawnData data : plugin.getSpawnRateManager().getSpawnDataMap().values()) {
            double chance = getNetherChance(data, isNight);
            if (rng.nextDouble() >= chance) continue;

            World world = netherWorlds.get(rng.nextInt(netherWorlds.size()));
            Location loc = findNetherSurface(world, rng);
            if (loc == null) continue;

            int levelRange = data.getCurrentMaxLevel() - data.getCurrentMinLevel();
            int level = data.getCurrentMinLevel() + (levelRange > 0 ? rng.nextInt(levelRange + 1) : 0);

            Chunk chunk = loc.getChunk();
            if (forceLoad) chunk.setForceLoaded(true);

            try {
                ActiveMob activeMob = (ActiveMob) MythicBukkit.inst()
                        .getMobManager()
                        .spawnMob(data.getMobKey(), loc, level);

                data.recordSpawn(isNight);
                records.add(new SpawnRecord(data.getMobKey(), level, loc));

                if (forceLoad && activeMob != null && plugin.getNetherSpawnManager() != null) {
                    try {
                        Entity entity = (Entity) activeMob.getEntity().getBukkitEntity();
                        if (entity != null) {
                            plugin.getNetherSpawnManager().getForcedChunks();
                        }
                    } catch (Exception ignored) {}
                }

            } catch (Exception e) {
                if (forceLoad) chunk.setForceLoaded(false);
                plugin.getLogger().warning("[NetherTest] Could not spawn " + data.getMobKey() + ": " + e.getMessage());
            }
        }
    }

    private void reportResults(CommandSender sender, boolean simulatedNight,
                               boolean originalNight, List<SpawnRecord> spawned) {

        plugin.getSpawnRateManager().getSpawnDataMap().values()
                .forEach(d -> d.applyPhase(originalNight));

        sender.sendMessage(Component.text("=== Test Results ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Total spawned: " + spawned.size()).color(NamedTextColor.YELLOW));

        if (spawned.isEmpty()) {
            sender.sendMessage(Component.text("No mobs spawned. Check: nether world exists, mob keys are correct, region is valid.").color(NamedTextColor.RED));
        }

        Map<String, Integer> countByKey = new LinkedHashMap<>();
        for (SpawnRecord r : spawned) countByKey.merge(r.mobKey, 1, Integer::sum);
        for (Map.Entry<String, Integer> e : countByKey.entrySet()) {
            sender.sendMessage(Component.text("  " + e.getKey() + " x" + e.getValue()).color(NamedTextColor.AQUA));
        }

        if (!spawned.isEmpty()) {
            sender.sendMessage(Component.text("--- Locations (click to TP) ---").color(NamedTextColor.GOLD));
            for (int i = 0; i < spawned.size(); i++) {
                SpawnRecord r = spawned.get(i);
                String label = "#" + (i + 1) + " " + r.mobKey + " lv" + r.level;
                if (sender instanceof Player player) {
                    sendTpLink(player, label, r.location);
                } else {
                    sender.sendMessage(Component.text(
                            "  " + label + " -> "
                                    + r.location.getWorld().getName()
                                    + " " + r.location.getBlockX()
                                    + " " + r.location.getBlockY()
                                    + " " + r.location.getBlockZ()
                    ).color(NamedTextColor.WHITE));
                }
            }
        }

        sender.sendMessage(Component.text("--- Per-Mob Totals ---").color(NamedTextColor.GOLD));
        for (MobSpawnData data : plugin.getSpawnRateManager().getSpawnDataMap().values()) {
            sender.sendMessage(Component.text(
                    "  " + data.getMobKey()
                            + "  day=" + data.getDaySpawnCount()
                            + "  night=" + data.getNightSpawnCount()
                            + "  total=" + data.getTotalSpawnCount()
            ).color(NamedTextColor.WHITE));
        }

        sender.sendMessage(Component.text("Phase restored to: " + (originalNight ? "Night" : "Day") + ". Chunks stay loaded until mobs die.").color(NamedTextColor.GRAY));
    }

    private int clearTrackedMobs() {
        Set<String> tracked = plugin.getSpawnRateManager().getSpawnDataMap().keySet();
        int count = 0;
        for (ActiveMob am : new ArrayList<>(MythicBukkit.inst().getMobManager().getActiveMobs())) {
            if (!tracked.contains(am.getType().getInternalName())) continue;
            try {
                Entity entity = (Entity) am.getEntity().getBukkitEntity();
                if (entity != null && entity.isValid()) {
                    entity.remove();
                    count++;
                }
            } catch (Exception ignored) {}
        }
        return count;
    }

    private double getNetherChance(MobSpawnData data, boolean isNight) {
        String key = "mobs." + data.getMobKey() + ".nether-"
                + (isNight ? "night" : "day") + "-chance";
        if (plugin.getConfig().contains(key)) {
            return plugin.getConfig().getDouble(key, data.getCurrentChance());
        }
        return data.getCurrentChance();
    }

    private Location findNetherSurface(World world, Random rng) {
        int attempts = plugin.getConfig().getInt("nether-spawn.location-attempts", 15);
        int minX = plugin.getConfig().getInt("nether-spawn.region.min-x", -200);
        int maxX = plugin.getConfig().getInt("nether-spawn.region.max-x",  200);
        int minZ = plugin.getConfig().getInt("nether-spawn.region.min-z", -200);
        int maxZ = plugin.getConfig().getInt("nether-spawn.region.max-z",  200);
        int minY = plugin.getConfig().getInt("nether-spawn.region.min-y",  32);
        int maxY = plugin.getConfig().getInt("nether-spawn.region.max-y",  100);

        for (int i = 0; i < attempts; i++) {
            int x = minX + rng.nextInt(Math.max(1, maxX - minX + 1));
            int z = minZ + rng.nextInt(Math.max(1, maxZ - minZ + 1));
            for (int y = maxY; y >= minY; y--) {
                if (!world.getBlockAt(x, y, z).getType().isSolid()) continue;
                if (!world.getBlockAt(x, y + 1, z).getType().isAir()) continue;
                if (!world.getBlockAt(x, y + 2, z).getType().isAir()) continue;
                return new Location(world, x + 0.5, y + 1.0, z + 0.5);
            }
        }
        return null;
    }

    private void sendTpLink(Player player, String label, Location loc) {
        int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
        String world = loc.getWorld().getName();
        String cmd = "/ramadhanintegrationmobs _tp " + world + " " + bx + " " + by + " " + bz;

        TextComponent line = new TextComponent("  ");
        TextComponent link = new TextComponent(label + " [" + bx + " " + by + " " + bz + "]");
        link.setColor(ChatColor.WHITE);
        link.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd));
        link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(ChatColor.YELLOW + "Click to teleport")));
        line.addExtra(link);
        player.spigot().sendMessage(line);
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("Usage: /" + label + " <day|night> <cycles>").color(NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("  Clears mobs, forces phase, runs nether cycles, reports coords.").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Spawned mobs have chunks force-loaded — TP to verify them.").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("  Example: /" + label + " night 20").color(NamedTextColor.AQUA));
    }

    private static class SpawnRecord {
        final String   mobKey;
        final int      level;
        final Location location;

        SpawnRecord(String mobKey, int level, Location location) {
            this.mobKey   = mobKey;
            this.level    = level;
            this.location = location;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            for (String p : PHASES) {
                if (p.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(p);
            }
            return out;
        }
        return List.of();
    }
}