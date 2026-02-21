package my.pikrew.ramadhanEvent.manager;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Live registry of every tracked ghost mob currently present in the world.
 *
 * <p>Entries enter the registry one tick after {@link GhostSpawnManager} or
 * {@link my.pikrew.ramadhanEvent.listener.GhostSpawnListener} confirms a successful
 * spawn, and are removed by {@link my.pikrew.ramadhanEvent.listener.MobDeathListener}
 * on death or despawn. Everything is keyed by entity UUID so stale references
 * cannot silently accumulate between restarts or reloads.</p>
 *
 * <p>{@link #syncFromWorld(Set)} is a reconciliation pass driven by the command
 * handler before it reports live counts. It queries the MythicMobs API directly for
 * all active entities whose internal name matches one of the tracked keys, adds any
 * that are missing from the local registry, and prunes any entries whose entity has
 * since disappeared. This covers mobs that were spawned before the plugin loaded,
 * placed via {@code /mm m spawn}, or whose spawn event was swallowed for any reason.</p>
 *
 * <p>Note on MythicMobs 5.x compatibility: {@code ActiveMob.getEntity()} returns
 * {@code io.lumine.mythic.api.adapters.AbstractEntity}, not {@code org.bukkit.entity.Entity}.
 * Calling {@code getBukkitEntity()} on it returns the underlying Bukkit object. We cast
 * directly rather than going through {@code BukkitAdapter.adapt()} because the adapter
 * method has an incompatible return type in some 5.x builds.</p>
 */
public class MobTracker {

    private final Map<String, Set<UUID>> liveByType  = new ConcurrentHashMap<>();
    private final Map<UUID, TrackedMob>  entityIndex  = new ConcurrentHashMap<>();

    public void register(String mobKey, Entity entity) {
        UUID id = entity.getUniqueId();
        liveByType.computeIfAbsent(mobKey, k -> ConcurrentHashMap.newKeySet()).add(id);
        entityIndex.put(id, new TrackedMob(mobKey, entity));
    }

    public void unregister(UUID entityId) {
        TrackedMob mob = entityIndex.remove(entityId);
        if (mob == null) return;
        Set<UUID> bucket = liveByType.get(mob.mobKey());
        if (bucket != null) bucket.remove(entityId);
    }

    /**
     * Reconciles the registry against the live MythicMobs world state.
     * Only considers mobs whose internal name is in {@code trackedKeys}.
     */
    public void syncFromWorld(Set<String> trackedKeys) {
        Set<UUID> seen = new HashSet<>();

        for (ActiveMob am : MythicBukkit.inst().getMobManager().getActiveMobs()) {
            String mobKey = am.getType().getInternalName();
            if (!trackedKeys.contains(mobKey)) continue;

            Entity entity = extractEntity(am);
            if (entity == null || !entity.isValid() || entity.isDead()) continue;

            UUID id = entity.getUniqueId();
            seen.add(id);

            if (!entityIndex.containsKey(id)) {
                liveByType.computeIfAbsent(mobKey, k -> ConcurrentHashMap.newKeySet()).add(id);
                entityIndex.put(id, new TrackedMob(mobKey, entity));
            }
        }

        Iterator<Map.Entry<UUID, TrackedMob>> it = entityIndex.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, TrackedMob> entry = it.next();
            if (!seen.contains(entry.getKey()) || !entry.getValue().isValid()) {
                Set<UUID> bucket = liveByType.get(entry.getValue().mobKey());
                if (bucket != null) bucket.remove(entry.getKey());
                it.remove();
            }
        }
    }

    private Entity extractEntity(ActiveMob am) {
        try {
            return (Entity) am.getEntity().getBukkitEntity();
        } catch (Exception e) {
            return null;
        }
    }

    public List<TrackedMob> getLiveFor(String mobKey) {
        Set<UUID> ids = liveByType.getOrDefault(mobKey, Collections.emptySet());
        List<TrackedMob> result = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            TrackedMob mob = entityIndex.get(id);
            if (mob != null) result.add(mob);
        }
        return result;
    }

    public int getLiveCount(String mobKey) {
        Set<UUID> bucket = liveByType.get(mobKey);
        return bucket == null ? 0 : bucket.size();
    }

    public int getTotalLiveCount() {
        return entityIndex.size();
    }

    public void reset() {
        liveByType.clear();
        entityIndex.clear();
    }

    /**
     * Immutable snapshot of a single live ghost mob entity.
     * Holds a direct Bukkit entity reference so location reads reflect
     * wherever the mob has wandered to since it spawned.
     */
    public static class TrackedMob {

        private final String mobKey;
        private final Entity entity;

        TrackedMob(String mobKey, Entity entity) {
            this.mobKey = mobKey;
            this.entity = entity;
        }

        public String   mobKey()   { return mobKey; }
        public Entity   entity()   { return entity; }
        public Location location() { return entity.getLocation(); }
        public boolean  isValid()  { return entity.isValid() && !entity.isDead(); }
    }
}