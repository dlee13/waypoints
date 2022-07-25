package xyz.holocons.mc.waypoints;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperPlugin extends JavaPlugin {

    private FileConfiguration config;
    private HologramMap hologramMap;
    private TravelerMap travelerMap;
    private WaypointMap waypointMap;
    private Token token;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        config = getConfig();
        hologramMap = new HologramMap();
        travelerMap = new TravelerMap();
        waypointMap = new WaypointMap();
        token = new Token(this);
    }

    @Override
    public void onEnable() {
        final var commandHandler = new CommandHandler(this);
        getCommand("waypoints").setExecutor(commandHandler);
        getCommand("editwaypoints").setExecutor(commandHandler);
        final var eventListener = new EventListener(this);
        Bukkit.getPluginManager().registerEvents(eventListener, this);
    }

    @Override
    public void onDisable() {
        saveData();
    }

    public void backupData() {
        final var zipFile = new File(getDataFolder(), "backup-" + Instant.now().toString() + ".zip");
        final var travelerFile = new File(getDataFolder(), TravelerMap.FILENAME);
        final var waypointFile = new File(getDataFolder(), WaypointMap.FILENAME);
        try {
            final var writer = new ZipWriter(zipFile);
            writer.addFile(travelerFile, waypointFile);
            writer.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        getLogger().info("Backup written");
    }

    public void loadData() {
        try {
            travelerMap.loadTravelers(this);
            waypointMap.loadWaypoints(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        getLogger().info("Loaded");
    }

    public void saveData() {
        try {
            waypointMap.saveWaypoints(this);
            travelerMap.saveTravelers(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        getLogger().info("Saved");
    }

    public int getTravelerRegenChargeTime() {
        return config.getInt("traveler.regen-charge-time");
    }

    public int getTravelerTeleportWaitTime() {
        return config.getInt("traveler.teleport-wait-time");
    }

    public int getTravelerMaxCharges() {
        return config.getInt("traveler.max-charges");
    }

    public int getTravelerMaxTokens() {
        return config.getInt("traveler.max-tokens");
    }

    public int getWaypointTokenRequirement() {
        return config.getInt("waypoint.token-requirement");
    }

    public List<String> getWorldHome() {
        return config.getStringList("world.home");
    }

    public List<String> getWorldCamp() {
        return config.getStringList("world.camp");
    }

    public HologramMap getHologramMap() {
        return hologramMap;
    }

    public TravelerMap getTravelerMap() {
        return travelerMap;
    }

    public WaypointMap getWaypointMap() {
        return waypointMap;
    }

    public boolean isToken(Object obj) {
        return token.isToken(obj);
    }
}
