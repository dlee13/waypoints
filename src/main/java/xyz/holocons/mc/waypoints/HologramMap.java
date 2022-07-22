package xyz.holocons.mc.waypoints;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

public class HologramMap {

    public record FakeEntity(int entityId, UUID uniqueId) {

        private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

        @SuppressWarnings("deprecation")
        public FakeEntity() {
            this(Bukkit.getUnsafe().nextEntityId(), new UUID(RANDOM.nextLong(), RANDOM.nextLong()));
        }
    }

    private final ProtocolManager protocolManager;
    private final HashMap<Hologram, Integer> holograms;

    public HologramMap() {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.holograms = new HashMap<>();
    }

    public void show(Waypoint waypoint, Player player) {
        show(waypoint, List.of(player));
    }

    public void show(Waypoint waypoint, Collection<? extends Player> players) {
        final var fakeEntity = new FakeEntity();
        final var spawnPacket = Hologram.getSpawnPacket(fakeEntity.entityId, fakeEntity.uniqueId, waypoint);
        players.forEach(player -> {
            protocolManager.sendServerPacket(player, spawnPacket);
            final var hologram = new Hologram(waypoint, player);
            final var entityId = holograms.put(hologram, fakeEntity.entityId);
            if (entityId != null) {
                final var destroyPacket = Hologram.getDestroyPacket(entityId);
                protocolManager.sendServerPacket(player, destroyPacket);
            }
        });
        update(waypoint, players);
    }

    public void showTrackedPlayers(Waypoint waypoint, Player player) {
        show(waypoint, getTrackedPlayers(player));
    }

    public void update(Waypoint waypoint, Player player) {
        final var hologram = new Hologram(waypoint, player);
        final var entityId = holograms.get(hologram);
        if (entityId != null) {
            final var packet = Hologram.getMetadataPacket(entityId, waypoint);
            protocolManager.sendServerPacket(player, packet);
        }
    }

    public void update(Waypoint waypoint, Collection<? extends Player> players) {
        players.forEach(player -> update(waypoint, player));
    }

    public void updateTrackedPlayers(Waypoint waypoint, Player player) {
        update(waypoint, getTrackedPlayers(player));
    }

    public void hide(Waypoint waypoint, Player player) {
        final var hologram = new Hologram(waypoint, player);
        final var entityId = holograms.remove(hologram);
        if (entityId != null) {
            final var packet = Hologram.getDestroyPacket(entityId);
            protocolManager.sendServerPacket(player, packet);
        }
    }

    public void hide(Waypoint waypoint, Collection<? extends Player> players) {
        players.forEach(player -> hide(waypoint, player));
    }

    public void hideTrackedPlayers(Waypoint waypoint, Player player) {
        hide(waypoint, getTrackedPlayers(player));
    }

    public void remove(Waypoint waypoint) {
        hide(waypoint, Bukkit.getOnlinePlayers());
    }

    public void remove(Player player) {
        final var uniqueId = player.getUniqueId();
        holograms.keySet().removeIf(hologram -> hologram.uniqueId() == uniqueId);
    }

    private static Collection<Player> getTrackedPlayers(Player player) {
        final var players = player.getTrackedPlayers();
        players.add(player);
        return players;
    }
}
