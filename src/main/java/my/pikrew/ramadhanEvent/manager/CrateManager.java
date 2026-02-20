package my.pikrew.ramadhanEvent.manager;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import my.pikrew.ramadhanEvent.RamadhanEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class CrateManager {

    private final RamadhanEvent plugin;
    private final Map<UUID, CrateData> activeCrates = new ConcurrentHashMap<>();
    private final Set<String> worldsSpawnedIn = ConcurrentHashMap.newKeySet();

    // Per-auto-spawn-cycle broadcast guards
    private boolean autoStartBroadcasted = false;
    private boolean autoHalfBroadcasted  = false;
    private boolean autoExpiredBroadcasted = false;

    public CrateManager(RamadhanEvent plugin) {
        this.plugin = plugin;
    }

    /** Called on /ramadhan reload so crate settings pick up fresh config. */
    public void reloadConfig() {
        // Currently no cached crate config fields; tasks re-read config live.
        // Extend here if caching is added in the future.
    }

    public Map<UUID, CrateData> getActiveCrates() {
        return activeCrates;
    }

    public Optional<CrateData> getCrateByBlock(Block b) {
        if (b == null) return Optional.empty();
        return activeCrates.values().stream()
                .filter(c -> c.getLocation() != null && c.getLocation().getBlock().equals(b))
                .findFirst();
    }

    public void claimCrate(UUID crateId, Player player) {
        CrateData data = activeCrates.get(crateId);
        if (data == null) return;

        handleRewards(player);

        if (!data.isAutoSpawn()) {
            broadcastList("crate.claimed-broadcast", "%player%", player.getName());
        }

        data.cancel();
        activeCrates.remove(crateId);
        if (data.getLocation().getBlock().getType() == Material.PLAYER_HEAD) {
            data.getLocation().getBlock().setType(Material.AIR);
        }
    }

    public UUID spawnCrateAt(Location loc) {
        return spawnCrateAt(loc, false);
    }

    public UUID spawnCrateAt(Location loc, boolean isAutoSpawn) {
        if (loc == null) return null;

        Block block = loc.getBlock();
        block.setType(Material.PLAYER_HEAD);

        String texture = plugin.getConfig().getString("crate.texture", "");
        if (!texture.isEmpty()) applyTextureToBlock(block, texture);

        UUID crateId = UUID.randomUUID();
        int lifetime = plugin.getConfig().getInt("crate.lifetime-seconds", 300);
        CrateData data = new CrateData(crateId, loc, lifetime, isAutoSpawn);
        activeCrates.put(crateId, data);

        if (!isAutoSpawn) {
            String locationString = loc.getWorld().getName()
                    + " " + loc.getBlockX()
                    + " " + loc.getBlockY()
                    + " " + loc.getBlockZ();
            broadcastList("crate.manual-spawn-broadcast", "%location%", locationString);
        }

        data.startTimer();
        return crateId;
    }

    public UUID spawnRandomCrate() {
        FileConfiguration config = plugin.getConfig();
        List<String> allowedWorlds = config.getStringList("crate.region.world");
        if (allowedWorlds.isEmpty()) return null;

        String worldName = allowedWorlds.get(ThreadLocalRandom.current().nextInt(allowedWorlds.size()));
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        int minX = config.getInt("crate.region.min-x", -500);
        int maxX = config.getInt("crate.region.max-x",  500);
        int minZ = config.getInt("crate.region.min-z", -500);
        int maxZ = config.getInt("crate.region.max-z",  500);
        int minY = config.getInt("crate.region.min-y",  60);
        int maxY = config.getInt("crate.region.max-y",  256);

        Random rand = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 50; attempt++) {
            int x = rand.nextInt(maxX - minX + 1) + minX;
            int z = rand.nextInt(maxZ - minZ + 1) + minZ;
            for (int y = minY; y < maxY - 2; y++) {
                Block floor     = world.getBlockAt(x, y,     z);
                Block crateSpot = world.getBlockAt(x, y + 1, z);
                Block airAbove  = world.getBlockAt(x, y + 2, z);
                if (floor.getType().isSolid() && crateSpot.getType().isAir() && airAbove.getType().isAir()) {
                    return spawnCrateAt(new Location(world, x, y + 1, z), true);
                }
            }
        }
        return null;
    }

    public void manualAutoSpawnOnce() {
        FileConfiguration config = plugin.getConfig();
        int amount = config.getInt("crate.amount", 1);

        int successfulSpawns = 0;
        worldsSpawnedIn.clear();

        for (int i = 0; i < amount; i++) {
            UUID id = spawnRandomCrate();
            if (id != null) {
                successfulSpawns++;
                CrateData data = activeCrates.get(id);
                if (data != null && data.getLocation() != null) {
                    worldsSpawnedIn.add(data.getLocation().getWorld().getName());
                }
            }
        }

        if (successfulSpawns > 0) {
            String worldList = String.join(", ", worldsSpawnedIn);
            broadcastList("crate.auto-spawn-broadcast", "%world%", worldList);
        } else {
            plugin.getLogger().warning("[CrateManager] Failed to find safe spawn locations for any crate!");
        }
    }

    public void startAutoSpawnTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                LocalTime now = LocalTime.now();
                List<String> spawnTimes = plugin.getConfig().getStringList("crate.spawn-times");

                for (String t : spawnTimes) {
                    try {
                        String[] split = t.split(":");
                        LocalTime spawnTime = LocalTime.of(
                                Integer.parseInt(split[0]),
                                Integer.parseInt(split[1])
                        );

                        if (now.getHour() == spawnTime.getHour() && now.getMinute() == spawnTime.getMinute()) {
                            if (!autoStartBroadcasted) {
                                autoStartBroadcasted = true;
                                manualAutoSpawnOnce();
                            }
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                autoStartBroadcasted   = false;
                                autoHalfBroadcasted    = false;
                                autoExpiredBroadcasted = false;
                            }, 20L * 60);
                            break;
                        }
                    } catch (Exception ignored) {}
                }
            }
        }.runTaskTimer(plugin, 0L, 20L * 60);
    }

    public void removeCrate(UUID crateId) {
        CrateData data = activeCrates.remove(crateId);
        if (data != null) {
            data.cancel();
            if (data.getLocation().getBlock().getType() == Material.PLAYER_HEAD) {
                data.getLocation().getBlock().setType(Material.AIR);
            }
        }
    }

    public void cleanupAll() {
        for (UUID id : new ArrayList<>(activeCrates.keySet())) {
            removeCrate(id);
        }
        activeCrates.clear();
    }

    public String getActiveCratesStatus() {
        if (!activeCrates.isEmpty()) return plugin.getMessageUtil().getMessage("crate.status.has-spawned");

        List<String> spawnTimes = plugin.getConfig().getStringList("crate.spawn-times");
        if (spawnTimes.isEmpty()) return plugin.getMessageUtil().getMessage("crate.status.not-scheduled");

        LocalTime now = LocalTime.now();
        LocalTime nextSpawn = null;

        for (String t : spawnTimes) {
            try {
                String[] split = t.split(":");
                LocalTime spawnTime = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
                if (spawnTime.isAfter(now)) {
                    nextSpawn = spawnTime;
                    break;
                }
            } catch (Exception ignored) {}
        }

        if (nextSpawn == null) {
            try {
                String[] split = spawnTimes.get(0).split(":");
                nextSpawn = LocalTime.of(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
            } catch (Exception ignored) {
                return plugin.getMessageUtil().getMessage("crate.status.error");
            }
        }

        Duration duration = Duration.between(now, nextSpawn);
        if (duration.isNegative()) duration = duration.plusDays(1);

        long hours   = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return plugin.getMessageUtil().getMessage("crate.status.next-spawn")
                .replace("{hours}",   String.valueOf(hours))
                .replace("{minutes}", String.valueOf(minutes));
    }

    // ---------------------------------------------------------------
    // Skull / texture helpers
    // ---------------------------------------------------------------

    public boolean applyTextureToMeta(SkullMeta meta, String base64) {
        if (meta == null || base64 == null || base64.isEmpty()) return false;
        GameProfile profile = makeProfileFromBase64(base64);
        if (profile == null) return false;
        return injectProfileReflectively(meta, profile);
    }

    private boolean applyTextureToBlock(Block block, String base64) {
        if (block == null || base64 == null || base64.isEmpty()) return false;
        BlockState state = block.getState();
        if (state == null) return false;
        GameProfile profile = makeProfileFromBase64(base64);
        if (profile == null) return false;
        boolean ok = injectProfileReflectively(state, profile);
        if (ok) {
            try { state.update(true, false); } catch (Throwable ignored) {
                try { state.update(); } catch (Throwable ex) { /* best effort */ }
            }
        }
        return ok;
    }

    private GameProfile makeProfileFromBase64(String base64) {
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "RamadhanCrate");
            profile.getProperties().put("textures", new Property("textures", base64));
            return profile;
        } catch (Throwable t) {
            return null;
        }
    }

    private boolean injectProfileReflectively(Object target, GameProfile profile) {
        if (target == null || profile == null) return false;
        try {
            Class<?> clazz = target.getClass();
            Field field = null;
            try { field = clazz.getDeclaredField("profile"); } catch (NoSuchFieldException ignored) {}
            if (field == null) {
                for (Field f : clazz.getDeclaredFields()) {
                    String name = f.getName().toLowerCase(Locale.ROOT);
                    if (name.contains("profile") || name.contains("gameprofile") || name.contains("owner")) {
                        field = f;
                        break;
                    }
                }
            }
            if (field == null) return false;
            field.setAccessible(true);
            Class<?> fieldType = field.getType();
            if (fieldType.isAssignableFrom(GameProfile.class)) {
                field.set(target, profile);
                return true;
            }
            for (Constructor<?> c : fieldType.getDeclaredConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(GameProfile.class)) {
                    c.setAccessible(true);
                    field.set(target, c.newInstance(profile));
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    // ---------------------------------------------------------------
    // Rewards
    // ---------------------------------------------------------------

    private void handleRewards(Player player) {
        List<String> rewards = plugin.getConfig().getStringList("crate.rewards");
        Random random = ThreadLocalRandom.current();

        for (String entry : rewards) {
            entry = entry.trim();
            double chance = 100.0;
            String rewardString = entry;

            if (entry.contains("%|")) {
                try {
                    String[] parts = entry.split("%\\|", 2);
                    chance = Double.parseDouble(parts[0].trim());
                    rewardString = parts[1].trim();
                } catch (Exception e) {
                    plugin.getLogger().warning("[CrateManager] Invalid reward entry: " + entry);
                    continue;
                }
            } else {
                plugin.getLogger().warning("[CrateManager] Deprecated reward format: " + entry + " — use CHANCE%|REWARD:DATA");
            }

            if (random.nextDouble() * 100.0 > chance) continue;

            if (rewardString.startsWith("command:")) {
                String cmd = rewardString.split(":", 2)[1].replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } else if (rewardString.startsWith("item:")) {
                try {
                    String[] parts = rewardString.split(":");
                    Material mat = Material.getMaterial(parts[1].toUpperCase());
                    int qty = Integer.parseInt(parts[2]);
                    if (mat != null) player.getInventory().addItem(new ItemStack(mat, qty));
                } catch (Exception ignored) {}
            }
        }
    }

    // ---------------------------------------------------------------
    // Broadcast via MessageUtil (supports list format)
    // ---------------------------------------------------------------

    /**
     * Broadcasts a message path from messages.yml.
     * If the path is a list, each entry is sent with a 1-tick stagger.
     * A single placeholder pair (token → replacement) is applied to every line.
     */
    private void broadcastList(String path, String token, String replacement) {
        List<String> lines = plugin.getMessageUtil().getMessageList(path);
        if (!lines.isEmpty()) {
            // List format
            for (int i = 0; i < lines.size(); i++) {
                final String line = lines.get(i).replace(token, replacement);
                if (i == 0) {
                    Bukkit.broadcastMessage(line);
                } else {
                    final int delay = i;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.broadcastMessage(line), delay);
                }
            }
        } else {
            // Single string fallback
            String single = plugin.getMessageUtil().getMessage(path).replace(token, replacement);
            if (!single.startsWith("Message not found")) {
                Bukkit.broadcastMessage(single);
            }
        }
    }

    // ---------------------------------------------------------------
    // Inner CrateData
    // ---------------------------------------------------------------

    public class CrateData {
        private final UUID id;
        private final Location location;
        private final int lifetimeSeconds;
        private int ticksLeft;
        private BukkitRunnable task;
        private boolean halfBroadcasted = false;
        private final boolean autoSpawn;

        public CrateData(UUID id, Location location, int lifetimeSeconds, boolean autoSpawn) {
            this.id = id;
            this.location = location;
            this.lifetimeSeconds = lifetimeSeconds;
            this.ticksLeft = lifetimeSeconds;
            this.autoSpawn = autoSpawn;
        }

        public UUID getId()           { return id; }
        public Location getLocation() { return location; }
        public boolean isAutoSpawn()  { return autoSpawn; }

        public void startTimer() {
            final int half = lifetimeSeconds / 2;
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    ticksLeft--;

                    if (autoSpawn && !autoHalfBroadcasted && ticksLeft <= half) {
                        autoHalfBroadcasted = true;
                        broadcastList("crate.half-auto", "%seconds%", String.valueOf(ticksLeft));

                    } else if (!autoSpawn && !halfBroadcasted && ticksLeft <= half) {
                        halfBroadcasted = true;
                        broadcastList("crate.half-manual", "%seconds%", String.valueOf(ticksLeft));
                    }

                    if (ticksLeft <= 0) {
                        if (autoSpawn && !autoExpiredBroadcasted) {
                            autoExpiredBroadcasted = true;
                            broadcastList("crate.expired-auto", "", "");
                        } else if (!autoSpawn) {
                            broadcastList("crate.expired-manual", "", "");
                        }

                        if (location.getBlock().getType() == Material.PLAYER_HEAD) {
                            location.getBlock().setType(Material.AIR);
                        }
                        cancel();
                        activeCrates.remove(id);
                    }
                }
            };
            task.runTaskTimer(plugin, 20L, 20L);
        }

        public void cancel() {
            if (task != null) task.cancel();
        }
    }
}