package xyz.holocons.mc.waypoints;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TeleportTask extends BukkitRunnable {

    final Player player;
    final Traveler traveler;
    final Location destination;
    final double initialHealth;
    final Vector initialPosition;
    final NamespacedKey key;

    public TeleportTask(final PaperPlugin plugin, final Player player, final Location destination) {
        plugin.getTravelerManager().registerTask(player, this);
        final var teleportWaitTime = plugin.getTravelerTeleportWaitTime();
        final var period = teleportWaitTime / 20;
        final var taskId = runTaskTimer(plugin, period, period).getTaskId();
        this.player = player;
        this.traveler = plugin.getTravelerManager().getOrCreateTraveler(player);
        this.destination = toXZCenterLocation(destination);
        this.initialHealth = player.getHealth();
        this.initialPosition = player.getLocation().toVector();
        this.key = new NamespacedKey(plugin, Integer.toString(taskId));
        final var bossBar = Bukkit.createBossBar(key, "Teleporting...", BarColor.GREEN, BarStyle.SEGMENTED_20);
        bossBar.setProgress(0.0);
        bossBar.addPlayer(player);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> removeBossBar(key), teleportWaitTime + 2);
    }

    @Override
    public void run() {
        if (playerTookDamage() || playerMoved()) {
            cancel();
            removeBossBar(key);
            player.sendMessage("Teleportation cancelled!");
            return;
        }

        final var bossBar = Bukkit.getBossBar(key);
        if (bossBar == null) {
            cancel();
            return;
        }

        final var newProgress = Math.min(bossBar.getProgress() + 0.05, 1.0);
        bossBar.setProgress(newProgress);
        if (newProgress == 1.0) {
            cancel();
            traveler.setCharges(traveler.getCharges() - 1);
            destination.setDirection(player.getLocation().getDirection());
            player.teleport(destination);
        }
    }

    private void removeBossBar(final NamespacedKey key) {
        final var bossBar = Bukkit.getBossBar(key);
        if (bossBar != null) {
            bossBar.removePlayer(player);
            Bukkit.removeBossBar(key);
        }
    }

    private boolean playerTookDamage() {
        return player.getHealth() < initialHealth;
    }

    private boolean playerMoved() {
        final var currentPosition = player.getLocation().toVector();
        final var distanceX = currentPosition.getX() - initialPosition.getX();
        final var distanceZ = currentPosition.getZ() - initialPosition.getZ();
        return distanceX * distanceX + distanceZ * distanceZ > 1.0;
    }

    private static Location toXZCenterLocation(final Location location) {
        final var newLocation = location.clone();
        newLocation.setX(location.getBlockX() + 0.5);
        newLocation.setZ(location.getBlockZ() + 0.5);
        return newLocation;
    }
}
