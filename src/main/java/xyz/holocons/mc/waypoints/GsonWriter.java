package xyz.holocons.mc.waypoints;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.UUID;

import com.google.gson.stream.JsonWriter;

import org.bukkit.Location;

public class GsonWriter extends JsonWriter {

    public GsonWriter(final File file) throws IOException {
        super(new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8)));
    }

    public void writeWaypointMap(HashMap<Long, Waypoint> value) throws IOException {
        if (value == null) {
            nullValue();
            return;
        }

        beginArray();
        for (final var waypoint : value.values()) {
            writeWaypoint(waypoint);
        }
        endArray();
    }

    public void writeTravelerMap(HashMap<UUID, Traveler> value) throws IOException {
        if (value == null) {
            nullValue();
            return;
        }

        beginObject();
        for (final var traveler : value.entrySet()) {
            name(traveler.getKey().toString());
            writeTraveler(traveler.getValue());
        }
        endObject();
    }

    public void writeWaypoint(Waypoint value) throws IOException {
        if (value == null) {
            nullValue();
            return;
        }

        beginObject();
        name("id");
        value(value.getId());
        name("location");
        writeLocation(value.getLocation());
        name("contributors");
        writeArrayListUUID(value.getContributors());
        name("active");
        value(value.isActive());
        endObject();
    }

    public void writeTraveler(Traveler value) throws IOException {
        if (value == null) {
            nullValue();
            return;
        }

        beginObject();
        name("charges");
        value(value.getCharges());
        name("tokens");
        value(value.getTokens());
        name("home");
        writeLocation(value.getHome());
        name("camp");
        writeLocation(value.getCamp());
        name("waypoints");
        writeBitSet(value.getWaypoints());
        endObject();
    }

    public void writeLocation(Location value) throws IOException {
        if (value == null) {
            nullValue();
            return;
        }

        var world = value.getWorld().getName();
        var x = Double.toString(value.getX());
        var y = Double.toString(value.getY());
        var z = Double.toString(value.getZ());
        value(world + ',' + x + ',' + y + ',' + z);
    }

    public void writeBitSet(BitSet value) throws IOException {
        if (value == null) {
            nullValue();
            return;
        }

        value(HexFormat.of().formatHex(value.toByteArray()));
    }

    public void writeArrayListUUID(ArrayList<UUID> value) throws IOException {
        if (value == null || value.isEmpty()) {
            nullValue();
            return;
        }

        beginArray();
        for (var uniqueId : value) {
            if (uniqueId == null) {
                continue;
            }

            value(uniqueId.toString());
        }
        endArray();
    }
}
