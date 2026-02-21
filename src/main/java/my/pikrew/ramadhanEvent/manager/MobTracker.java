package my.pikrew.ramadhanEvent.manager;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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