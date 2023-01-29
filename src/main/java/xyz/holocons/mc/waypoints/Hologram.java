package xyz.holocons.mc.waypoints;

import java.util.Optional;
import java.util.UUID;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.Registry;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public record Hologram(long chunkKey, UUID uniqueId) {

    public Hologram(Waypoint waypoint, Player player) {
        this(waypoint.getChunkKey(), player.getUniqueId());
    }

    private static final Vector HOLOGRAM_POSITION_OFFSET = new Vector(0.5, 1.6, 0.5);

    // https://nms.screamingsandals.org/1.19.3/net/minecraft/network/protocol/game/ClientboundAddEntityPacket.html
    // https://wiki.vg/Protocol#Spawn_Entity
    public static PacketContainer getSpawnPacket(int entityId, UUID uniqueId, Waypoint waypoint) {
        var location = waypoint.getLocation().add(HOLOGRAM_POSITION_OFFSET);
        var packet = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
        packet.getIntegers()
                .write(0, entityId) // id
                .write(1, 0) // xa
                .write(2, 0) // ya
                .write(3, 0) // za
                .write(4, 0); // data
        packet.getUUIDs()
                .write(0, uniqueId); // uuid
        packet.getEntityTypeModifier()
                .write(0, EntityType.ARMOR_STAND); // type
        packet.getDoubles()
                .write(0, location.getX()) // x
                .write(1, location.getY()) // y
                .write(2, location.getZ()); // z
        packet.getBytes()
                .write(0, (byte) 0) // xRot
                .write(1, (byte) 0) // yRot
                .write(2, (byte) 0); // yHeadRot
        return packet;
    }

    // https://nms.screamingsandals.org/1.19.3/net/minecraft/network/protocol/game/ClientboundSetEntityDataPacket.html
    // https://wiki.vg/Protocol#Set_Entity_Metadata
    // https://wiki.vg/Entity_metadata#Entity_Metadata_Format
    public static PacketContainer getMetadataPacket(int entityId, Waypoint waypoint) {
        var name = WrappedChatComponent.fromJson(GsonComponentSerializer.gson().serialize(waypoint.getDisplayName()));
        var metadata = ObjectList.of(
                new WrappedDataValue(0, Registry.get(Byte.class), (byte) 0x20),
                new WrappedDataValue(2, Registry.getChatComponentSerializer(true), Optional.of(name.getHandle())),
                new WrappedDataValue(3, Registry.get(Boolean.class), true),
                new WrappedDataValue(15, Registry.get(Byte.class), (byte) (0x08 | 0x10)));
        var packet = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers()
                .write(0, entityId); // id
        packet.getDataValueCollectionModifier()
                .write(0, metadata); // packedItems
        return packet;
    }

    // https://nms.screamingsandals.org/1.19.3/net/minecraft/network/protocol/game/ClientboundRemoveEntitiesPacket.html
    // https://wiki.vg/Protocol#Remove_Entities
    public static PacketContainer getDestroyPacket(int... entityId) {
        var entityIds = IntList.of(entityId);
        var packet = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
        packet.getIntLists()
                .write(0, entityIds); // entityIds
        return packet;
    }
}
