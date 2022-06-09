package xyz.holocons.mc.waypoints;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;

public final class DataType {

    public static final PersistentDataType<byte[], Location> LOCATION = new PersistentDataType<byte[], Location>() {

        private static final int BYTES = Double.BYTES * 3 + Long.BYTES * 2;

        @Override
        public Class<byte[]> getPrimitiveType() {
            return byte[].class;
        }

        @Override
        public Class<Location> getComplexType() {
            return Location.class;
        }

        @Override
        public byte[] toPrimitive(Location complex, PersistentDataAdapterContext context) {
            final var x = complex.getX();
            final var y = complex.getY();
            final var z = complex.getZ();
            final var world = complex.isWorldLoaded() ? complex.getWorld().getUID() : new UUID(0, 0);

            final var buffer = ByteBuffer.wrap(new byte[BYTES]);
            buffer.putDouble(x);
            buffer.putDouble(y);
            buffer.putDouble(z);
            buffer.putLong(world.getMostSignificantBits());
            buffer.putLong(world.getLeastSignificantBits());
            return buffer.array();
        }

        @Override
        public Location fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
            final var buffer = ByteBuffer.wrap(primitive);
            final var x = buffer.getDouble();
            final var y = buffer.getDouble();
            final var z = buffer.getDouble();
            final var firstLong = buffer.getLong();
            final var secondLong = buffer.getLong();

            final var world = Bukkit.getWorld(new UUID(firstLong, secondLong));
            return new Location(world, x, y, z);
        }
    };

    public static final PersistentDataType<byte[], UUID> UUID = new PersistentDataType<byte[], UUID>() {

        private static final int BYTES = Long.BYTES * 2;

        @Override
        public Class<byte[]> getPrimitiveType() {
            return byte[].class;
        }

        @Override
        public Class<UUID> getComplexType() {
            return UUID.class;
        }

        @Override
        public byte[] toPrimitive(UUID complex, PersistentDataAdapterContext context) {
            final var buffer = ByteBuffer.wrap(new byte[BYTES]);
            buffer.putLong(complex.getMostSignificantBits());
            buffer.putLong(complex.getLeastSignificantBits());
            return buffer.array();
        }

        @Override
        public UUID fromPrimitive(byte[] primitive, PersistentDataAdapterContext context) {
            final var buffer = ByteBuffer.wrap(primitive);
            final var firstLong = buffer.getLong();
            final var secondLong = buffer.getLong();
            return new UUID(firstLong, secondLong);
        }
    };
}
