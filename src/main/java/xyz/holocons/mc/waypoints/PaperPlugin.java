package xyz.holocons.mc.waypoints;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import com.google.gson.Gson;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PaperPlugin extends JavaPlugin {

    private FileConfiguration config;
    private Gson gson;
    private HologramManager hologramManager;
    private TravelerManager travelerManager;
    private WaypointManager waypointManager;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        config = getConfig();
        gson = new Gson();
        hologramManager = new HologramManager();
        travelerManager = new TravelerManager();
        waypointManager = new WaypointManager();
    }

    @Override
    public void onEnable() {
        try {
            travelerManager.loadTravelers(this);
            waypointManager.loadWaypoints(this);
        } catch (IOException e) {
            travelerManager.clearTravelers();
            waypointManager.clearWaypoints();
            throw new UncheckedIOException(e);
        }
        final var commandHandler = new CommandHandler(this);
        getCommand("waypoints").setExecutor(commandHandler);
        getCommand("editwaypoints").setExecutor(commandHandler);
        final var eventListener = new EventListener(this);
        getServer().getPluginManager().registerEvents(eventListener, this);
    }

    @Override
    public void onDisable() {
        try {
            waypointManager.saveWaypoints(this);
            travelerManager.saveTravelers(this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    public Gson getGson() {
        return gson;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public TravelerManager getTravelerManager() {
        return travelerManager;
    }

    public WaypointManager getWaypointManager() {
        return waypointManager;
    }
}
