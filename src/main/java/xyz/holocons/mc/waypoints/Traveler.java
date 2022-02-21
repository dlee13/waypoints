package xyz.holocons.mc.waypoints;

import java.util.BitSet;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Traveler {

    private int charges;
    private int tokens;
    private Location home;
    private Location camp;
    private BitSet waypoints;
    private BukkitTask regenChargeTask;

    public Traveler(int charges, int tokens, Location home, Location camp, BitSet waypoints) {
        this.charges = charges;
        this.tokens = tokens;
        this.home = home;
        this.camp = camp;
        this.waypoints = waypoints != null ? waypoints : new BitSet();
        this.regenChargeTask = null;
    }

    public int getCharges() {
        return charges;
    }

    public int getTokens() {
        return tokens;
    }

    public Location getHome() {
        return home;
    }

    public Location getCamp() {
        return camp;
    }

    public BitSet getWaypoints() {
        return waypoints;
    }

    public boolean hasWaypoint(Waypoint waypoint) {
        return waypoint != null && waypoints.get(waypoint.getId());
    }

    public void registerWaypoint(Waypoint waypoint) {
        if (waypoint == null) {
            return;
        }
        waypoints.set(waypoint.getId());
    }

    public void unregisterWaypoint(Waypoint waypoint) {
        if (waypoint == null) {
            return;
        }
        waypoints.clear(waypoint.getId());
    }

    public void setCharges(int charges) {
        this.charges = charges;
    }

    public void setTokens(int tokens) {
        this.tokens = tokens;
    }

    public void setHome(Location home) {
        this.home = home;
    }

    public void setCamp(Location camp) {
        this.camp = camp;
    }

    public void startRegenCharge(PaperPlugin plugin) {
        if (regenChargeTask == null || regenChargeTask.isCancelled()) {
            final var traveler = this;
            final var maxCharges = plugin.getTravelerMaxCharges();
            final var period = plugin.getTravelerRegenChargeTime();

            regenChargeTask = new BukkitRunnable() {

                @Override
                public void run() {
                    final var newCharges = Math.min(maxCharges, traveler.getCharges() + 1);
                    traveler.setCharges(newCharges);
                    if (newCharges == maxCharges) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, period, period);
        }
    }

    public void stopRegenCharge() {
        if (regenChargeTask != null) {
            regenChargeTask.cancel();
            regenChargeTask = null;
        }
    }
}
