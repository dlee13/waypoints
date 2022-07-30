package xyz.holocons.mc.waypoints;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class TravelerTask extends BukkitRunnable {

    public enum Type {
        ACTIVATE,
        ADDTOKEN,
        CREATE,
        DELETE,
        REMOVETOKEN,
        SETCAMP,
        SETHOME,
    }

    private final Player player;
    private final Type type;
    private final int expiration;

    public TravelerTask(final WaypointsPlugin plugin, final Player player, final Type type) {
        plugin.getTravelerMap().registerTask(player, this);
        final var period = 40;
        runTaskTimer(plugin, 0, period);
        this.player = player;
        this.type = type;
        this.expiration = Bukkit.getCurrentTick() + 600;
        final var messageComponent = Component.text()
            .clickEvent(ClickEvent.runCommand("/waypoints cancel"))
            .hoverEvent(HoverEvent.showText(Component.text("Click to cancel early!")))
            .append(Component.text("You entered WAYPOINT " + type.toString() + " mode for 30 seconds!"))
            .build();
        player.sendMessage(messageComponent);
    }

    @Override
    public void run() {
        player.sendActionBar(Component.text("WAYPOINT " + type.toString(), NamedTextColor.GREEN));
        if (Bukkit.getCurrentTick() >= expiration) {
            cancel();
        }
    }

    public Type getType() {
        return type;
    }
}
