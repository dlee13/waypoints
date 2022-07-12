package xyz.holocons.mc.waypoints;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

public class CommandHandler implements TabExecutor {

    private final PaperPlugin plugin;
    private final TravelerMap travelerMap;
    private final WaypointMap waypointMap;

    public CommandHandler(final PaperPlugin plugin) {
        this.plugin = plugin;
        this.travelerMap = plugin.getTravelerMap();
        this.waypointMap = plugin.getWaypointMap();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            switch (command.getName().toUpperCase()) {
                case "WAYPOINTS" -> {
                    if (args.length == 0) {
                        new Menu(plugin, player, Menu.Type.TELEPORT);
                        return true;
                    }
                    final var subcommand = args[0].toUpperCase();
                    switch (subcommand) {
                        case "ADDTOKEN", "CREATE", "REMOVETOKEN", "SETCAMP", "SETHOME" -> {
                            new TravelerTask(plugin, player, TravelerTask.Type.valueOf(subcommand));
                        }
                        case "CANCEL" -> {
                            travelerMap.unregisterTask(player);
                        }
                        case "TELEPORT" -> {
                            if (args.length == 1) {
                                new Menu(plugin, player, Menu.Type.TELEPORT);
                            } else {
                                teleport(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
                            }
                        }
                        default -> {
                            return false;
                        }
                    }
                }
                case "EDITWAYPOINTS" -> {
                    if (args.length == 0) {
                        new Menu(plugin, player, Menu.Type.EDIT);
                        return true;
                    }
                    final var subcommand = args[0].toUpperCase();
                    switch (subcommand) {
                        case "ACTIVATE", "DELETE" -> {
                            new TravelerTask(plugin, player, TravelerTask.Type.valueOf(subcommand));
                        }
                        case "MENU" -> {
                            new Menu(plugin, player, Menu.Type.EDIT);
                        }
                        case "UNSETCAMPS" -> {
                            travelerMap.removeCamps();
                        }
                        case "UNSETHOMES" -> {
                            travelerMap.removeHomes();
                        }
                        default -> {
                            return false;
                        }
                    }
                }
            }
        } else {
            if (args.length == 0) {
                return false;
            }
            final var subcommand = args[0].toUpperCase();
            switch (subcommand) {
                case "BACKUP" -> {
                    plugin.backupData();
                }
                case "LOAD" -> {
                    plugin.reloadConfig();
                    plugin.loadData();
                }
                case "SAVE" -> {
                    plugin.saveData();
                }
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player) {
            return switch (command.getName().toUpperCase()) {
                case "WAYPOINTS" -> {
                    yield switch (args.length) {
                        case 1 -> {
                            yield List.of("addtoken", "create", "removetoken", "setcamp", "sethome", "teleport");
                        }
                        case 2 -> {
                            if (args[0].equalsIgnoreCase("teleport")) {
                                final var traveler = travelerMap.getOrCreateTraveler(player);
                                final var waypoints = waypointMap.getNamedWaypoints().filter(traveler::hasWaypoint);
                                var names = waypoints.map(Waypoint::getName);
                                if (traveler.getCamp() != null) {
                                    names = Stream.concat(names, Stream.of("camp"));
                                }
                                if (traveler.getHome() != null) {
                                    names = Stream.concat(names, Stream.of("home"));
                                }
                                yield names.toList();
                            } else {
                                yield List.of();
                            }
                        }
                        default -> List.of();
                    };
                }
                case "EDITWAYPOINTS" -> {
                    yield switch (args.length) {
                        case 1 -> {
                            yield List.of("activate", "delete", "menu");
                        }
                        default -> List.of();
                    };
                }
                default -> List.of();
            };
        } else {
            return List.of();
        }
    }

    private void teleport(Player player, String destination) {
        final var traveler = travelerMap.getOrCreateTraveler(player);
        Location location;
        if (destination.equalsIgnoreCase("camp")) {
            location = traveler.getCamp();
            if (location == null) {
                player.sendMessage(Component.text("You don't have a camp!"));
            }
        } else if (destination.equalsIgnoreCase("home")) {
            location = traveler.getHome();
            if (location == null) {
                player.sendMessage(Component.text("You don't have a home!"));
            }
        } else {
            Predicate<Waypoint> matchesName = waypoint -> waypoint.getName().matches(destination);
            final var waypoint = waypointMap.getNamedWaypoints().filter(matchesName).findAny().orElse(null);
            location = traveler.hasWaypoint(waypoint) ? waypoint.getLocation() : null;
        }
        if (location == null) {
            new Menu(plugin, player, Menu.Type.TELEPORT);
            return;
        }
        new TeleportTask(plugin, player, location);
    }
}
