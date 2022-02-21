package xyz.holocons.mc.waypoints;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class EventListener implements Listener {

    private final PaperPlugin plugin;
    private final HologramManager hologramManager;
    private final TravelerManager travelerManager;
    private final WaypointManager waypointManager;

    public EventListener(final PaperPlugin plugin) {
        this.plugin = plugin;
        this.hologramManager = plugin.getHologramManager();
        this.travelerManager = plugin.getTravelerManager();
        this.waypointManager = plugin.getWaypointManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!Tag.ITEMS_BANNERS.isTagged(event.getItemInHand().getType())) {
            return;
        }

        final var blockPlaced = event.getBlockPlaced();
        final var player =  event.getPlayer();
        final var task = travelerManager.getTask(player, ModifyWaypointTask.class);

        if (task == null) {
            if (waypointManager.isWaypoint(blockPlaced)) {
                final var waypoint = waypointManager.getNearbyWaypoint(blockPlaced);
                hologramManager.updateTrackedPlayers(waypoint, player);
            }
            return;
        }

        if (!isValidWaypointPlacement(blockPlaced, event.getBlockAgainst())) {
            return;
        }

        switch (task.getMode()) {
            case CREATE -> {
                final var waypoint = waypointManager.createWaypoint(blockPlaced);

                if (waypoint == null) {
                    player.sendMessage(Component.text("There is already a waypoint nearby!", NamedTextColor.RED));
                    return;
                }

                hologramManager.showTrackedPlayers(waypoint, player);
            }
            default -> {
                return;
            }
        }
        travelerManager.unregisterTask(player);
    }

    private boolean isValidWaypointPlacement(Block blockPlaced, Block blockAgainst) {
        return blockAgainst.getFace(blockPlaced) == BlockFace.UP
            && plugin.getWorldHome().contains(blockPlaced.getWorld().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        // menu
    }

    @EventHandler
    public void onPlayerChunkLoad(PlayerChunkLoadEvent event) {
        final var waypoint = waypointManager.getWaypoint(event.getChunk().getChunkKey());

        if (waypoint == null || waypoint.getLocation().getWorld() != event.getWorld()) {
            return;
        }

        hologramManager.show(waypoint, event.getPlayer());
    }

    @EventHandler
    public void onPlayerChunkUnload(PlayerChunkUnloadEvent event) {
        final var waypoint = waypointManager.getWaypoint(event.getChunk().getChunkKey());

        if (waypoint == null || waypoint.getLocation().getWorld() != event.getWorld()) {
            return;
        }

        hologramManager.hide(waypoint, event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isBlockInHand() || event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final var clickedBlock = event.getClickedBlock();
        final var player = event.getPlayer();
        final var task = travelerManager.getTask(player, ModifyWaypointTask.class);

        if (!waypointManager.isWaypoint(clickedBlock)) {
            if (task == null || !Tag.ITEMS_BANNERS.isTagged(clickedBlock.getType())) {
                return;
            }
            switch (task.getMode()) {
                case SETCAMP -> {
                    if (plugin.getWorldCamp().contains(clickedBlock.getWorld().getName())) {
                        travelerManager.getOrCreateTraveler(player).setCamp(clickedBlock.getLocation());
                    }
                }
                case SETHOME -> {
                    if (plugin.getWorldHome().contains(clickedBlock.getWorld().getName())) {
                        travelerManager.getOrCreateTraveler(player).setHome(clickedBlock.getLocation());
                    }
                }
                default -> {
                    return;
                }
            }
            travelerManager.unregisterTask(player);
            return;
        }

        final var waypoint = waypointManager.getNearbyWaypoint(clickedBlock);

        if (task == null) {
            if (waypoint.isActive()) {
                final var traveler = travelerManager.getOrCreateTraveler(player);
                if (!traveler.hasWaypoint(waypoint)) {
                    traveler.registerWaypoint(waypoint);
                    player.sendMessage(Component.text("You registered a waypoint!", NamedTextColor.GOLD));
                }
            } else {
                final var tokenRequirement = plugin.getWaypointTokenRequirement();
                final var contributors = waypoint.getContributorNames();
                final var contributorNamesBuilder = Component.text()
                    .color(NamedTextColor.GOLD);
                if (contributors.isEmpty()) {
                    contributorNamesBuilder.append(Component.text("Nobody has contributed to this waypoint!"));
                } else {
                    contributorNamesBuilder.append(Component.text("Contributors: " + String.join(", ", contributors)));
                }
                player.sendMessage(contributorNamesBuilder.build());
                sendActionBar(player, contributors.size(), tokenRequirement);
            }
            return;
        }

        switch (task.getMode()) {
            case ACTIVATE -> {
                waypoint.activate();
                hologramManager.updateTrackedPlayers(waypoint, player);
            }
            case ADDPOINT -> {
                if (waypoint.isActive()) {
                    return;
                }
                final var tokenRequirement = plugin.getWaypointTokenRequirement();
                final var contributors = waypoint.getContributors();
                final var traveler = travelerManager.getOrCreateTraveler(player);
                final var tokens = traveler.getTokens();
                if (tokens > 0) {
                    player.sendMessage(Component.text("You added a token!", NamedTextColor.BLUE));
                    traveler.setTokens(tokens - 1);
                    contributors.add(player.getUniqueId());
                    if (contributors.size() >= tokenRequirement) {
                        waypoint.activate();
                        hologramManager.updateTrackedPlayers(waypoint, player);
                    }
                }
                sendActionBar(player, contributors.size(), tokenRequirement);
            }
            case CREATE -> {
                if (waypoint.isActive()) {
                    return;
                }
                waypointManager.removeWaypoint(waypoint);
                final var maxTokens = plugin.getTravelerMaxTokens();
                for (final var uniqueId : waypoint.getContributors()) {
                    final var traveler = travelerManager.getOrCreateTraveler(uniqueId);
                    traveler.setTokens(Math.min(traveler.getTokens() + 1, maxTokens));
                }
                hologramManager.remove(waypoint);
            }
            case DELETE -> {
                waypointManager.removeWaypoint(waypoint);
                travelerManager.removeWaypoint(waypoint);
                final var maxTokens = plugin.getTravelerMaxTokens();
                for (final var uniqueId : waypoint.getContributors()) {
                    final var traveler = travelerManager.getOrCreateTraveler(uniqueId);
                    traveler.setTokens(Math.min(traveler.getTokens() + 1, maxTokens));
                }
                hologramManager.remove(waypoint);
            }
            case REMOVEPOINT -> {
                if (waypoint.isActive()) {
                    return;
                }
                final var uniqueId = player.getUniqueId();
                final var contributors = waypoint.getContributors();
                if (contributors.contains(uniqueId)) {
                    player.sendMessage(Component.text("You removed a token!", NamedTextColor.BLUE));
                    final var maxTokens = plugin.getTravelerMaxTokens();
                    final var traveler = travelerManager.getOrCreateTraveler(player);
                    traveler.setTokens(Math.min(traveler.getTokens() + 1, maxTokens));
                    contributors.remove(uniqueId);
                }
                final var tokenRequirement = plugin.getWaypointTokenRequirement();
                sendActionBar(player, contributors.size(), tokenRequirement);
            }
            default -> {
                return;
            }
        }
        travelerManager.unregisterTask(player);
    }

    private static void sendActionBar(Player player, int contributorsSize, int tokenRequirement) {
        player.sendActionBar(Component.text(String.format("%d / %d", contributorsSize, tokenRequirement)));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        final var player = event.getPlayer();
        travelerManager.getOrCreateTraveler(player).startRegenCharge(plugin);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final var player = event.getPlayer();
        travelerManager.getOrCreateTraveler(player).stopRegenCharge();
        travelerManager.unregisterTask(player);
        hologramManager.remove(player);
    }
}
