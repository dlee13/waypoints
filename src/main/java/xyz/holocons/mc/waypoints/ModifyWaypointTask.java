package xyz.holocons.mc.waypoints;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class ModifyWaypointTask extends BukkitRunnable {

    public enum Mode {
        ACTIVATE,
        ADDPOINT,
        CREATE,
        DELETE,
        REMOVEPOINT,
        SETCAMP,
        SETHOME,
    }

    private final Player player;
    private final Mode mode;
    private final int expiration;

    public ModifyWaypointTask(final PaperPlugin plugin, final Player player, final Mode mode) {
        final var period = 40;
        runTaskTimer(plugin, 0, period);
        this.player = player;
        this.mode = mode;
        this.expiration = Bukkit.getCurrentTick() + 600;
        final var messageComponent = Component.text()
            .clickEvent(ClickEvent.runCommand("/waypoints cancel"))
            .hoverEvent(HoverEvent.showText(Component.text("Click to cancel early!")))
            .append(Component.text("You entered WAYPOINT " + mode.toString() + " mode for 30 seconds!"))
            .build();
        player.sendMessage(messageComponent);
    }

    @Override
    public void run() {
        player.sendActionBar(Component.text("WAYPOINT " + mode.toString(), NamedTextColor.GREEN));
        if (Bukkit.getCurrentTick() >= expiration) {
            cancel();
        }
    }

    public Mode getMode() {
        return mode;
    }
}
