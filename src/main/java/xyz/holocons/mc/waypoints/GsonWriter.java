package xyz.holocons.mc.waypoints;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HexFormat;
import java.util.UUID;

import com.google.gson.stream.JsonWriter;

import org.bukkit.Location;

public class GsonWriter extends JsonWriter {

    public GsonWriter(final File file) throws IOException {
        super(new BufferedWriter(new FileWriter(file, Charset.forName("UTF-8"))));
    }

    public void value(Waypoint value) throws IOException {
        if (value == null) {
            nullValue();
            return;
        }

        beginObject();
        name("id");
        value(value.getId());
        name("location");
        value(value.getLocation());
        name("contributors");
        value(value.getContributors());
        name("active");
        value(value.isActive());
        endObject();
    }

    public void value(Traveler value) throws IOException {
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
        value(value.getHome());
        name("camp");
        value(value.getCamp());
        name("waypoints");
        value(value.getWaypoints());
        endObject();
    }

    public void value(Location value) throws IOException {
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

    public void value(BitSet value) throws IOException {
        if (value == null) {
            nullValue();
            return;
        }

        value(HexFormat.of().formatHex(value.toByteArray()));
    }

    public void value(ArrayList<UUID> value) throws IOException {
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
